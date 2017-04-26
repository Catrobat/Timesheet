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
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;

import javax.ws.rs.core.Response;
import java.util.List;

@Transactional
public interface ConfigService {

    Config getConfiguration();

    Team addTeam(String teamName, List<String> coordinatorGroups, List<String> developerGroups, List<String> teamCategoryNames);

    Response editTeamName(String oldTeamName, String newTeamName);

    Config editCategoryName(String oldCategoryName, String newCategoryName);

    Config editMail(String mailFromName, String mailFrom, String mailSubjectTime,
            String mailSubjectInactive, String mailSubjectOffline, String mailSubjectActive, String mailSubjectEntry,
            String mailBodyTime, String mailBodyInactive, String mailBodyOffline, String mailBodyActive, String mailBodyEntry);

    Config editReadOnlyUsers(String readOnlyUsers);

    int[] getCategoryIDsForTeam(String teamName);

    List<String> getCategoryNamesForTeam(String teamName);

    boolean isTimesheetAdmin(String userKey);

    TimesheetAdmin addTimesheetAdmin(ApplicationUser user);

    void editTimesheetAdmins(List<String> timesheetAdmins);

    void clearTimesheetAdmins();

    Config removeTimesheetAdmin(String TimesheetAdminKey);

    void editPairProgrammingGroup(String pairProgrammingGroup);
}
