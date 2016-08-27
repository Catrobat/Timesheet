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

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractPermissionCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.user.UserManager;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;

import java.util.Map;

public class TimesheetPermissionCondition extends AbstractPermissionCondition {

    private final ConfigService configurationService;
    private final GroupManager groupManager;
    private final UserManager userManager;

    public TimesheetPermissionCondition(PermissionManager permissionManager, ConfigService configurationService,
                                        UserManager userManager, GroupManager groupManager) {
        super(permissionManager);
        this.configurationService = configurationService;
        this.groupManager = groupManager;
        this.userManager = userManager;
    }


    @Override
    public void init(Map params) {
        // needed to be overridden
    }

    @Override
    public boolean shouldDisplay(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        return hasPermission(applicationUser);
    }

    public boolean hasPermission(ApplicationUser applicationUser) {
        if (applicationUser == null) {
            return false;
        } else if (ComponentAccessor.getGroupManager().isUserInGroup(applicationUser, "jira-administrators") ||
                ComponentAccessor.getGroupManager().isUserInGroup(applicationUser, "Jira-Test-Administrators") ||
                ComponentAccessor.getGroupManager().isUserInGroup(applicationUser, "Timesheet")) {
            return true;
        }

        return false;
    }
}
