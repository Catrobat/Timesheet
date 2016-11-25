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
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.joda.time.DateTime;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

public class PermissionServiceImpl implements PermissionService {

    private final TeamService teamService;
    private final ConfigService configService;

    public PermissionServiceImpl(TeamService teamService, ConfigService configService) {
        this.teamService = teamService;
        this.configService = configService;
    }

    public ApplicationUser checkIfUserExists() throws PermissionException {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (user == null) {
            throw new PermissionException("User does not exist.");
        }
        return user;
    }

    public boolean checkIfUserIsGroupMember(String groupName) {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        return ComponentAccessor.getGroupManager().isUserInGroup(user, groupName);
    }

    public boolean isUserTeamCoordinator(ApplicationUser user) {
        return teamService.getTeamsOfCoordinator(user.getUsername()).isEmpty();
    }

    public Response checkGlobalPermission() {
        try {
            ApplicationUser user = checkIfUserExists();
            if (!(checkIfUserIsGroupMember("jira-administrators")
                    || checkIfUserIsGroupMember("Jira-Test-Administrators")
                    || checkIfUserIsGroupMember("Timesheet"))
                    || isReadOnlyUser(user)) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("'User' is not assigned to " +
                        "'jira-administrators', or 'Timesheet' group.").build();
            }
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }
        return null;
    }

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

        return Response.status(Response.Status.UNAUTHORIZED).entity("Sorry, you are not a timesheet admin!").build();
    }

    public boolean isTimesheetAdmin(ApplicationUser user) {
        Config config = configService.getConfiguration();

        if (config.getTimesheetAdminUsers().length == 0 && config.getTimesheetAdminGroups().length == 0) {
            return false;
        }

        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername());
        if (configService.isTimesheetAdmin(userKey)) {
            return true;
        }

        Collection<String> groupNameCollection = ComponentAccessor.getGroupManager().getGroupNamesForUser(user.getUsername());
        for (String groupName : groupNameCollection) {
            if (configService.isTimesheetAdminGroup(groupName)) {
                return true;
            }
        }

        return false;
    }

    public boolean isJiraAdministrator(ApplicationUser user) {
        boolean isJiraAdmin = ComponentAccessor.getGroupManager().isUserInGroup(user, "jira-administrators");
        boolean isJiraTestAdmin = ComponentAccessor.getGroupManager().isUserInGroup(user, "Jira-Test-Administrators");
        return (isJiraAdmin || isJiraTestAdmin);
    }

    public boolean isReadOnlyUser(ApplicationUser user) {
        String[] readOnlyUsers = configService.getConfiguration().getReadOnlyUsers().split(",");
        for (String readOnlyUser : readOnlyUsers) {
            if (readOnlyUser.equals(user.getUsername())) {
                return true;
            }
        }
        return false;
    }

    private boolean userOwnsSheet(ApplicationUser user, Timesheet sheet) {
        if (sheet == null || user == null) {
            return false;
        }

        String sheetKey = sheet.getUserKey();
        String userKey = user.getKey();

        return sheetKey.equals(userKey);
    }

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

    public boolean isUserCoordinatorOfTeam(ApplicationUser user, Team team) {
        Set<Team> teamsOfCoordinator = teamService.getTeamsOfCoordinator(user.getName());
        for (Team aTeam : teamsOfCoordinator) {
            if (aTeam.getID() == team.getID())
                return true;
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
    public void userCanEditTimesheetEntry(ApplicationUser user, Timesheet sheet, JsonTimesheetEntry entry) throws PermissionException {
        if (userOwnsSheet(user, sheet)) {
            if (!entry.getIsGoogleDocImport()) {
                if (dateIsOlderThanAMonth(entry.getBeginDate()) || dateIsOlderThanAMonth(entry.getEndDate())) {
                    throw new PermissionException("You can not edit an entry that is older than 30 days.");
                }
            } else {
                if (dateIsOlderThanFiveYears(entry.getBeginDate()) || dateIsOlderThanFiveYears(entry.getEndDate())) {
                    throw new PermissionException("You can not edit an imported entry that is older than 5 years.");
                }
            }
        } else if (!isTimesheetAdmin(user)) {
            throw new PermissionException("Access forbidden: Sorry, you are not a timesheet admin!");
        }
    }

    @Override
    public void userCanDeleteTimesheetEntry(ApplicationUser user, TimesheetEntry entry) throws PermissionException {

        if (userOwnsSheet(user, entry.getTimeSheet())) {
            if (!entry.getIsGoogleDocImport()) {
                if (dateIsOlderThanAMonth(entry.getBeginDate()) || dateIsOlderThanAMonth(entry.getEndDate())) {
                    throw new PermissionException("You can not delete an that is older than 30 days.");
                }
            } else {
                if (dateIsOlderThanFiveYears(entry.getBeginDate()) || dateIsOlderThanFiveYears(entry.getEndDate())) {
                    throw new PermissionException("You can not delete an imported entry that is older than 5 years.");
                }
            }
        } else if (!isTimesheetAdmin(user)) {
            throw new PermissionException("Access forbidden: Sorry, but you are not a timesheet admin!");
        }
    }

    private boolean dateIsOlderThanAMonth(Date date) {
        DateTime aMonthAgo = new DateTime().minusDays(30);
        DateTime datetime = new DateTime(date);

        return (datetime.compareTo(aMonthAgo) < 0);
    }

    private boolean dateIsOlderThanFiveYears(Date date) {
        DateTime fiveYearsAgo = new DateTime().minusYears(5);
        DateTime datetime = new DateTime(date);

        return (datetime.compareTo(fiveYearsAgo) < 0);
    }

    @Override
    public Collection<com.atlassian.crowd.embedded.api.Group> printALLUserGroups() {
        return ComponentAccessor.getGroupManager().getAllGroups();
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
}
