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
import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.google.gson.Gson;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.helper.TimesheetPermissionCondition;
import org.catrobat.jira.timesheet.rest.json.*;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;
import org.catrobat.jira.timesheet.utility.EmailUtil;
import org.catrobat.jira.timesheet.utility.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.joda.JodaTimeContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.catrobat.jira.timesheet.utility.RestUtils.asSortedList;
import static org.catrobat.jira.timesheet.utility.RestUtils.convertTeamsToJSON;

@Path("/")
@Produces({MediaType.APPLICATION_JSON})
public class TimesheetRest {

    private final TimesheetEntryService entryService;
    private final TimesheetService sheetService;
    private final CategoryService categoryService;
    private final TeamService teamService;
    private final PermissionService permissionService;
    private final TimesheetPermissionCondition permissionCondition;
    private final EmailUtil emailUtil;
    private static final Logger logger = LoggerFactory.getLogger(TimesheetRest.class);
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(TimesheetRest.class);

    public TimesheetRest(final TimesheetEntryService es, final TimesheetService ss, final CategoryService cs,
                         final TeamService ts, PermissionService ps, final ConfigService ahcs, TimesheetPermissionCondition permissionCondition) {
        this.teamService = ts;
        this.entryService = es;
        this.sheetService = ss;
        this.categoryService = cs;
        this.permissionService = ps;
        this.permissionCondition = permissionCondition;
        emailUtil = new EmailUtil(ahcs);
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

        Timesheet sheet = sheetService.getTimesheetByID(timesheetID);

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
        ownerSheet = sheetService.getTimesheetByID(timesheetID);
        //retrieving owner of timesheet to watch
        String owner_key = ownerSheet.getUserKey();
        String owner_name = ComponentAccessor.getUserManager().getUserByKey(owner_key).getName();

        if (owner_key == null || owner_name == null) {
            return Response.serverError().entity("No user was found for timesheet id: " + timesheetID).build();
        }

        if (!permissionService.userCanViewTimesheet(loggedInUser, ownerSheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
        }

        List<JsonTimesheetEntry> jsonTimesheetEntries = new LinkedList<>();
        Vector<String> TeamMembers = new Vector<>();
        LOGGER.trace("We want to extract team entries from timesheet: " + timesheetID);

        //calculating data only for most active team
        Team team = teamService.getMostActiveTeamForUser(owner_name, ownerSheet);
        
        if (team == null) {
        	return Response.serverError().entity("User has no team: " + owner_name).build();
        }
        
        LOGGER.trace("In getTeamEntries calculating for team: " + team.getTeamName());

        for (String teamMembersAndGroups : teamService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.DEVELOPER)) {
            LOGGER.trace("currently to validate is : " + teamMembersAndGroups);
            if (ComponentAccessor.getUserManager().getUserByName(teamMembersAndGroups) == null) {
                Collection<String> usersInGroup = ComponentAccessor.getGroupManager().getUserNamesInGroup(teamMembersAndGroups);
                for (String member : usersInGroup) {
                    if (!TeamMembers.contains(member)) {
                        TeamMembers.add(member);
                    }
                }
            } else {
                if (!TeamMembers.contains(teamMembersAndGroups)) {
                    TeamMembers.add(teamMembersAndGroups);
                }
            }
        }

        LOGGER.trace("after extraction list is: " + TeamMembers.toString());
        for (String member : TeamMembers) {
            //collect all timesheet entries of all team members
            try {
                String userKey = ComponentAccessor.getUserManager().getUserByName(member).getKey();
                if (sheetService.userHasTimesheet(userKey)) {
                    Timesheet sheet = sheetService.getTimesheetByUser(userKey);

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

        return Response.ok(jsonTimesheetEntries).build();
    }

    @GET
    @Path("timesheet/{teamName}/entries")
    public Response getAllTimesheetEntriesForTeam(@Context HttpServletRequest request,
                                                  @PathParam("teamName") String teamName) {
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
            if (!(permissionService.isUserCoordinatorOfTeam(user, team) ||
                    permissionService.isTimesheetAdmin(user) ||
                    permissionService.isReadOnlyUser(user))) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to access the timesheet!").build();
            }
        } catch (ServiceException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        List<JsonTimesheetEntry> jsonTimesheetEntries = new LinkedList<>();
        for (String developerTeamMemberName : teamService.getGroupsForRole(teamName, TeamToGroup.Role.DEVELOPER)) {
            TimesheetEntry[] timesheetEntries;
            try {
                String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(developerTeamMemberName);
                Timesheet timesheetByUser = sheetService.getTimesheetByUser(userKey);

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
    @Path("timesheet/timesheetID/{userName}")
    public Response getTimesheetIDOFUser(@Context HttpServletRequest request,
                                         @PathParam("userName") String userName) {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        Timesheet sheet;
        try {
            sheet = sheetService.getTimesheetByUser(ComponentAccessor.
                    getUserKeyService().getKeyForUsername(userName));
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

        Timesheet sheet = sheetService.getTimesheetByID(timesheetID);

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
    @Path("timesheet/of/{userName}")
    public Response getTimesheetForUsername(@Context HttpServletRequest request,
                                            @PathParam("userName") String userName) {
        Timesheet sheet;
        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(userName);
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        try {
            sheet = sheetService.getTimesheetByUser(userKey);
        } catch (ServiceException e) {
            return Response.serverError().entity("No Timesheet available for this user.").build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User Timesheet has not been initialized.").build();
        } else if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
        }

        JsonTimesheet jsonTimesheet = new JsonTimesheet(sheet);
        return Response.ok(jsonTimesheet).build();
    }

    @GET
    @Path("timesheets/{timesheetID}")
    public Response getTimesheet(@Context HttpServletRequest request,
                                 @PathParam("timesheetID") int timesheetID) {
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

        Timesheet sheet = sheetService.getTimesheetByID(timesheetID);

        if (sheet == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User Timesheet has not been initialized.").build();
        } else if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Timesheet Access denied.").build();
        }

        //check permissions for each sheet
        if (!permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not allowed to see the timesheet.").build();
        }

        int completeTime = sheet.getHoursCompleted();
        int targetTime = sheet.getTargetHours();

        // FIXME: i dont think here is the right place for that check
        // Todo: should be moved to service class
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

        Timesheet sheet = sheetService.getTimesheetByID(timesheetID);

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
    @Path("timesheets/{timesheetID}/entry")
    public Response postTimesheetEntry(@Context HttpServletRequest request,
                                       final JsonTimesheetEntry entry,
                                       @PathParam("timesheetID") int timesheetID) {
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }
        LOGGER.trace("We want to save an entry...");
        try {
            RestUtils.checkJsonTimesheetEntryAndCategory(entry, categoryService);
            // TODO: how to remove the old error message if the done stuff was yet successful?? 
        } catch (ParseException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }

        Timesheet sheet = sheetService.getTimesheetByID(timesheetID);

        try {
            RestUtils.checkTimesheetIsEnabled(sheet);
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        ApplicationUser user;
        Category category = categoryService.getCategoryByID(entry.getCategoryID());
        Team team = teamService.getTeamByID(entry.getTeamID());
//        LOGGER.trace("team is: " + team.getTeamName());

        try {
            user = permissionService.checkIfUserExists();
            permissionService.userCanAddTimesheetEntry(user, sheet, entry.getBeginDate(), entry.IsGoogleDocImport());
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        try {
            teamService.checkIfCategoryIsAssociatedWithTeam(team, category);
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }

        if (categoryService.isPairProgrammingCategory(category) && entry.getPairProgrammingUserName().isEmpty()) {
            return Response.status(Response.Status.CONFLICT).entity("Pair Programming Partner is missing!").build();
        }
        if (!categoryService.isPairProgrammingCategory(category) && !entry.getPairProgrammingUserName().isEmpty()) {
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

        TimesheetEntry newEntry;

        try {
            newEntry = entryService.add(sheet, entry.getBeginDate(), entry.getEndDate(), category,
                    entry.getDescription(), entry.getPauseMinutes(), team, entry.IsGoogleDocImport(),
                    entry.getInactiveEndDate(), entry.getTicketID(), entry.getPairProgrammingUserName(),
                    entry.getTeamroom());
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        entry.setEntryID(newEntry.getID());

        return Response.ok(entry).build();
    }


    @POST
    @Path("timesheets/{timesheetID}/entries")
    public Response postTimesheetEntries(@Context HttpServletRequest request,
                                         final JsonTimesheetEntry[] entries,
                                         @PathParam("timesheetID") int timesheetID) {
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

        if (!permissionService.isGoogleDocsImportEnabled() && !permissionService.isTimesheetAdmin(user)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("GoogleDocsImport has been Disabled. " +
                    "Talk to a Timesheet Admin if you still need to import a timesheet.").build();
        }

        Timesheet sheet = sheetService.getTimesheetByID(timesheetID);
        try {
            RestUtils.checkTimesheetIsEnabled(sheet);
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Map<String, List<JsonTimesheetEntry>> errorMap = new HashMap<>();
        errorMap.put("correct", new ArrayList<>());
        for (JsonTimesheetEntry entry : entries) {
            try {
                RestUtils.checkJsonTimesheetEntry(entry);
                permissionService.userCanAddTimesheetEntry(user, sheet, entry.getBeginDate(), entry.IsGoogleDocImport());
                Category category = categoryService.getCategoryByID(entry.getCategoryID());
                Team team = teamService.getTeamByID(entry.getTeamID());

                if (entry.IsGoogleDocImport()) {
                    int categoryID = entry.getCategoryID();
                    switch (categoryID) {
                        case -1:
                            category = categoryService.getCategoryByName(SpecialCategories.THEORY);
                            break;
                        case -4:
                            category = categoryService.getCategoryByName(SpecialCategories.MEETING);
                            break;
                        case -5:
                            category = categoryService.getCategoryByName(SpecialCategories.PAIR_PROGRAMMING);
                            break;
                        case -6:
                            category = categoryService.getCategoryByName(SpecialCategories.PROGRAMMING);
                            break;
                        case -7:
                            category = categoryService.getCategoryByName(SpecialCategories.RESEARCH);
                            break;
                        case -8:
                            category = categoryService.getCategoryByName(SpecialCategories.PLANNING_GAME);
                            break;
                        case -9:
                            category = categoryService.getCategoryByName(SpecialCategories.REFACTORING);
                            break;
                        case -10:
                            category = categoryService.getCategoryByName(SpecialCategories.REFACTORING_PP);
                            break;
                        case -11:
                            category = categoryService.getCategoryByName(SpecialCategories.CODE_ACCEPTANCE);
                            break;
                        case -12:
                            category = categoryService.getCategoryByName(SpecialCategories.ORGANISATIONAL_TASKS);
                            break;
                        case -13:
                            category = categoryService.getCategoryByName(SpecialCategories.DISCUSSING_ISSUES_SUPPORTING_CONSULTING);
                            break;
                        case -14:
                            category = categoryService.getCategoryByName(SpecialCategories.INACTIVE);
                            break;
                        case -15:
                            category = categoryService.getCategoryByName(SpecialCategories.OTHER);
                            break;
                        case -16:
                            category = categoryService.getCategoryByName(SpecialCategories.BUG_FIXING_PP);
                            break;
                        case -17:
                            category = categoryService.getCategoryByName(SpecialCategories.BUG_FIXING);
                            break;
                        case -2:
                        default:
                            category = categoryService.getCategoryByName(SpecialCategories.GOOGLEDOCSIMPORT);
                    }
                }

                // Category cannot be associated with team, check null only
                // teamService.checkIfCategoryIsAssociatedWithTeam(team, category);

                TimesheetEntry newEntry = entryService.add(sheet, entry.getBeginDate(), entry.getEndDate(), category,
                        entry.getDescription(), entry.getPauseMinutes(), team, entry.IsGoogleDocImport(),
                        entry.getInactiveEndDate(), entry.getTicketID(), entry.getPairProgrammingUserName(),
                        entry.getTeamroom());
                entry.setEntryID(newEntry.getID());
                errorMap.get("correct").add(entry);
            } catch (ParseException | ServiceException | PermissionException e) {
                if (!errorMap.containsKey(e.getMessage())) {
                    errorMap.put(e.getMessage(), new ArrayList<>());
                }
                errorMap.get(e.getMessage()).add(entry);
            }
        }

        if (errorMap.size() > 1) {
            return Response.status(Response.Status.CONFLICT).entity(errorMap).build();
        }

        return Response.ok(entries).build();
    }

    @POST
    @Path("timesheets/update/{timesheetID}")
    public Response postTimesheetHours(@Context HttpServletRequest request,
                                       final JsonTimesheet jsonTimesheet,
                                       @PathParam("timesheetID") int timesheetID) {
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

        Timesheet sheet = sheetService.getTimesheetByID(timesheetID);
        try {
            RestUtils.checkTimesheetIsEditableByUser(user, sheet, permissionService);
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        try {
            if (permissionService.isJiraAdministrator(user)) {
                sheet = sheetService.editTimesheets(sheet.getUserKey(), jsonTimesheet.getTargetHourPractice(),
                        jsonTimesheet.getTargetHours(), jsonTimesheet.getTargetHoursCompleted(),
                        jsonTimesheet.getTargetHoursRemoved(), jsonTimesheet.getLectures(), jsonTimesheet.getReason(),
                        jsonTimesheet.getLatestEntryDate(), jsonTimesheet.getState());
            } else {
                sheet = sheetService.editTimesheets(ComponentAccessor.
                                getUserKeyService().getKeyForUsername(user.getUsername()), jsonTimesheet.getTargetHourPractice(),
                        jsonTimesheet.getTargetHours(), jsonTimesheet.getTargetHoursCompleted(),
                        jsonTimesheet.getTargetHoursRemoved(), jsonTimesheet.getLectures(), jsonTimesheet.getReason(),
                        jsonTimesheet.getLatestEntryDate(), jsonTimesheet.getState());
            }
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
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

    @POST
    @Path("timesheets/{timesheetID}/updateEnableState/{enabled}")
    public Response postTimesheetEnableState(@Context HttpServletRequest request,
                                             @PathParam("timesheetID") int timesheetID,
                                             @PathParam("enabled") Boolean enabled) throws ServiceException {
        Response response = permissionService.checkRootPermission();
        if (response != null) {
            return response;
        }

        Timesheet sheet = sheetService.getTimesheetByID(timesheetID);
        if (sheet != null) {
            sheet = sheetService.updateTimesheetEnableState(timesheetID, enabled);
        }

        JsonTimesheet jsonTimesheet = new JsonTimesheet(sheet);
        return Response.ok(jsonTimesheet).build();
    }

    @PUT
    @Path("entries/{entryID}")
    public Response putTimesheetEntry(@Context HttpServletRequest request,
                                      final JsonTimesheetEntry jsonEntry,
                                      @PathParam("entryID") int entryID) {
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        if (jsonEntry.getDescription().isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("The 'Task Description' field must not be empty.").build();
        }

        ApplicationUser user;
        TimesheetEntry entry = entryService.getEntryByID(entryID);
        Category category = categoryService.getCategoryByID(jsonEntry.getCategoryID());
        Team team = teamService.getTeamByID(jsonEntry.getTeamID());
        Timesheet sheet = entry.getTimeSheet();
        try {
            RestUtils.checkTimesheetIsEnabled(sheet);
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        try {
            user = permissionService.checkIfUserExists();
            permissionService.userCanEditTimesheetEntry(user, entry);
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        try {
            teamService.checkIfCategoryIsAssociatedWithTeam(team, category);
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }

        if (categoryService.isPairProgrammingCategory(category) && jsonEntry.getPairProgrammingUserName().isEmpty()) {
            return Response.status(Response.Status.CONFLICT).entity("Pair Programming Partner is missing!").build();
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
                    jsonEntry.getInactiveEndDate(), jsonEntry.getTicketID(), jsonEntry.getPairProgrammingUserName(),
                    jsonEntry.getTeamroom());
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

        return Response.ok(jsonEntry).build();
    }

    @DELETE
    @Path("entries/{entryID}")
    public Response deleteTimesheetEntry(@Context HttpServletRequest request,
                                         @PathParam("entryID") int entryID) {
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

        TimesheetEntry entry = entryService.getEntryByID(entryID);
        Timesheet sheet = entry.getTimeSheet();

        try {
            permissionService.userCanDeleteTimesheetEntry(user, entry);
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.CONFLICT).entity("Timesheet not found.").build();
        }

        if (sheet.getState() == Timesheet.State.DISABLED) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
        } else if (!permissionService.userCanViewTimesheet(user, sheet)) {
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

        if (entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0) {
            state = Timesheet.State.ACTIVE;
        }

        int deducted_hours = sheet.getHoursCompleted() - entry.getDurationMinutes() / 60;

        entryService.delete(entry);

        //update latest timesheet entry date if latest entry date is < new latest entry in the table
        if (sheet.getEntries().length > 0) {
            if (entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate()) > 0) {
                try {
                    sheetService.editTimesheets(ComponentAccessor.
                                    getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getHoursPracticeCompleted(),
                            sheet.getTargetHours(), deducted_hours,
                            sheet.getHoursDeducted(), sheet.getLectures(), sheet.getReason(),
                            entryService.getLatestEntry(sheet).getBeginDate(), state);
                } catch (ServiceException e) {
                    return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
                }
            }
        } else {
            try {
                sheetService.editTimesheets(ComponentAccessor.
                                getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getHoursPracticeCompleted(),
                        sheet.getTargetHours(), deducted_hours,
                        sheet.getHoursDeducted(), sheet.getLectures(), sheet.getReason(),
                        new Date(), sheet.getState());
            } catch (ServiceException e) {
                return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
            }
        }
        return Response.ok().build();
    }

    // This is a demo API REST method to show you how to use the powerful and wonderful JqlQueryBuilder class
//    @GET
//    @Path("issueTickets")
//    public Response getIssueTickets(@Context HttpServletRequest request) throws ServiceException {
//        try {
//            permissionService.checkIfUserExists();
//        } catch (PermissionException e) {
//            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
//        }
//
//        SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
//        ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
//
//        final JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
//
//        //builder.where().project("DEMO");
//        builder.where().assigneeUser("Admin");
//        Query query = builder.buildQuery();
//
//        List<Issue> issues = null;
//
//        try {
//            SearchResults results = searchService.search(currentUser, query, PagerFilter.getUnlimitedFilter());
//            issues = results.getIssues();
//
//            for (Issue issue : issues) {
//                String displayName = issue.getAssignee().getDisplayName();
//                logger.debug("displayName = {}", displayName);
//            }
//
//        } catch (SearchException e) {
//            logger.warn("Error running search: {}", e);
//        }
//
//        JSONObject jo = new JSONObject();
//        try {
//            for (Issue issue : issues) {
//                JSONObject item = new JSONObject();
//                item.put("assignee", issue.getAssignee());
//                item.put("id", issue.getId());
//                jo.append("issues", item);
//            }
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        return Response.ok(jo.toString()).build();
//    }

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

        timesheets.sort(timesheetComparator);
        for (Timesheet timesheet : timesheets) {
            String userKey = timesheet.getUserKey();
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
            String entry = user.getDisplayName() + "  (" + timesheet.getLatestEntryBeginDate().toString() + ")";
            users.add(entry);
        }

        String jsonList = new Gson().toJson(users);
        return Response.ok(jsonList).build();
    }

    @DELETE
    @Path("/delete/{timesheetId}")
    public Response deleteTimesheet(@PathParam("timesheetId") int timesheet_id) {
        Response unauthorized = permissionService.checkRootPermission();
        if (unauthorized != null) {
            return unauthorized;
        }
        Timesheet sheet = sheetService.getTimesheetByID(timesheet_id);
        if (sheet == null) {
            return Response.status(Response.Status.CONFLICT).entity("The given sheet with id: " + timesheet_id + " does not exist!").build();
        }
        ApplicationUser current_user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        if (permissionService.isUserEligibleForTimesheet(current_user)) {
            try {
                sheetService.remove(sheet);
                return Response.ok().build();

            } catch (ServiceException e) {
                return Response.serverError().entity(e.getMessage()).build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("The current user does not have the required access" +
                    " rights to delete the timesheet with id: " + timesheet_id).build();
        }
    }

    @PUT
    @Path("/reactivate/{timesheetId}")
    public Response reactivateTimsheet(@PathParam("timesheetId") int timesheet_id) {
        LOGGER.error("trying to reactivate timesheet with id: " + timesheet_id);

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }
        Timesheet timesheet = sheetService.getTimesheetByID(timesheet_id);
        if (timesheet == null) {
            return Response.status(Response.Status.CONFLICT).entity("No timehseet with id: " + timesheet_id + " was found.").build();
        }
        Timesheet.State current_state = timesheet.getState();

        if (current_state == Timesheet.State.INACTIVE || current_state == Timesheet.State.INACTIVE_OFFLINE ||
                current_state == Timesheet.State.AUTO_INACTIVE) {
            LOGGER.error("we got an inactive timesheet processing...");
            try {
                TimesheetEntry latest_entry = entryService.getLatestInactiveEntry(timesheet);
                timesheet.setState(Timesheet.State.ACTIVE);
                timesheet.save();

                //in this inactive workflow the last entry must be the inactive one
                Date inactive_begin_date = latest_entry.getBeginDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(inactive_begin_date);
                //today
                Calendar cal1 = Calendar.getInstance();
                cal1.setTime(new Date());

                boolean is_same_day = cal.get(Calendar.YEAR) == cal1.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == cal1.get(Calendar.DAY_OF_YEAR);

                if (is_same_day) {
                    LOGGER.trace("We want to edit a date which was created today and therefore we delete it");

                    entryService.delete(latest_entry);
                } else {
                    LOGGER.trace("Setting endDate to today");
                    latest_entry.setInactiveEndDate(new Date());
                    latest_entry.save();
                }
            } catch (Exception e) {
                return Response.serverError().entity(e.getMessage()).build();
            }
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_MODIFIED).entity("Timesheet is already in active state").build();
        }
    }

    @GET
    @Path("/getTeamEntries/{teamId}")
    public Response getTeamEntries(@PathParam("teamId") int teamId){
        ApplicationUser user;
        Team team = teamService.getTeamByID(teamId);
        if(team == null){
           return Response.status(Response.Status.CONFLICT).entity("Team with ID " + teamId + " does not Exist").build();
        }
        try {
           user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        if(!(permissionService.isUserTeamCoordinator(user) || permissionService.isTimesheetAdmin(user) ||
                permissionService.isReadOnlyUser(user) || teamService.isUserInTeam(team, user.getUsername()))){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        LOGGER.trace("getting entries for team with id: " + teamId);
        TimesheetEntry[] entries = teamService.getTeamEntriesById(teamId);

        if(entries.length == 0){
            return Response.status(Response.Status.CONFLICT).entity("NO entries were found!").build();
        }

        ArrayList<JsonTimesheetEntry> response_set = new ArrayList<>();
        for(TimesheetEntry entry : entries){
            response_set.add(new JsonTimesheetEntry(entry, true));
        }

        Collections.reverse(response_set);

        return Response.ok(response_set).build();
    }

    @GET
    @Path("getMostActiveTeamForUser/{userKey}")
    public Response getMostActiveTeamForUser(@PathParam("userKey") String user_key){
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
        ApplicationUser to_query = ComponentAccessor.getUserManager().getUserByKey(user_key);
        if(to_query == null){
            return Response.status(Response.Status.CONFLICT).entity("The user does not exist").build();
        }
        try {
            Timesheet sheet_of_user = sheetService.getTimesheetByUser(user_key);
            Map<String, Object> result = new HashMap<>();
            Team most_active_team = teamService.getMostActiveTeamForUser(to_query.getUsername(), sheet_of_user);

            result.put("teamName", most_active_team.getTeamName());
            result.put("teamID", most_active_team.getID());

            return Response.ok().entity(result).build();
        }catch (Exception e){
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("getTeamsOfUser")
    public Response getTeamsOfUser(){
        ApplicationUser user;
        ArrayList<Object> response_list = new ArrayList<>();
        ArrayList<String> team_names = new ArrayList<>();
        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }
        if(permissionService.isUserTeamCoordinator(user)){
            Set<Team> teams = teamService.getTeamsOfCoordinator(user.getUsername());
            for(Team team : teams){
                team_names.add(team.getTeamName());
            }

            response_list.addAll(generateTeamResponse(teams, null));
        }

        Set<Team> teams = teamService.getTeamsOfUser(user.getUsername());

        response_list.addAll(generateTeamResponse(teams, team_names));

        return Response.ok().entity(response_list).build();
    }

    private ArrayList<Object> generateTeamResponse(Set<Team> teams, ArrayList<String> team_names){
        ArrayList<Object> response_teams = new ArrayList<>();
        for(Team team : teams){
            if(team_names != null){
                if(team_names.contains(team.getTeamName()))
                    continue;
            }
            Map<String, Object> current_team_json = new HashMap<>();
            current_team_json.put("teamName", team.getTeamName());
            current_team_json.put("teamID", team.getID());

            response_teams.add(current_team_json);
        }
        return response_teams;
    }

    @POST
    @Path("updateTimesheetReasonData")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setTimesheetReasonData(@Context HttpServletRequest request,
                                           final JsonTimesheetReasonData jsonTimesheetReasonData){
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

        LOGGER.trace("We want to save data: " + jsonTimesheetReasonData);
        try{
            Timesheet sheet = sheetService.getTimesheetByUser(user.getKey());
            sheetService.updateTimesheetReasonData(sheet, jsonTimesheetReasonData);

            return Response.ok(new JsonTimesheet(sheet)).build();
        }
        catch (ServiceException e){
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("updateTotalTargetHours/{hours}")
    public Response updateTotalTargetHours(@Context HttpServletRequest request, @PathParam("hours") int hours){
        ApplicationUser user;

        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null)
            return response;

        try{
            Timesheet sheet = sheetService.getTimesheetByUser(user.getKey());

            if(sheet != null) {
                sheet.setTargetHours(hours);
                sheet.save();
            }

            return Response.ok(new JsonTimesheet(sheet)).build();
        }
        catch (ServiceException e){
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("deleteLecture")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteLecture(@Context HttpServletRequest request, final JsonTimesheetReasonData data){
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

        try{
            Timesheet sheet = sheetService.getTimesheetByUser(user.getKey());
            sheetService.deleteLecture(sheet, data);
            return Response.ok(new JsonTimesheet(sheet)).build();
        }
        catch (ServiceException e){
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }
}