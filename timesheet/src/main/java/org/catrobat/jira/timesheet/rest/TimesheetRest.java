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

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.exception.InvalidCredentialException;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.mail.Email;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.rest.json.*;
import org.catrobat.jira.timesheet.services.*;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


@Path("/")
@Produces({MediaType.APPLICATION_JSON})
public class TimesheetRest {

    private final TimesheetEntryService entryService;
    private final TimesheetService sheetService;
    private final CategoryService categoryService;
    private final TeamService teamService;
    private final UserManager userManager;
    private final PermissionService permissionService;
    private final DBFillerService dbfiller;
    private final ConfigService configService;
    private final UserPreferencesManager userPreferencesManager;
    private final GroupManager groupManager;

    public TimesheetRest(final TimesheetEntryService es, final TimesheetService ss, final CategoryService cs,
                         final UserManager um, final TeamService ts, PermissionService ps,
                         final DBFillerService df, final ConfigService ahcs,
                         final GroupManager gm, final UserPreferencesManager upm) {
        this.userManager = um;
        this.teamService = ts;
        this.entryService = es;
        this.sheetService = ss;
        this.categoryService = cs;
        this.permissionService = ps;
        this.dbfiller = df;
        this.configService = ahcs;
        this.groupManager = gm;
        this.userPreferencesManager = upm;
    }

    private void checkIfCategoryIsAssociatedWithTeam(@Nullable Team team, @Nullable Category category) throws InvalidCredentialException {

        if (team == null) {
            throw new InvalidCredentialException("Team not found.");
        }

        if (category == null) {
            throw new InvalidCredentialException("Category not found.");
        }

        if (!Arrays.asList(team.getCategories()).contains(category)) {
            throw new InvalidCredentialException("Category is not associated with Team.");
        }
    }

    @GET
    @Path("teams")
    public Response getTeams(@Context HttpServletRequest request) throws ServiceException {

        List<JsonTeam> teams = new LinkedList<JsonTeam>();
        UserProfile user;

        user = permissionService.checkIfUserExists(request);

        String userName = user.getUsername();

        for (Team team : teamService.getTeamsOfUser(userName)) {
            Category[] categories = team.getCategories();
            int[] categoryIDs = new int[categories.length];
            for (int i = 0; i < categories.length; i++) {
                categoryIDs[i] = categories[i].getID();
            }
            teams.add(new JsonTeam(team.getID(), team.getTeamName(), categoryIDs));
        }

        return Response.ok(teams).build();
    }

    @GET
    @Path("teams/{timesheetID}")
    public Response getTimesheetTeams(@Context HttpServletRequest request,
                                      @PathParam("timesheetID") int timesheetID) throws ServiceException {

        List<JsonTeam> teams = new LinkedList<JsonTeam>();
        Collection<User> allUsers = ComponentAccessor.getUserManager().getAllUsers();
        UserProfile userProfile;

        userProfile = permissionService.checkIfUserExists(request);

        for (User user : allUsers) {
            if (sheetService.getTimesheetByID(timesheetID).getUserKey().equals(ComponentAccessor.getUserManager().
                    getUserByName(user.getName()).getKey().toString())) {
                for (Team team : teamService.getTeamsOfUser(user.getName())) {
                    Category[] categories = team.getCategories();
                    int[] categoryIDs = new int[categories.length];
                    for (int i = 0; i < categories.length; i++) {
                        categoryIDs[i] = categories[i].getID();
                    }
                    teams.add(new JsonTeam(team.getID(), team.getTeamName(), categoryIDs));
                }
            }
        }

        return Response.ok(teams).build();
    }

    @GET
    @Path("categories")
    public Response getCategories(@Context HttpServletRequest request) {

        List<JsonCategory> categories = new LinkedList<JsonCategory>();

        for (Category category : categoryService.all()) {
            categories.add(new JsonCategory(category.getID(), category.getName()));
        }

        return Response.ok(categories).build();
    }

    @GET
    @Path("teamInformation")
    public Response getAllTeams(@Context HttpServletRequest request) {

        List<JsonTeam> teams = new LinkedList<JsonTeam>();

        for (Team team : teamService.all()) {
            int[] teamCategoryIDs = new int[team.getCategories().length];
            for (int i = 0; i < team.getCategories().length; i++) {
                teamCategoryIDs[i] = team.getCategories()[i].getID();
            }

            teams.add(new JsonTeam(team.getID(), team.getTeamName(), teamCategoryIDs));
        }

        return Response.ok(teams).build();
    }

    @GET
    @Path("timesheet/{timesheetID}/teamEntries")
    public Response getTeamTimesheetEntries(@Context HttpServletRequest request,
                                            @PathParam("timesheetID") int timesheetID) throws ServiceException {

        List<JsonTimesheetEntry> jsonTimesheetEntries = new LinkedList<JsonTimesheetEntry>();
        Collection<User> allUsers = ComponentAccessor.getUserManager().getAllUsers();
        UserProfile userProfile;

        userProfile = permissionService.checkIfUserExists(request);

        for (User user : allUsers) {
            //get user name
            if (sheetService.getTimesheetByID(timesheetID).getUserKey().equals(ComponentAccessor.getUserManager().
                    getUserByName(user.getName()).getKey().toString())) {
                //get all teams of that user
                for (Team team : teamService.getTeamsOfUser(user.getName())) {
                    //get all team members
                    for (String teamMember : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.DEVELOPER)) {
                        if (user.getName().compareTo(teamMember) == 0) {
                            //collect all timesheet entries of those team members
                            Timesheet sheet = sheetService.getTimesheetByUser(
                                    ComponentAccessor.getUserManager().getUserByName(teamMember).getKey().toString());
                            //all entries of each user
                            TimesheetEntry[] entries = entryService.getEntriesBySheet(sheet);
                            for (TimesheetEntry entry : entries) {
                                jsonTimesheetEntries.add(new JsonTimesheetEntry(entry.getID(), entry.getBeginDate(),
                                        entry.getEndDate(), entry.getPauseMinutes(),
                                        entry.getDescription(), entry.getTeam().getID(),
                                        entry.getCategory().getID(), entry.getIsGoogleDocImport()));
                            }
                        }
                    }
                }
            }
        }

        return Response.ok(jsonTimesheetEntries).build();
    }

    @GET
    @Path("timesheetID/fromUser/{userName}")
    public Response getTimesheetIDForUser(@Context HttpServletRequest request,
                                          @PathParam("userName") String userName) throws ServiceException {

        Timesheet sheet;
        UserProfile user;

        try {
            user = permissionService.checkIfUsernameExists(userName);
            sheet = sheetService.getTimesheetByUser(ComponentAccessor.
                    getUserKeyService().getKeyForUsername(user.getUsername()));
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }


        if (sheet == null || !permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.ok(sheet.getID()).build();
    }

    @GET
    @Path("timesheets/owner/{timesheetID}")
    public Response getTimesheetOwner(@Context HttpServletRequest request,
                                      @PathParam("timesheetID") int timesheetID) throws ServiceException {

        Timesheet sheet;
        UserProfile user;

        try {
            user = permissionService.checkIfUserExists(request);
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        if (sheet == null || !permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        JsonUser jsonUser = new JsonUser();
        jsonUser.setUserName(user.getUsername());

        return Response.ok(jsonUser).build();
    }

    @GET
    @Path("timesheets/{timesheetID}")
    public Response getTimesheet(@Context HttpServletRequest request,
                                 @PathParam("timesheetID") int timesheetID) throws ServiceException {

        Timesheet sheet;
        UserProfile user;

        try {
            user = permissionService.checkIfUserExists(request);
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        if (sheet == null || !permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (sheet.getTargetHoursTheory() < 10) {
            sendEmailNotification(user.getEmail(), "time", sheet, user);
        }

        JsonTimesheet jsonTimesheet = new JsonTimesheet(timesheetID, sheet.getLectures(), sheet.getReason(),
                sheet.getEcts(), sheet.getLatestEntryDate(), sheet.getTargetHoursPractice(),
                sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                sheet.getTargetHoursRemoved(), sheet.getIsActive(), sheet.getIsEnabled());

        return Response.ok(jsonTimesheet).build();
    }

    @GET
    @Path("coordinator/{timesheetID}/entries")
    public Response getTimesheetEntriesCoordinator(@Context HttpServletRequest request,
                                                   @PathParam("timesheetID") int timesheetID) throws ServiceException {

        Timesheet sheet;
        UserProfile user;

        try {
            sheet = sheetService.getTimesheetByID(timesheetID);
            user = permissionService.checkIfUserExists(request);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        TimesheetEntry[] entries = entryService.getEntriesBySheet(sheet);

        if (dateIsOlderThanTwoWeeks(entries[0].getBeginDate())) {
            sendEmailNotification(user.getEmail(), "inactive", sheet, user);
        }

        List<JsonTimesheetEntry> jsonEntries = new ArrayList<JsonTimesheetEntry>(entries.length);

        for (TimesheetEntry entry : entries) {
            jsonEntries.add(new JsonTimesheetEntry(entry.getID(), entry.getBeginDate(),
                    entry.getEndDate(), entry.getPauseMinutes(),
                    entry.getDescription(), entry.getTeam().getID(),
                    entry.getCategory().getID(), entry.getIsGoogleDocImport()));
        }
        return Response.ok(jsonEntries).build();
    }

    @GET
    @Path("coordinator/{timesheetID}")
    public Response getTimesheetCoordinator(@Context HttpServletRequest request,
                                            @PathParam("timesheetID") int timesheetID) throws ServiceException {

        Timesheet sheet;

        try {
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        if (sheet == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        JsonTimesheet jsonTimesheet = new JsonTimesheet(timesheetID, sheet.getLectures(), sheet.getReason(),
                sheet.getEcts(), sheet.getLatestEntryDate(), sheet.getTargetHoursPractice(),
                sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                sheet.getTargetHoursRemoved(), sheet.getIsActive(), sheet.getIsEnabled());

        return Response.ok(jsonTimesheet).build();
    }

    @GET
    @Path("timesheets/{timesheetID}/entries")
    public Response getTimesheetEntries(@Context HttpServletRequest request,
                                        @PathParam("timesheetID") int timesheetID) throws ServiceException {

        Timesheet sheet;
        UserProfile user;

        try {
            user = permissionService.checkIfUserExists(request);
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        if (sheet == null || !permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        TimesheetEntry[] entries = entryService.getEntriesBySheet(sheet);

        //update latest entry date value
        if (entries.length > 0) {
            sheetService.editTimesheet(ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername()),
                    sheet.getTargetHoursPractice(), sheet.getTargetHoursTheory(), sheet.getTargetHours(),
                    sheet.getTargetHoursCompleted(), sheet.getTargetHoursRemoved(), sheet.getLectures(),
                    sheet.getReason(), sheet.getEcts(), entries[0].getBeginDate().toString(), sheet.getIsActive(),
                    sheet.getIsEnabled());
        }

        List<JsonTimesheetEntry> jsonEntries = new ArrayList<JsonTimesheetEntry>(entries.length);

        for (TimesheetEntry entry : entries) {
            jsonEntries.add(new JsonTimesheetEntry(entry.getID(), entry.getBeginDate(),
                    entry.getEndDate(), entry.getPauseMinutes(),
                    entry.getDescription(), entry.getTeam().getID(),
                    entry.getCategory().getID(), entry.getIsGoogleDocImport()));
        }
        return Response.ok(jsonEntries).build();
    }

    @GET
    @Path("timesheets/getTimesheets")
    public Response getTimesheets(@Context HttpServletRequest request) throws ServiceException {

        List<Timesheet> timesheetList = new LinkedList<Timesheet>();
        List<JsonTimesheet> jsonTimesheetList = new ArrayList<JsonTimesheet>();
        Collection<User> allUsers = ComponentAccessor.getUserManager().getAllUsers();
        UserProfile userProfile;

        userProfile = permissionService.checkIfUserExists(request);
        timesheetList = sheetService.all();

        if (timesheetList == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        for (User user : allUsers) {
            JsonTimesheet jsonTimesheet = new JsonTimesheet();

            boolean isActive = false;
            boolean isEnabled = false;
            String latestEntryDate = "Not Available";
            int timesheetID = 0;

            for (Timesheet timesheet : timesheetList) {
                if (timesheet.getUserKey().equals(ComponentAccessor.getUserManager().
                        getUserByName(user.getName()).getKey().toString())) {
                    isActive = timesheet.getIsActive();
                    isEnabled = timesheet.getIsEnabled();
                    latestEntryDate = timesheet.getLatestEntryDate();
                    timesheetID = timesheet.getID();
                }
            }

            if (user.getName().compareTo("admin") != 0) {
                jsonTimesheet.setActive(isActive);
                jsonTimesheet.setEnabled(isEnabled);
                jsonTimesheet.setLatestEntryDate(latestEntryDate);
                jsonTimesheet.setTimesheetID(timesheetID);
                jsonTimesheetList.add(jsonTimesheet);
            }
        }

        return Response.ok(jsonTimesheetList).build();
    }

    @POST
    @Path("timesheets/{timesheetID}/entry")
    public Response postTimesheetEntry(@Context HttpServletRequest request,
                                       final JsonTimesheetEntry entry, @PathParam("timesheetID") int timesheetID) throws ServiceException, InvalidCredentialException {

        if (entry.getDescription().isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("The 'Task Description' field must not be empty.").build();
        }

        Timesheet sheet;
        UserProfile user;
        Category category;
        Team team;

        try {
            user = permissionService.checkIfUserExists(request);
            sheet = sheetService.getTimesheetByID(timesheetID);
            category = categoryService.getCategoryByID(entry.getCategoryID());
            team = teamService.getTeamByID(entry.getTeamID());
            checkIfCategoryIsAssociatedWithTeam(team, category);
            permissionService.userCanEditTimesheetEntry(user, sheet, entry);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (com.atlassian.jira.exception.PermissionException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        if (!sheet.getIsEnabled()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
        }

        TimesheetEntry newEntry = entryService.add(sheet, entry.getBeginDate(),
                entry.getEndDate(), category, entry.getDescription(),
                entry.getPauseMinutes(), team, entry.getIsGoogleDocImport());

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        //update latest timesheet entry date if latest entry date is < new latest entry in the table
        if (sheet.getEntries().length == 1) {
            sheetService.editTimesheet(ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername()),
                    sheet.getTargetHoursPractice(), sheet.getTargetHoursTheory(), sheet.getTargetHours(),
                    sheet.getTargetHoursCompleted(), sheet.getTargetHoursRemoved(), sheet.getLectures(),
                    sheet.getReason(), sheet.getEcts(), df.format(entryService.getEntriesBySheet(sheet)[0].
                            getBeginDate()), sheet.getIsActive(), sheet.getIsEnabled());
        } else if (entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate()) >= 0) {
            sheetService.editTimesheet(ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername()),
                    sheet.getTargetHoursPractice(), sheet.getTargetHoursTheory(), sheet.getTargetHours(),
                    sheet.getTargetHoursCompleted(), sheet.getTargetHoursRemoved(), sheet.getLectures(),
                    sheet.getReason(), sheet.getEcts(), df.format(entry.getBeginDate()), sheet.getIsActive(),
                    sheet.getIsEnabled());
        }

        entry.setEntryID(newEntry.getID());

        return Response.ok(entry).build();
    }

    @POST
    @Path("timesheets/{timesheetID}/entries")
    public Response postTimesheetEntries(@Context HttpServletRequest request,
                                         final JsonTimesheetEntry[] entries, @PathParam("timesheetID") int timesheetID) throws ServiceException, InvalidCredentialException, com.atlassian.jira.exception.PermissionException {

        Timesheet sheet;
        UserProfile user;

        try {
            user = permissionService.checkIfUserExists(request);
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        if (!sheet.getIsEnabled()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
        }

        List<JsonTimesheetEntry> newEntries = new LinkedList<JsonTimesheetEntry>();
        List<String> errorMessages = new LinkedList<String>();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (JsonTimesheetEntry entry : entries) {
            if (entry.getDescription().isEmpty()) {
                return Response.status(Response.Status.FORBIDDEN).entity("The 'Task Description' field must not be empty.").build();
            }

            try {
                permissionService.userCanEditTimesheetEntry(user, sheet, entry);
                Category category = categoryService.getCategoryByID(entry.getCategoryID());
                Team team = teamService.getTeamByID(entry.getTeamID());
                checkIfCategoryIsAssociatedWithTeam(team, category);

                TimesheetEntry newEntry = entryService.add(sheet, entry.getBeginDate(),
                        entry.getEndDate(), category, entry.getDescription(),
                        entry.getPauseMinutes(), team, entry.getIsGoogleDocImport());

                //update latest timesheet entry date if latest entry date is < new latest entry in the table
                if (sheet.getEntries().length == 1) {
                    sheetService.editTimesheet(ComponentAccessor.
                                    getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                            sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                            sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(), sheet.getEcts(),
                            df.format(entryService.getEntriesBySheet(sheet)[0].getBeginDate()), sheet.getIsActive(),
                            sheet.getIsEnabled());
                } else if (entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate()) >= 0) {
                    sheetService.editTimesheet(ComponentAccessor.
                                    getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                            sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                            sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(), sheet.getEcts(),
                            df.format(entryService.getEntriesBySheet(sheet)[0].getBeginDate()), sheet.getIsActive(),
                            sheet.getIsEnabled());
                }

                entry.setEntryID(newEntry.getID());
                newEntries.add(entry);

            } catch (ServiceException e) {
                errorMessages.add(entry.toReadableString() + ": " + e.getMessage());
            }
        }

        JsonTimesheetEntries jsonNewEntries = new JsonTimesheetEntries(
                newEntries.toArray(new JsonTimesheetEntry[newEntries.size()]),
                errorMessages.toArray(new String[errorMessages.size()])
        );

        return Response.ok(jsonNewEntries).build();
    }

    @POST
    @Path("timesheets/update/{timesheetID}")
    public Response postTimesheetHours(@Context HttpServletRequest request,
                                       final JsonTimesheet jsonTimesheet, @PathParam("timesheetID") int timesheetID) throws ServiceException {

        Timesheet sheet;
        UserProfile user;

        try {
            user = permissionService.checkIfUserExists(request);
            sheet = sheetService.getTimesheetByID(timesheetID);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        if (sheet == null || !permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (!sheet.getIsEnabled()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
        }

        if (userManager.isAdmin(ComponentAccessor.
                getUserKeyService().getKeyForUsername(user.getUsername()))) {
            sheetService.editTimesheet(sheet.getUserKey(), jsonTimesheet.getTargetHourPractice(),
                    jsonTimesheet.getTargetHourTheory(), jsonTimesheet.getTargetHours(), jsonTimesheet.getTargetHoursCompleted(),
                    jsonTimesheet.getTargetHoursRemoved(), jsonTimesheet.getLectures(), jsonTimesheet.getReason(),
                    jsonTimesheet.getEcts(), jsonTimesheet.getLatestEntryDate(), sheet.getIsActive(), sheet.getIsEnabled());
        } else {
            sheetService.editTimesheet(ComponentAccessor.
                            getUserKeyService().getKeyForUsername(user.getUsername()), jsonTimesheet.getTargetHourPractice(),
                    jsonTimesheet.getTargetHourTheory(), jsonTimesheet.getTargetHours(), jsonTimesheet.getTargetHoursCompleted(),
                    jsonTimesheet.getTargetHoursRemoved(), jsonTimesheet.getLectures(), jsonTimesheet.getReason(),
                    jsonTimesheet.getEcts(), jsonTimesheet.getLatestEntryDate(), sheet.getIsActive(), sheet.getIsEnabled());
        }


        JsonTimesheet newJsonTimesheet = new JsonTimesheet(timesheetID, sheet.getLectures(), sheet.getReason(),
                sheet.getEcts(), sheet.getLatestEntryDate(), sheet.getTargetHoursPractice(), sheet.getTargetHoursTheory(),
                sheet.getTargetHours(), sheet.getTargetHoursCompleted(), sheet.getTargetHoursRemoved(), sheet.getIsActive(),
                sheet.getIsEnabled());

        return Response.ok(newJsonTimesheet).build();
    }

    @POST
    @Path("timesheets/updateEnableStates")
    public Response postTimesheetEnableStates(@Context HttpServletRequest request,
                                              final JsonTimesheet[] jsonTimesheetList) throws ServiceException {

        Timesheet sheet;
        UserProfile user;
        List<JsonTimesheet> newJsonTimesheets = new LinkedList<JsonTimesheet>();

        user = permissionService.checkIfUserExists(request);

        if (jsonTimesheetList == null) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        for (JsonTimesheet jsonTimesheet : jsonTimesheetList) {

            sheet = sheetService.getTimesheetByID(jsonTimesheet.getTimesheetID());

            if (sheet != null) {

                sheetService.updateTimesheetEnableState(jsonTimesheet.getTimesheetID(), jsonTimesheet.isEnabled());

                JsonTimesheet newJsonTimesheet = new JsonTimesheet(sheet.getID(), sheet.getLectures(), sheet.getReason(),
                        sheet.getEcts(), sheet.getLatestEntryDate(), sheet.getTargetHoursPractice(),
                        sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                        sheet.getTargetHoursRemoved(), sheet.getIsActive(), sheet.getIsEnabled());

                newJsonTimesheets.add(newJsonTimesheet);
            }
        }

        return Response.ok(newJsonTimesheets).build();
    }

    @PUT
    @Path("entries/{entryID}")
    public Response putTimesheetEntry(@Context HttpServletRequest request,
                                      final JsonTimesheetEntry jsonEntry, @PathParam("entryID") int entryID) throws ServiceException, InvalidCredentialException, com.atlassian.jira.exception.PermissionException {

        if (jsonEntry.getDescription().isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("The 'Task Description' field must not be empty.").build();
        }

        UserProfile user;
        TimesheetEntry entry;
        Category category;
        Team team;
        Timesheet sheet;

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        try {
            user = permissionService.checkIfUserExists(request);
            entry = entryService.getEntryByID(entryID);
            category = categoryService.getCategoryByID(jsonEntry.getCategoryID());
            team = teamService.getTeamByID(jsonEntry.getTeamID());
            sheet = entry.getTimeSheet();
            checkIfCategoryIsAssociatedWithTeam(team, category);
            permissionService.userCanEditTimesheetEntry(user, entry.getTimeSheet(), jsonEntry);
        } catch (ServiceException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }

        if (sheet.getIsEnabled() || permissionService.checkIfUserIsGroupMember(request, "jira-administrators")) {
            entryService.edit(entryID, entry.getTimeSheet(), jsonEntry.getBeginDate(),
                    jsonEntry.getEndDate(), category, jsonEntry.getDescription(),
                    jsonEntry.getPauseMinutes(), team, jsonEntry.getIsGoogleDocImport());

            if (sheet.getEntries().length == 1) {
                sheetService.editTimesheet(ComponentAccessor.
                                getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                        sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                        sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(), sheet.getEcts(),
                        df.format(entryService.getEntriesBySheet(sheet)[0].getBeginDate()), sheet.getIsActive(),
                        sheet.getIsEnabled());
            } else if (entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate()) >= 0) {
                sheetService.editTimesheet(ComponentAccessor.
                                getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                        sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                        sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(), sheet.getEcts(),
                        df.format(entryService.getEntriesBySheet(sheet)[0].getBeginDate()), sheet.getIsActive(),
                        sheet.getIsEnabled());
            }

            return Response.ok(jsonEntry).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
    }

    @DELETE
    @Path("entries/{entryID}")
    public Response deleteTimesheetEntry(@Context HttpServletRequest request,
                                         @PathParam("entryID") int entryID) throws ServiceException, com.atlassian.jira.exception.PermissionException {
        UserProfile user;
        TimesheetEntry entry;
        Timesheet sheet;

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        user = permissionService.checkIfUserExists(request);
        entry = entryService.getEntryByID(entryID);
        permissionService.userCanDeleteTimesheetEntry(user, entry);

        //update latest date
        sheet = sheetService.getTimesheetByUser(ComponentAccessor.
                getUserKeyService().getKeyForUsername(user.getUsername()));

        if (!sheet.getIsEnabled()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Your timesheet has been disabled.").build();
        }

        if (sheet == null || !permissionService.userCanViewTimesheet(user, sheet)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        entryService.delete(entry);

        //update latest timesheet entry date if latest entry date is < new latest entry in the table
        if (sheet.getEntries().length > 0) {
            if (entry.getBeginDate().compareTo(entryService.getEntriesBySheet(sheet)[0].getBeginDate()) > 0) {
                sheetService.editTimesheet(ComponentAccessor.
                                getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                        sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                        sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(), sheet.getEcts(),
                        df.format(entryService.getEntriesBySheet(sheet)[0].getBeginDate()), sheet.getIsActive(),
                        sheet.getIsEnabled());
            }
        } else {
            sheetService.editTimesheet(ComponentAccessor.
                            getUserKeyService().getKeyForUsername(user.getUsername()), sheet.getTargetHoursPractice(),
                    sheet.getTargetHoursTheory(), sheet.getTargetHours(), sheet.getTargetHoursCompleted(),
                    sheet.getTargetHoursRemoved(), sheet.getLectures(), sheet.getReason(), sheet.getEcts(),
                    "Not Available", sheet.getIsActive(), sheet.getIsEnabled());
        }
        return Response.ok().build();
    }

    @GET
    @Path("cleanandinitdb")
    public Response cleanAndInitDB() {

        if (userManager.isAdmin(userManager.getRemoteUsername())) {
            dbfiller.printDBStatus();
            dbfiller.cleanDB();
            dbfiller.printDBStatus();
            dbfiller.insertDefaultData();
            dbfiller.printDBStatus();
            return Response.ok("cleanandinitdb").build();
        } else {
            return Response.ok("you're not admin").build();
        }
    }

    private void sendEmailNotification(String emailTo, String reason, Timesheet sheet, UserProfile user) {
        Config config = configService.getConfiguration();

        //for testing only!
        //emailTo = config.getMailFrom();
        emailTo = "test@test123.com";

        String mailBody = "";
        String mailSubject = "";

        if (reason == "time") {
            mailSubject = config.getMailSubjectTime() != null && config.getMailSubjectTime().length() != 0
                    ? config.getMailSubjectTime() : "Out Of Time Notification";
            mailBody = config.getMailBodyTime() != null && config.getMailBodyTime().length() != 0
                    ? config.getMailBodyTime() : "Hi " + user.getFullName() + ",\n" +
                    "you have only" + sheet.getTargetHoursTheory() + " hours left! \n" +
                    "Please contact you coordinator, or one of the administrators\n\n" +
                    "Best regards,\n" +
                    "Catrobat-Admins";
        } else if (reason == "inactive") {
            mailSubject = config.getMailSubjectInactive() != null && config.getMailSubjectInactive().length() != 0
                    ? config.getMailSubjectInactive() : "Inactive Notification";
            mailBody = config.getMailBodyInactive() != null && config.getMailBodyInactive().length() != 0
                    ? config.getMailBodyInactive() : "Hi " + user.getFullName() + ",\n" +
                    "we could not see any activity in your timesheet since the last two weeks.\n" +
                    "Information: an inactive entry was created automatically.\n\n" +
                    "Best regards,\n" +
                    "Catrobat-Admins";
        } else if (reason == "entry") {
            mailSubject = config.getMailSubjectEntry() != null &&
                    config.getMailSubjectEntry().length() != 0
                    ? config.getMailSubjectEntry() : "Timesheet Entry Changed Notification";
            mailBody = config.getMailBodyEntry() != null &&
                    config.getMailBodyEntry().length() != 0
                    ? config.getMailBodyEntry() : "Hi " + user.getFullName() + ",\n" +
                    "External changes were applied to your timesheet\n" +
                    "Information: OLD ENTRY + NEW ENTRY.\n\n" +
                    "Best regards,\n" +
                    "Catrobat-Admins";
        }

        mailBody = mailBody.replaceAll("\\{\\{name\\}\\}", user.getFullName());
        mailBody = mailBody.replaceAll("\\{\\{time\\}\\}", Integer.toString(sheet.getTargetHoursTheory()));

        if (sheet.getEntries().length > 0) {
            mailBody = mailBody.replaceAll("\\{\\{date\\}\\}", sheet.getEntries()[0].getBeginDate().toString());
        }

        mailBody = mailBody.replaceAll("\\{\\{original\\}\\}", "OLD ENTRY");
        mailBody = mailBody.replaceAll("\\{\\{actual\\}\\}", "NEW ENTRY");


        Email email = new Email(emailTo);
        email.setSubject(mailSubject);
        email.setBody(mailBody);

        SingleMailQueueItem item = new SingleMailQueueItem(email);
        ComponentAccessor.getMailQueue().addItem(item);
    }

    private boolean dateIsOlderThanTwoWeeks(Date date) {
        DateTime twoWeeksAgo = new DateTime().minusDays(14);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoWeeksAgo) < 0);
    }
}
