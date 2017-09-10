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

package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.scheduler.SchedulerService;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.*;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

@Component
public class PermissionServiceImpl implements PermissionService {

    private static final boolean DEBUG_MODE = true;
    private final TeamService teamService;
    private final ConfigService configService;
    private final SchedulingService schedulingService;
    private final AllowedModUsersService allowedModUsersService;
    private final String BASE_URL = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
    private boolean GOOGLE_DOCS_IMPORT_ENABLED = false; // Patch2: set to false!

    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(PermissionService.class);

    public PermissionServiceImpl(TeamService teamService, ConfigService configService, SchedulingService schedulerService,
                                 AllowedModUsersService allowedModUsersService) {
        this.teamService = teamService;
        this.configService = configService;
        this.schedulingService = schedulerService;
        this.allowedModUsersService = allowedModUsersService;
    }

    @Override
    public boolean toggleGoogleDocsImport() {
        GOOGLE_DOCS_IMPORT_ENABLED = !GOOGLE_DOCS_IMPORT_ENABLED;
        return GOOGLE_DOCS_IMPORT_ENABLED;
    }

    @Override
    public boolean isGoogleDocsImportEnabled() {
        return GOOGLE_DOCS_IMPORT_ENABLED;
    }

    @Override
    public ApplicationUser getLoggedInUser() {
        return ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    }

    @Override
    public ApplicationUser checkIfUserExists() throws PermissionException {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (user == null) {
            throw new PermissionException("User does not exist.");
        }
        return user;
    }

    @Override
    public boolean checkIfUserIsGroupMember(String groupName) {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        return ComponentAccessor.getGroupManager().isUserInGroup(user, groupName);
    }

    @Override
    public boolean isUserTeamCoordinator(ApplicationUser user) {
        return !teamService.getTeamsOfCoordinator(user.getUsername()).isEmpty();
    }

    private boolean isUserAssignedToTeam(ApplicationUser user){
        String userName = user.getUsername();
        Set<Team> teams = teamService.getTeamsOfUser(userName);
        if (teams.isEmpty()) {
            return false;
        }

        for (Team team : teams) {
            Category[] categories = team.getCategories();
            if (categories == null || categories.length == 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Response checkUserPermission() {
        ApplicationUser user;
        try {
            user = checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        if (BASE_URL.contains("test")) {
            if (!((!timesheetAdminExists() && checkIfUserIsGroupMember(JIRA_TEST_ADMINISTRATORS))
                    || checkIfUserIsGroupMember("Timesheet")
                    || isReadOnlyUser(user)
                    || isTimesheetAdmin(user)
                    || isUserAssignedToTeam(user))) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("'User' is not assigned to " +
                        "'Jira-Test-Administrators', 'Timesheet' or 'read only users'").build();
            }
        } else {
            if (!((!timesheetAdminExists() && checkIfUserIsGroupMember(JIRA_ADMINISTRATORS))
                    || checkIfUserIsGroupMember("Timesheet")
                    || isReadOnlyUser(user)
                    || isTimesheetAdmin(user)
                    || isUserAssignedToTeam(user))) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("'User' is not assigned to " +
                        "'jira-administrators', 'Timesheet' or 'read only users'").build();
            }
        }
        return null;
    }

    @Override
    public Response checkRootPermission() {
        TimesheetAdmin[] timesheetAdmins = configService.getConfiguration().getTimesheetAdminUsers();
        ApplicationUser user;

        try {
            user = checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        for (TimesheetAdmin timesheetAdmin : timesheetAdmins) {
            if (timesheetAdmin.getUserKey().equals(user.getKey())) {
                return null;
            }
        }

        if (!timesheetAdminExists() && isJiraAdministrator(user)) {
            return null;
        }

        return Response.status(Response.Status.UNAUTHORIZED).entity("Sorry, you are not a timesheet admin!").build();
    }

    @Override
    public boolean isTimesheetAdmin(ApplicationUser user) {
        Config config = configService.getConfiguration();

        if (config.getTimesheetAdminUsers().length == 0) {
            return false;
        }

        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername());
        if (configService.isTimesheetAdmin(userKey)) {
            return true;
        }

        // TODO: insert code below if groups get saved as well
//        Collection<String> groupNameCollection = ComponentAccessor.getGroupManager().getGroupNamesForUser(user.getUsername());
//        for (String groupName : groupNameCollection) {
//            if (configService.isTimesheetAdmin(groupName)) {
//                return true;
//            }
//        }

        return false;
    }

    @Override
    public boolean isJiraAdministrator(ApplicationUser user) {
        boolean isJiraAdmin = false;
        boolean isJiraTestAdmin = false;
        if (BASE_URL.contains("test")) {
            isJiraTestAdmin = ComponentAccessor.getGroupManager().isUserInGroup(user, JIRA_TEST_ADMINISTRATORS);
        } else {
            isJiraAdmin = ComponentAccessor.getGroupManager().isUserInGroup(user, JIRA_ADMINISTRATORS);
        }

        if (DEBUG_MODE) {
            isJiraTestAdmin = ComponentAccessor.getGroupManager().isUserInGroup(user, JIRA_TEST_ADMINISTRATORS);
            isJiraAdmin = ComponentAccessor.getGroupManager().isUserInGroup(user, JIRA_ADMINISTRATORS);
        }
        return (isJiraAdmin || isJiraTestAdmin);
    }

    @Override
    public boolean isReadOnlyUser(ApplicationUser user) {
        if (configService.getConfiguration().getReadOnlyUsers() == null) {
            return false;
        }
        String[] readOnlyUsers = configService.getConfiguration().getReadOnlyUsers().split(",");
        for (String readOnlyUser : readOnlyUsers) {
            if (readOnlyUser.equals(user.getUsername())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean timesheetAdminExists() {
        Config config = configService.getConfiguration();
        TimesheetAdmin[] timesheetAdmins = config.getTimesheetAdminUsers();

        return timesheetAdmins.length > 0;
    }

    private boolean userOwnsSheet(ApplicationUser user, Timesheet sheet) {
        if (sheet == null || user == null) {
            return false;
        }

        String sheetKey = sheet.getUserKey();
        String userKey = user.getKey();

        return sheetKey.equals(userKey);
    }

    @Override
    public boolean isUserCoordinatorOfTimesheet(ApplicationUser user, Timesheet sheet) {
        ApplicationUser owner = ComponentAccessor.getUserManager().getUserByKey(sheet.getUserKey());

        if (owner == null) {
            return false;
        }

        Set<Team> teamsOfOwner = teamService.getTeamsOfUser(owner.getUsername());
        Set<Team> teamsOfCoordinator = teamService.getTeamsOfCoordinator(user.getUsername());

        teamsOfOwner.retainAll(teamsOfCoordinator);

        return teamsOfOwner.size() > 0;
    }

    @Override
    public boolean isUserCoordinatorOfTeam(ApplicationUser user, Team... teams) {
        Set<Team> teamsOfCoordinator = teamService.getTeamsOfCoordinator(user.getName());
        for (Team coordinatorTeam : teamsOfCoordinator) {
            for (Team team : teams) {
                if (coordinatorTeam.getID() == team.getID())
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean userCanViewTimesheet(ApplicationUser user, Timesheet sheet) {
        return user != null && sheet != null &&
                (userOwnsSheet(user, sheet)
                        || isUserCoordinatorOfTimesheet(user, sheet)
                        || isTimesheetAdmin(user)
                        || isReadOnlyUser(user));
    }

    @Override
    public boolean userCanEditTimesheet(ApplicationUser user, Timesheet sheet) {
        return user != null && sheet != null &&
                (userOwnsSheet(user, sheet)
                        || isTimesheetAdmin(user));
    }

    @Override
    public void userCanAddTimesheetEntry(ApplicationUser user, Timesheet sheet, Date beginDate, boolean isGoogleDocsImport) throws PermissionException {
        if (isTimesheetAdmin(user)) {
            return;
        }
        if (isGoogleDocsImport && !GOOGLE_DOCS_IMPORT_ENABLED) {
            throw new PermissionException("GoogleDocsImport has been Disabled. " +
                    "Talk to a Timesheet Admin if you still need to import a timesheet.");
        }
        //checkTimesheetAccess("add", user, sheet, beginDate, isGoogleDocsImport);
    }

    @Override
    public void userCanEditTimesheetEntry(ApplicationUser user, TimesheetEntry entry) throws PermissionException {
        if (isTimesheetAdmin(user)) {
            return;
        }
        Timesheet sheet = entry.getTimeSheet();
        Date beginDate = entry.getBeginDate();
        boolean isGoogleDocsImport = entry.getIsGoogleDocImport();
        checkTimesheetAccess("edit", user, sheet, beginDate, isGoogleDocsImport);
    }

    @Override
    public void userCanDeleteTimesheetEntry(ApplicationUser user, TimesheetEntry entry) throws PermissionException {
        if (isTimesheetAdmin(user)) {
            return;
        }
        Timesheet sheet = entry.getTimeSheet();
        Date beginDate = entry.getBeginDate();
        boolean isGoogleDocsImport = entry.getIsGoogleDocImport();
        checkTimesheetAccess("delete", user, sheet, beginDate, isGoogleDocsImport);
    }

    private void checkTimesheetAccess(String method, ApplicationUser user, Timesheet sheet, Date beginDate, boolean isGoogleDocsImport) throws PermissionException {
        if (userOwnsSheet(user, sheet)) {
            if(allowedModUsersService.checkIfUserIsInList(user.getKey())){
                LOGGER.error("User is in Unlimited Mod List so this action is allowed!");
                return;
            }
            if (dateIsOlderThanMaxModTime(beginDate)) {
                int tolerant_days = schedulingService.getMaxModificationDays();

                throw new PermissionException("You can not " + method + " an entry that is older than " +
                        tolerant_days + " days.");
            } else {
                if (dateIsOlderThanFiveYears(beginDate)) {
                    throw new PermissionException("You can not " + method + " an imported entry that is older than 5 years.");
                }
            }
        } else {
            throw new PermissionException("Access forbidden: Sorry, you are not a timesheet admin!");
        }
    }

    private boolean dateIsOlderThanMaxModTime(Date date) {
        Instant instant = date.toInstant();
        ZonedDateTime dataTime = instant.atZone(ZoneId.systemDefault());

        int tolerant_days = schedulingService.getMaxModificationDays();

        ZonedDateTime latest_possible_date = ZonedDateTime.now().minusDays(tolerant_days);

        return dataTime.isBefore(latest_possible_date);
    }

    private boolean dateIsOlderThanFiveYears(Date date) {
        Instant instant = date.toInstant();
        ZonedDateTime dataTime = instant.atZone(ZoneId.systemDefault());
        ZonedDateTime fiveYearsAgo = ZonedDateTime.now().minusYears(5);

        return dataTime.isBefore(fiveYearsAgo);
    }

    @Override
    public Collection<String> getGroupNames(HttpServletRequest request) {
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (loggedInUser == null) {
            return null;
        }

        String username = loggedInUser.getUsername();
        return ComponentAccessor.getGroupManager().getGroupNamesForUser(username);
    }

    @Override
    public boolean isUserEligibleForTimesheet(ApplicationUser user) {
/*        Response response = checkUserPermission();
        if (response != null) {
            return false;
        }*/

        return isUserAssignedToTeam(user);
    }
}
