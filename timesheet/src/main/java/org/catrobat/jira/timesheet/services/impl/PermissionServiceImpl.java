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
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
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

    private final UserManager userManager;
    private final TeamService teamService;
    private final ConfigService configService;

    public PermissionServiceImpl(UserManager userManager, TeamService teamService,
            ConfigService configService) {
        this.userManager = userManager;
        this.teamService = teamService;
        this.configService = configService;
    }

    public UserProfile checkIfUserExists(HttpServletRequest request) throws ServiceException {
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        if (loggedInUser == null) {
            throw new ServiceException("loggedInUser does not exist.");
        }

        UserProfile userProfile = userManager.getUserProfile(loggedInUser.getUsername());

        if (userProfile == null) {
            throw new ServiceException("User does not exist.");
        }
        return userProfile;
    }

    public boolean checkIfUserIsGroupMember(HttpServletRequest request, String groupName) {
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        if (loggedInUser == null) {
            System.out.println("loggedInUser is null!");
            return false;
        }

        String username = loggedInUser.getUsername();
        UserProfile userProfile = userManager.getUserProfile(username);

        if (userProfile == null) {
            System.out.println("UserProfile is null!");
            return false;
        }

        String userKey = ComponentAccessor.
                getUserKeyService().getKeyForUsername(username);
        Collection<String> userGroups = ComponentAccessor.getGroupManager().getGroupNamesForUser(
                ComponentAccessor.getUserManager().getUserByKey(userKey));

        return userGroups.contains(groupName);
    }

    public boolean checkIfUserIsTeamCoordinator(HttpServletRequest request){
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        String username = loggedInUser.getUsername();
        return teamService.getCoordinatorTeamsOfUser(username).isEmpty() ? false : true;
    }

    public UserProfile checkIfUsernameExists(String userName) throws ServiceException {
        UserProfile userProfile = userManager.getUserProfile(userName);

        if (userProfile == null) {
            throw new ServiceException("User does not exist.");
        }
        return userProfile;
    }

    public boolean checkIfUserExists(String userName) {
        UserProfile userProfile = userManager.getUserProfile(userName);

        return userProfile != null;
    }

    public Response checkPermission(HttpServletRequest request) {
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        UserProfile userProfile = userManager.getUserProfile(loggedInUser.getUsername());

        if (userProfile == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("'User' does not have a valid profil.").build();
        }

        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(userProfile.getUsername());

        if (userKey == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("'User' does not have a valid " +
                    "'UserKey'.").build();
        } else if (!(checkIfUserIsGroupMember(request, "jira-administrators") ||
                checkIfUserIsGroupMember(request, "Jira-Test-Administrators"))) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("'User' is not assigned to " +
                    "'jira-administrators', or 'Timesheet' group.").build();
        }

        return null;
    }

    public boolean isApproved(ApplicationUser user) {
        Config config = configService.getConfiguration();

        if (config.getApprovedUsers().length == 0 && config.getApprovedGroups().length == 0) {
            return false;
        }

        if (configService.isUserApproved(ComponentAccessor.
                getUserKeyService().getKeyForUsername(user.getUsername()))) {
            return true;
        }

        Collection<String> groupNameCollection = ComponentAccessor.getGroupManager().getGroupNamesForUser(user.getUsername());
        for (String groupName : groupNameCollection) {
            if (configService.isGroupApproved(groupName)) {
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
        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername());

        return sheetKey.equals(userKey);
    }

    private boolean isUserAdmin(ApplicationUser user) {
        return userManager.isAdmin(user.getKey());
    }

    private boolean userCoordinatesTeamsOfSheet(ApplicationUser user, Timesheet sheet) {
        UserProfile owner = userManager.getUserProfile(sheet.getUserKey());
        if (owner == null) { return false; }

        Set<Team> ownerTeams = teamService.getTeamsOfUser(owner.getUsername());
        Set<Team> userTeams = teamService.getCoordinatorTeamsOfUser(user.getUsername());

        ownerTeams.retainAll(userTeams);

        return ownerTeams.size() > 0;
    }

    @Override
    public boolean userCanViewTimesheet(ApplicationUser user, Timesheet sheet) {
        return user != null && sheet != null &&
                (userOwnsSheet(user, sheet)
                        || isUserAdmin(user)
                        || userCoordinatesTeamsOfSheet(user, sheet)
                        || isApproved(user));
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
        } else if (!isUserAdmin(user)) {
            throw new PermissionException("You are not Admin.");
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
        } else if (!isUserAdmin(user)) {
            throw new PermissionException("You are not Admin.");
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
            System.out.println("loggedInUser is null!");
            return null;
        }

        String username = loggedInUser.getUsername();
        return ComponentAccessor.getGroupManager().getGroupNamesForUser(username);
    }
}
