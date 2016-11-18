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

package org.catrobat.jira.timesheet.services;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Transactional
public interface PermissionService {

    ApplicationUser checkIfUserExists() throws PermissionException;

    boolean checkIfUserIsGroupMember(String groupName);

    boolean checkIfUserIsTeamCoordinator(HttpServletRequest request);

    Response checkGlobalPermission();

    Response checkRootPermission();

    boolean userCanViewTimesheet(ApplicationUser user, Timesheet sheet);

    void userCanEditTimesheetEntry(ApplicationUser user, Timesheet sheet, JsonTimesheetEntry entry) throws PermissionException;

    void userCanDeleteTimesheetEntry(ApplicationUser user, TimesheetEntry entry) throws PermissionException;

    boolean isTimesheetAdmin(ApplicationUser user);

    boolean isJiraAdministrator(ApplicationUser user);

    Collection<Group> printALLUserGroups();

    Collection<String> getGroupNames(HttpServletRequest request);
}
