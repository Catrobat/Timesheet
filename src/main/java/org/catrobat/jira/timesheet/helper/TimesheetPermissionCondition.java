/*
 * Copyright 2014 Stephan Fellhofer
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

package org.catrobat.jira.timesheet.helper;

import com.atlassian.jira.plugin.webfragment.conditions.JiraGlobalPermissionCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;

public class TimesheetPermissionCondition extends JiraGlobalPermissionCondition {

    private final PermissionService permissionService;
    private final TeamService teamService;

    public TimesheetPermissionCondition(GlobalPermissionManager permissionManager, PermissionService permissionService, TeamService teamService) {
        super(permissionManager);
        this.permissionService = permissionService;
        this.teamService = teamService;
    }

    @Override
    public void init(Map params) {
        // needed to be overridden
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return false;
        }

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
}
