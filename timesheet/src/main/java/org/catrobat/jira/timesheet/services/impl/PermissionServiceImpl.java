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
        UserProfile userProfile = userManager.getUserProfile(userManager.getRemoteUser(request).getUsername());

        if (userProfile == null) {
            throw new ServiceException("User does not exist.");
        }
        return userProfile;
    }

    public boolean checkIfUserIsGroupMember(HttpServletRequest request, String groupName) {
        String username = userManager.getRemoteUser(request).getUsername();
        UserProfile userProfile = userManager.getUserProfile(username);

        if (userProfile == null) {
            return false;
        }

        String userKey = ComponentAccessor.
                getUserKeyService().getKeyForUsername(userProfile.getUsername());
        Collection<String> userGroups = ComponentAccessor.getGroupManager().getGroupNamesForUser(
                ComponentAccessor.getUserManager().getUserByKey(userKey));
        if (userGroups.contains(groupName)) {
            return true;
        }

        return false;
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

        if (userProfile == null) {
            return false;
        }

        return true;
    }

    public Response checkPermission(HttpServletRequest request) {
        UserProfile userProfile = userManager.getRemoteUser(request);

        if (userProfile == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(userProfile.getUsername());

        if (userKey == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } else if (!(checkIfUserIsGroupMember(request, "jira-administrators") || checkIfUserIsGroupMember(request, "Timesheet"))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return null;
    }

    public boolean isApproved(UserProfile userProfile) {
        Config config = configService.getConfiguration();

        if (config.getApprovedGroups().length == 0 && config.getApprovedUsers().length == 0) {
            return false;
        }

        if (configService.isUserApproved(ComponentAccessor.
                getUserKeyService().getKeyForUsername(userProfile.getUsername()))) {
            return true;
        }

        Collection<String> groupNameCollection = ComponentAccessor.getGroupManager().getGroupNamesForUser(userProfile.getUsername());
        for (String groupName : groupNameCollection) {
            if (configService.isGroupApproved(groupName)) {
                return true;
            }
        }

        return false;
    }

    public boolean isApproved(String userName) {
        return isApproved(userManager.getUserProfile(userName));
    }

    private boolean userOwnsSheet(UserProfile user, Timesheet sheet) {
        if (sheet == null || user == null) {
            return false;
        }

        String sheetKey = sheet.getUserKey();
        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername());

        return sheetKey.equals(userKey);
    }

    private boolean userIsAdmin(UserProfile userProfile) {
        return userManager.isAdmin(userProfile.getUserKey());
    }

    private boolean userCoordinatesTeamsOfSheet(UserProfile user, Timesheet sheet) {
        UserProfile owner = userManager.getUserProfile(sheet.getUserKey());
        if (owner == null)
            return false;

        Set<Team> ownerTeams = teamService.getTeamsOfUser(owner.getUsername());
        Set<Team> userTeams = teamService.getCoordinatorTeamsOfUser(user.getUsername());

        ownerTeams.retainAll(userTeams);

        return ownerTeams.size() > 0;
    }

    @Override
    public boolean userCanViewTimesheet(UserProfile user, Timesheet sheet) {
        return user != null && sheet != null &&
                (userOwnsSheet(user, sheet)
                        || userIsAdmin(user)
                        || userCoordinatesTeamsOfSheet(user, sheet));
    }

    @Override
    public void userCanEditTimesheetEntry(UserProfile user, Timesheet sheet, JsonTimesheetEntry entry) throws PermissionException {

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
        } else if (!userIsAdmin(user)) {
            throw new PermissionException("You are not Admin.");
        }
    }

    @Override
    public void userCanDeleteTimesheetEntry(UserProfile user, TimesheetEntry entry) throws PermissionException {

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
        } else if (!userIsAdmin(user)) {
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
}
