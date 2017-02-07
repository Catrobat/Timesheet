/*
 * Copyright 2016 Adrian Schnedlitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.catrobat.jira.timesheet.rest;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.google.gson.Gson;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.helper.TimesheetPermissionCondition;
import org.catrobat.jira.timesheet.rest.json.*;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;
import org.catrobat.jira.timesheet.utility.EmailUtil;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.catrobat.jira.timesheet.utility.RestUtils.asSortedList;
import static org.catrobat.jira.timesheet.utility.RestUtils.convertTeamsToJSON;

//TODO: check if permissions are to loosely, in case adapted it, maybe checkIfUserExists() is not enough

@Path("/")
@Produces({MediaType.APPLICATION_JSON})
public class TimesheetRest {

    private final TimesheetEntryService entryService;
    private final TimesheetService sheetService;
    private final CategoryService categoryService;
    private final TeamService teamService;
    private final PermissionService permissionService;
    private final ConfigService configService;
    private final TimesheetPermissionCondition permissionCondition;
    private final EmailUtil emailUtil;

    public TimesheetRest(final TimesheetEntryService es, final TimesheetService ss, final CategoryService cs,
            final TeamService ts, PermissionService ps, final ConfigService ahcs, TimesheetPermissionCondition permissionCondition) {
        this.teamService = ts;
        this.entryService = es;
        this.sheetService = ss;
        this.categoryService = cs;
        this.permissionService = ps;
        this.configService = ahcs;
        this.permissionCondition = permissionCondition;
        emailUtil = new EmailUtil(configService);
    }

    private String checkIfCategoryIsAssociatedWithTeam(@Nullable Team team, @Nullable Category category) {
        // TODO: this code is ugly, improve it
        if (team == null) {
            return "Team not found.";
        } else if (category == null) {
            return "Category not found.";
        } else if (!Arrays.asList(team.getCategories()).contains(category)) {
            return "Category is not associated with Team.";
        }
        return "";
    }

    @GET
    @Path("checkConstrains")
    public Response checkConstrains(@Context HttpServletRequest request) {
        boolean shouldDisplay = false;
        try {
            shouldDisplay = permissionCondition.shouldDisplay(permissionService.checkIfUserExists(), null);
        } catch (PermissionException e) {
            e.printStackTrace();
        }

        return Response.ok(shouldDisplay).build();
    }

    @GET
    @Path("teams/{timesheetID}")
    public Response getTeamsForTimesheetID(@Context HttpServletRequest request,
            @PathParam("timesheetID") int timesheetID) {
        ApplicationUser loggedInUser;
        try {
            loggedInUser = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        Timesheet sheet;
        try {
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }

        if (!permissionService.userCanViewTimesheet(loggedInUser, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
        }

        ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(sheet.getUserKey());

        Set<Team> teamsOfUser = teamService.getTeamsOfUser(user.getName());
        List<Team> sortedTeamsOfUsersList = asSortedList(teamsOfUser);
        List<JsonTeam> jsonTeams = convertTeamsToJSON(sortedTeamsOfUsersList);

        return Response.ok(jsonTeams).build();
    }

    @GET
    @Path("timesheet/{timesheetID}/teamEntries")
    public Response getTimesheetEntriesOfAllTeamMembers(@Context HttpServletRequest request,
            @PathParam("timesheetID") int timesheetID) {
        ApplicationUser loggedInUser;
        try {
            loggedInUser = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        Timesheet ownerSheet;
        try {
            ownerSheet = sheetService.getTimesheetByID(timesheetID);
            if (!permissionService.userCanViewTimesheet(loggedInUser, ownerSheet)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
            }
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }

        List<JsonTimesheetEntry> jsonTimesheetEntries = new LinkedList<>();

        //get all teams of that user
        for (Team team : teamService.getTeamsOfUser(loggedInUser.getName())) {
            //get all team members
            for (String teamMember : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.DEVELOPER)) {
                //collect all timesheet entries of all team members
                try {
                    if (ComponentAccessor.getUserManager().getUserByName(teamMember) == null) {
                        continue; // FIXME: User no longer in Ldap group is this an error? or can it happen?
                    }
                    String userKey = ComponentAccessor.getUserManager().getUserByName(teamMember).getKey();
                    if (sheetService.userHasTimesheet(userKey, false)) {
                        Timesheet sheet = sheetService.getTimesheetByUser(userKey, false);

                        //all entries of each user
                        TimesheetEntry[] entries = entryService.getEntriesBySheet(sheet);

                        // Add entries anonymously
                        for (TimesheetEntry entry : entries) {
                            jsonTimesheetEntries.add(new JsonTimesheetEntry(entry, true));
                        }
                    }
                } catch (ServiceException e) {
                    return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
                }
            }
        }

        return Response.ok(jsonTimesheetEntries).build();
    }

    @GET
    @Path("timesheet/{teamName}/entries")
    public Response getAllTimesheetEntriesForTeam(@Context HttpServletRequest request,
            @PathParam("teamName") String teamName) {
        //TODO: maybe coordinator private access is enouph - we will see
        // chess if user is coordinator of team
       /* Response unauthorized = permissionService.checkRootPermission(request);
        if (unauthorized != null) {
            return unauthorized;
        }*/

        ApplicationUser user;
        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        try {
            Team team = teamService.getTeamByName(teamName);
            if (!permissionService.isUserCoordinatorOfTeam(user, team) ||
                    !permissionService.isTimesheetAdmin(user) ||
                    !permissionService.isReadOnlyUser(user)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to access the timesheet!").build();
            }
        } catch (ServiceException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        List<JsonTimesheetEntry> jsonTimesheetEntries = new LinkedList<>();
        for (String developerTeamMemberName : configService.getGroupsForRole(teamName, TeamToGroup.Role.DEVELOPER)) {
            TimesheetEntry[] timesheetEntries;
            try {
                String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(developerTeamMemberName);
                Timesheet timesheetByUser = sheetService.getTimesheetByUser(userKey, false);

                //check permissions for each sheet
                if (!permissionService.userCanViewTimesheet(user, timesheetByUser)) {
                    return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
                }
                timesheetEntries = timesheetByUser.getEntries();
            } catch (ServiceException e) {
                return Response.serverError().entity("At least one Team Member has no valid Timesheet Entries.").build();
            }

            for (TimesheetEntry entry : timesheetEntries) {
                if (entry.getTeam().getTeamName().equals(teamName)) {
                    jsonTimesheetEntries.add(new JsonTimesheetEntry(entry));
                }
            }
        }

        return Response.ok(jsonTimesheetEntries).build();
    }

    @GET
    @Path("timesheet/timesheetID/{userName}/{getMTSheet}")
    public Response getTimesheetIDOFUser(@Context HttpServletRequest request,
            @PathParam("userName") String userName,
            @PathParam("getMTSheet") Boolean getMTSheet) {

        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        Timesheet sheet;
        try {
            sheet = sheetService.getTimesheetByUser(ComponentAccessor.
                    getUserKeyService().getKeyForUsername(userName), getMTSheet);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        //check permissions for each sheet
        if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.FORBIDDEN).entity("Timesheet of Username: "
                    + userName + " has not been initialized.  [sheet is null]").build();
        }

        return Response.ok(sheet.getID()).build();
    }

    @GET
    @Path("timesheets/owner/{timesheetID}")
    public Response getOwnerOfTimesheet(@Context HttpServletRequest request,
            @PathParam("timesheetID") int timesheetID) {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        Timesheet sheet;
        try {
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.serverError().entity("No Timesheet available with this ID: " + timesheetID + ".").build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User Timesheet has not been initialized.").build();
        } else if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Timesheet Access denied.").build();
        }

        JsonUser jsonUser = new JsonUser();
        jsonUser.setUserName(user.getUsername());

        return Response.ok(jsonUser).build();
    }

    @GET
    @Path("timesheet/of/{userName}/{isMTSheet}")
    public Response getTimesheetForUsername(@Context HttpServletRequest request,
            @PathParam("userName") String userName,
            @PathParam("isMTSheet") Boolean isMTSheet) {
        Timesheet sheet;
        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(userName);
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        try {
            sheet = sheetService.getTimesheetByUser(userKey, isMTSheet);
        } catch (ServiceException e) {
            return Response.serverError().entity("No Timesheet available for this user.").build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User Timesheet has not been initialized.").build();
        } else if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Timesheet Access denied.").build();
        }

        //check permissions for each sheet
        if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
        }
        JsonTimesheet jsonTimesheet = new JsonTimesheet(sheet);
        return Response.ok(jsonTimesheet).build();
    }

    @GET
    @Path("timesheets/{timesheetID}")
    public Response getTimesheet(@Context HttpServletRequest request,
            @PathParam("timesheetID") int timesheetID) {

        Timesheet sheet;
        ApplicationUser user;
        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        try {
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.serverError().entity("No Timesheet available with this ID: " + timesheetID + ".").build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User Timesheet has not been initialized.").build();
        } else if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Timesheet Access denied.").build();
        }

        //check permissions for each sheet
        if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
        }


        int completeTime = sheet.getTargetHoursCompleted();
        int targetTime = sheet.getTargetHours();

        // FIXME: i dont think here is the right place for that check
        if ((targetTime - completeTime) <= 80) {
            emailUtil.buildEmailOutOfTime(user.getEmailAddress(), sheet, user);
        }

        JsonTimesheet jsonTimesheet = new JsonTimesheet(sheet);

        return Response.ok(jsonTimesheet).build();
    }

    @GET
    @Path("timesheets/{timesheetID}/entries")
    public Response getTimesheetEntries(@Context HttpServletRequest request,
            @PathParam("timesheetID") int timesheetID) {

        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        Timesheet sheet;
        try {
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.serverError().entity("No Timesheet available with this ID: " + timesheetID + ".").build();
        }

        //check permissions for each sheet
        if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
        }

        TimesheetEntry[] entries = entryService.getEntriesBySheet(sheet);

        List<JsonTimesheetEntry> jsonEntries = new ArrayList<>(entries.length);

        for (TimesheetEntry entry : entries) {
            jsonEntries.add(new JsonTimesheetEntry(entry));
        }

        return Response.ok(jsonEntries).build();
    }

    // URL: http://localhost:2990/jira/rest/timesheet/latest/timesheets/getTimesheets
    @GET
    @Path("timesheets/getTimesheets")
    public Response getTimesheets(@Context HttpServletRequest request) {
        Response response = permissionService.checkRootPermission();
        if (response != null) {
            return response;
        }

        List<JsonTimesheet> jsonTimesheetList = new ArrayList<>();

        for (Timesheet timesheet : sheetService.all()) {
            JsonTimesheet jsonTimesheet = new JsonTimesheet(timesheet);
            jsonTimesheetList.add(jsonTimesheet);
        }

        return Response.ok(jsonTimesheetList).build();
    }

    @POST
    @Path("timesheets/{timesheetID}/entry/{isMTSheet}")
    public Response postTimesheetEntry(@Context HttpServletRequest request,
            final JsonTimesheetEntry entry,
            @PathParam("timesheetID") int timesheetID,
            @PathParam("isMTSheet") Boolean isMTSheet) {

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        Date twoMonthsAhead = new DateTime().plusMonths(2).toDate();

        if (entry.getInactiveEndDate().compareTo(entry.getBeginDate()) < 0) {
            String message = "The 'Inactive End Date' is before your 'Entry Date'. That is not possible. The begin date is " + entry.getBeginDate() +
                    " but your inactive end date is " + entry.getInactiveEndDate();
            return Response.status(Response.Status.FORBIDDEN).entity(message).build();
        } else if (entry.getInactiveEndDate().compareTo(twoMonthsAhead) > 0) {
            String message = "The 'Inactive End Date' is more than 2 months ahead. This is too far away.";
            return Response.status(Response.Status.FORBIDDEN).entity(message).build();
        } else if ((entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0) &&
                (!categoryService.getCategoryByID(entry.getCategoryID()).getName().equals("Inactive"))) {
            return Response.status(Response.Status.FORBIDDEN).entity("You also have to select the 'Inactive' Category for a valid 'Inactive-Entry'.").build();
        } else if (entry.getDeactivateEndDate().compareTo(entry.getBeginDate()) < 0) {
            String message = "The 'Inactive & Offline End Date' is before your 'Entry Date'. That is not possible. The begin date is " + entry.getBeginDate() +
                    " but your inactive end date is " + entry.getDeactivateEndDate();
            return Response.status(Response.Status.FORBIDDEN).entity(message).build();
        } else if (entry.getDescription().isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("The 'Task Description' field must not be empty.").build();
        }

        Timesheet sheet;
        ApplicationUser user;
        Category category;
        Team team;

        try {
            user = permissionService.checkIfUserExists();
            sheet = sheetService.getTimesheetByID(timesheetID);
            category = categoryService.getCategoryByID(entry.getCategoryID());
            team = teamService.getTeamByID(entry.getTeamID());
            String error = checkIfCategoryIsAssociatedWithTeam(team, category);
            if (error != "") {
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }
            permissionService.userCanAddTimesheetEntry(user, sheet, entry.getBeginDate(), entry.IsGoogleDocImport());
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("'Timesheet' not found.").build();
        } catch (com.atlassian.jira.exception.PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        String programmingPartnerName = "";
        if (categoryService.isPairProgrammingCategory(category)) {
            if (entry.getPairProgrammingUserName().isEmpty()) {
                return Response.status(Response.Status.CONFLICT).entity("Pair Programming Partner is missing!").build();
            }
            programmingPartnerName = entry.getPairProgrammingUserName();
        }

        if (sheet == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("The Timesheet your are looking for is NULL.").build();
        } else if (!sheet.getIsEnabled()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
        } else if (!categoryService.isPairProgrammingCategory(category) && !programmingPartnerName.equals("")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You can not select a 'Pair programming' " +
                    "Partner without selecting the 'Pair programming' category.").build();
        }

        //check if all pair programming names are valid
        for (String userName : entry.getPairProgrammingUserName().split(",")) {
            if (userName.equals(user.getUsername())) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("You can not select yourself for 'Pair Programming'.").build();
            } else if ((ComponentAccessor.getUserManager().getUserByName(userName) == null) &&
                    (!entry.getPairProgrammingUserName().isEmpty())) {
                return Response.status(Response.Status.NOT_FOUND).entity("Username : " + userName + " not found.").build();
            }
        }

        TimesheetEntry newEntry = entryService.add(sheet, entry.getBeginDate(), entry.getEndDate(), category,
                entry.getDescription(), entry.getPauseMinutes(), team, entry.IsGoogleDocImport(),
                entry.getInactiveEndDate(), entry.getDeactivateEndDate(), entry.getTicketID(), programmingPartnerName);

        Timesheet.State state = sheet.getState();

        if ((entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0)) {
            state = Timesheet.State.INACTIVE;
        } else if ((entry.getDeactivateEndDate().compareTo(entry.getBeginDate()) > 0)) {
            state = Timesheet.State.INACTIVE_OFFLINE;
        }

        //update latest timesheet entry date if latest entry date is < new latest entry in the table
        if (sheet.getEntries().length == 1) {
            try {
                sheetService.editTimesheet(ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername()),
                        sheet.getTargetHoursPractice(), sheet.getTargetHoursTheory(), sheet.getTargetHours(),
                        sheet.getTargetHoursCompleted(), sheet.getTargetHoursRemoved(), sheet.getLectures(),
                        sheet.getReason(), entryService.getEntriesBySheet(sheet)[0].
                                getBeginDate(), isMTSheet, sheet.getIsEnabled(), state);
            } catch (ServiceException e) {
                return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
            }
        } else if (entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate()) >= 0) {
            try {
                sheetService.editTimesheet(ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername()),
                        sheet.getTargetHoursPractice(), sheet.getTargetHoursTheory(), sheet.getTargetHours(),
                        sheet.getTargetHoursCompleted(), sheet.getTargetHoursRemoved(), sheet.getLectures(),
                        sheet.getReason(), entry.getBeginDate(), isMTSheet, sheet.getIsEnabled(), state);
            } catch (ServiceException e) {
                return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
            }
        } else {
            System.out.println(entryService.getEntriesBySheet(sheet)[0]);
            System.out.println(entry.getBeginDate());
            System.out.println(entryService.getEntriesBySheet(sheet)[0].getBeginDate());
            System.out.println((entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate())));
        }

        entry.setEntryID(newEntry.getID());

        return Response.ok(entry).build();
    }

    @POST
    @Path("timesheets/{timesheetID}/entries/{isMTSheet}")
    public Response postTimesheetEntries(@Context HttpServletRequest request,
            final JsonTimesheetEntry[] entries,
            @PathParam("timesheetID") int timesheetID,
            @PathParam("isMTSheet") Boolean isMTSheet) throws PermissionException {

        ApplicationUser user;
        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        Timesheet sheet;
        try {
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("'Timesheet' does not exist.").build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("The Timesheet your are looking for is NULL.").build();
        }

        if (!sheet.getIsEnabled()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
        }

        List<JsonTimesheetEntry> newEntries = new LinkedList<>();

        for (JsonTimesheetEntry entry : entries) {
            if (entry == null) {
                return Response.status(Response.Status.FORBIDDEN).entity("Please check whether your 'Import-Entry' fulfills all import-requirements.").build();
            } else if (entry.getDescription().isEmpty()) {
                return Response.status(Response.Status.FORBIDDEN).entity("The 'Task Description' field must not be empty.").build();
            } else if ((entry.getInactiveEndDate().compareTo(entry.getBeginDate()) < 0)) {
                return Response.status(Response.Status.FORBIDDEN).entity("The 'Inactive Date' is before your 'Timesheet Entry Date'. That is not possible.").build();
            }

            String programmingPartnerName = "";
            Timesheet.State state = sheet.getState();
            ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

            try {
                permissionService.userCanAddTimesheetEntry(loggedInUser, sheet, entry.getBeginDate(), entry.IsGoogleDocImport());
                Category category = categoryService.getCategoryByID(entry.getCategoryID());
                Team team = teamService.getTeamByID(entry.getTeamID());
                String error = checkIfCategoryIsAssociatedWithTeam(team, category);
                if (error != "") {
                    return Response.status(Response.Status.FORBIDDEN).entity(error).build();
                }

                if (!entry.getPairProgrammingUserName().isEmpty()) {
                    programmingPartnerName = ComponentAccessor.getUserManager().getUserByName(entry.getPairProgrammingUserName()).getUsername();
                } else if ((entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0)) {
                    state = Timesheet.State.INACTIVE;
                } else if ((entry.getDeactivateEndDate().compareTo(entry.getBeginDate()) > 0)) {
                    state = Timesheet.State.INACTIVE_OFFLINE;
                }

                if (entry.isTheory()) {
                    category = categoryService.getCategoryByName(SpecialCategories.THEORY);
                } else if (entry.IsGoogleDocImport()) {
                    category = categoryService.getCategoryByName(SpecialCategories.GOOGLEDOCSIMPORT);
                }

                TimesheetEntry newEntry = entryService.add(sheet, entry.getBeginDate(), entry.getEndDate(), category,
                        entry.getDescription(), entry.getPauseMinutes(), team, entry.IsGoogleDocImport(),
                        entry.getInactiveEndDate(), entry.getDeactivateEndDate(), entry.getTicketID(), programmingPartnerName);

                //update latest timesheet entry date if latest entry date is < new latest entry in the table
                if (sheet.getEntries().length == 1) {
                    sheetService.editTimesheet(ComponentAccessor.
                                    getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                            sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                            sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(),
                            entryService.getEntriesBySheet(sheet)[0].getBeginDate(),
                            isMTSheet, sheet.getIsEnabled(), state);
                } else if (entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate()) >= 0) {
                    sheetService.editTimesheet(ComponentAccessor.
                                    getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                            sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                            sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(),
                            entryService.getEntriesBySheet(sheet)[0].getBeginDate(),
                            isMTSheet, sheet.getIsEnabled(), state);
                }

                entry.setEntryID(newEntry.getID());
                newEntries.add(entry);

            } catch (ServiceException e) {
                return Response.status(Response.Status.FORBIDDEN).entity("'Timesheet' not found.").build();
            } catch (PermissionException e) {
                return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
            }
        }

        JsonTimesheetEntries jsonNewEntries = new JsonTimesheetEntries(
                newEntries.toArray(new JsonTimesheetEntry[newEntries.size()])
        );

        return Response.ok(jsonNewEntries).build();
    }

    @POST
    @Path("timesheets/update/{timesheetID}/{isMTSheet}")
    public Response postTimesheetHours(@Context HttpServletRequest request,
            final JsonTimesheet jsonTimesheet,
            @PathParam("timesheetID") int timesheetID,
            @PathParam("isMTSheet") Boolean isMTSheet) {

        Timesheet sheet;
        ApplicationUser user;
        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        try {
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("'Timesheet' not found.").build();
        }

        if (!permissionService.userCanEditTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to edit the timesheet.").build();
        }

        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        if (sheet == null || !permissionService.userCanViewTimesheet(loggedInUser, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } else if (!sheet.getIsEnabled()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
        }

        try {
            if (permissionService.isJiraAdministrator(user)) {
                sheet = sheetService.editTimesheet(sheet.getUserKey(), jsonTimesheet.getTargetHourPractice(),
                        jsonTimesheet.getTargetHourTheory(), jsonTimesheet.getTargetHours(), jsonTimesheet.getTargetHoursCompleted(),
                        jsonTimesheet.getTargetHoursRemoved(), jsonTimesheet.getLectures(), jsonTimesheet.getReason(),
                        jsonTimesheet.getLatestEntryDate(), isMTSheet, jsonTimesheet.isEnabled(), jsonTimesheet.getState());
            } else {
                sheet = sheetService.editTimesheet(ComponentAccessor.
                                getUserKeyService().getKeyForUsername(user.getUsername()), jsonTimesheet.getTargetHourPractice(),
                        jsonTimesheet.getTargetHourTheory(), jsonTimesheet.getTargetHours(), jsonTimesheet.getTargetHoursCompleted(),
                        jsonTimesheet.getTargetHoursRemoved(), jsonTimesheet.getLectures(), jsonTimesheet.getReason(),
                        jsonTimesheet.getLatestEntryDate(), isMTSheet, jsonTimesheet.isEnabled(), jsonTimesheet.getState());
            }
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
        if (sheet == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("'Timesheet' not found.").build();
        }

        JsonTimesheet newJsonTimesheet = new JsonTimesheet(sheet);
        return Response.ok(newJsonTimesheet).build();
    }


    @POST
    @Path("timesheets/updateEnableStates")
    public Response postTimesheetEnableStates(@Context HttpServletRequest request,
            final JsonTimesheet[] jsonTimesheetList) throws ServiceException {

        Response response = permissionService.checkRootPermission();
        if (response != null) {
            return response;
        }

        Timesheet sheet;
        List<JsonTimesheet> newJsonTimesheetList = new LinkedList<>();

        if (jsonTimesheetList == null) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        for (JsonTimesheet jsonTimesheet : jsonTimesheetList) {
            sheet = sheetService.getTimesheetByID(jsonTimesheet.getTimesheetID());
            if (sheet != null) {
                sheet = sheetService.updateTimesheetEnableState(jsonTimesheet.getTimesheetID(), jsonTimesheet.isEnabled());
                JsonTimesheet newJsonTimesheet = new JsonTimesheet(sheet);
                newJsonTimesheetList.add(newJsonTimesheet);
            }
        }

        return Response.ok(newJsonTimesheetList).build();
    }

    @PUT
    @Path("entries/{entryID}/{isMTSheet}")
    public Response putTimesheetEntry(@Context HttpServletRequest request,
            final JsonTimesheetEntry jsonEntry,
            @PathParam("entryID") int entryID,
            @PathParam("isMTSheet") Boolean isMTSheet) {

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        if (jsonEntry.getDescription().isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("The 'Task Description' field must not be empty.").build();
        }

        ApplicationUser user;
        TimesheetEntry entry;
        Category category;
        Team team;
        Timesheet sheet;
        String programmingPartnerName = "";
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        try {
            user = permissionService.checkIfUserExists();
            entry = entryService.getEntryByID(entryID);
            category = categoryService.getCategoryByID(jsonEntry.getCategoryID());
            team = teamService.getTeamByID(jsonEntry.getTeamID());
            sheet = entry.getTimeSheet();
            String error = checkIfCategoryIsAssociatedWithTeam(team, category);
            if (error != "") {
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }
            permissionService.userCanEditTimesheetEntry(loggedInUser, entry.getTimeSheet(), entry);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("'Timesheet' not found.").build();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        if (sheet.getIsEnabled()) {
            if (categoryService.isPairProgrammingCategory(category)) {
                if (jsonEntry.getPairProgrammingUserName().isEmpty()) {
                    return Response.status(Response.Status.CONFLICT).entity("Pair Programming Partner is missing!").build();
                }
                programmingPartnerName = jsonEntry.getPairProgrammingUserName();
            }

//            if (!entry.getPairProgrammingUserName().isEmpty()) {
//                if (!jsonEntry.getPairProgrammingUserName().isEmpty()) {
//                    programmingPartnerName = ComponentAccessor.getUserManager().getUserByName(jsonEntry.getPairProgrammingUserName()).getUsername();
//                } else {
//                    programmingPartnerName = "";
//                }
//            }
            try {
                entryService.edit(entryID, entry.getTimeSheet(), jsonEntry.getBeginDate(), jsonEntry.getEndDate(), category,
                        jsonEntry.getDescription(), jsonEntry.getPauseMinutes(), team, jsonEntry.IsGoogleDocImport(),
                        jsonEntry.getInactiveEndDate(), jsonEntry.getDeactivateEndDate(), programmingPartnerName, jsonEntry.getTicketID());
            } catch (ServiceException e) {
                return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
            }

            //inform user about Administrator changes
            try {
                if (permissionService.isJiraAdministrator(user)) {
                    String userEmail = ComponentAccessor.getUserManager().getUserByKey(sheet.getUserKey()).getEmailAddress();
                    emailUtil.buildEmailAdministratorChangedEntry(user.getEmailAddress(), userEmail, entry, jsonEntry);
                }
            } catch (ServiceException e) {
                return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
            }

            if ((sheet.getEntries().length == 1) ||
                    (entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate()) >= 0)) {
                try {
                    sheetService.editTimesheet(ComponentAccessor.
                                    getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                            sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                            sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(),
                            entryService.getEntriesBySheet(sheet)[0].getBeginDate(),
                            isMTSheet, sheet.getIsEnabled(), sheet.getState());
                } catch (ServiceException e) {
                    return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
                }
            }

            return Response.ok(jsonEntry).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
    }

    @DELETE
    @Path("entries/{entryID}/{isMTSheet}")
    public Response deleteTimesheetEntry(@Context HttpServletRequest request,
            @PathParam("entryID") int entryID,
            @PathParam("isMTSheet") Boolean isMTSheet) {
        ApplicationUser user;
        TimesheetEntry entry;
        Timesheet sheet;

        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        try {
            entry = entryService.getEntryByID(entryID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        try {
            permissionService.userCanDeleteTimesheetEntry(loggedInUser, entry);
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        //update latest date
        try {
            sheet = sheetService.getTimesheetByUser(ComponentAccessor.
                    getUserKeyService().getKeyForUsername(user.getUsername()), false);
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.CONFLICT).entity("Timesheet not found.").build();
        }

        if (!sheet.getIsEnabled()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
        } else if (!permissionService.userCanViewTimesheet(loggedInUser, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Access denied.").build();
        }

        try {
            if (permissionService.isJiraAdministrator(user)) {
                emailUtil.buildEmailAdministratorDeletedEntry(user.getEmailAddress(), ComponentAccessor.getUserManager().getUserByKey(sheet.getUserKey()).getEmailAddress(), entry);
            }
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }

        Timesheet.State state = sheet.getState();

        // FIXME: Set Back to Active if it was INACTIVE OR OFFLINE
        if (entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0) {
            state = Timesheet.State.ACTIVE;
        } else if (entry.getDeactivateEndDate().compareTo(entry.getBeginDate()) > 0) {
            state = Timesheet.State.ACTIVE;
        }

        entryService.delete(entry);

        //update latest timesheet entry date if latest entry date is < new latest entry in the table
        if (sheet.getEntries().length > 0) {
            if (entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate()) > 0) {
                try {
                    sheetService.editTimesheet(ComponentAccessor.
                                    getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                            sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                            sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(),
                            entryService.getEntriesBySheet(sheet)[0].getBeginDate(), isMTSheet, sheet.getIsEnabled(), state);
                } catch (ServiceException e) {
                    return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
                }
            }
        } else {
            try {
                sheetService.editTimesheet(ComponentAccessor.
                                getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                        sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                        sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(),
                        new Date(), isMTSheet, sheet.getIsEnabled(), sheet.getState());
            } catch (ServiceException e) {
                return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
            }
        }
        return Response.ok().build();
    }

    // This is a demo API REST method to show you how to use the powerful and wonderful JqlQueryBuilder class
    @GET
    @Path("issueTickets")
    public Response getIssueTickets(@Context HttpServletRequest request) throws ServiceException {
        try {
            permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
        ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        final JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();

        //builder.where().project("DEMO");
        builder.where().assigneeUser("Admin");
        Query query = builder.buildQuery();

        List<Issue> issues = null;

        try {
            SearchResults results = searchService.search(currentUser, query, PagerFilter.getUnlimitedFilter());
            issues = results.getIssues();

            for (Issue issue : issues) {
                String displayName = issue.getAssignee().getDisplayName();
                System.out.println("displayName = " + displayName);
            }

        } catch (SearchException e) {
            System.out.println("Error running search: " + e);
        }

        JSONObject jo = new JSONObject();
        try {
            for (Issue issue : issues) {
                JSONObject item = new JSONObject();
                item.put("assignee", issue.getAssignee());
                item.put("id", issue.getId());
                jo.append("issues", item);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return Response.ok(jo.toString()).build();
    }

    @GET
    @Path("inactiveUsers")
    public Response getInactiveUsers(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkRootPermission();
        if (unauthorized != null) {
            return unauthorized;
        }

        List<Timesheet> timesheetList = sheetService.all();
        ArrayList<String> users = new ArrayList<>();
        Comparator<Timesheet> timesheetComparator = (o1, o2) -> o2.getLatestEntryBeginDate().compareTo(o1.getLatestEntryBeginDate());
        ArrayList<Timesheet> timesheets = new ArrayList<>();

        for (Timesheet timesheet : timesheetList) {
            if (entryService.getEntriesBySheet(timesheet).length == 0) { // nothing to do
                continue;
            }
            if (timesheet.getState() != Timesheet.State.ACTIVE) {
                timesheets.add(timesheet);
            }
        }

        Collections.sort(timesheets, timesheetComparator);
        for (Timesheet timesheet : timesheets) {
            String userKey = timesheet.getUserKey();
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
            String entry = user.getDisplayName() + "  (" + timesheet.getLatestEntryBeginDate().toString() + ")";
            users.add(entry);
        }

        String jsonList = new Gson().toJson(users);
        return Response.ok(jsonList).build();
    }
}

