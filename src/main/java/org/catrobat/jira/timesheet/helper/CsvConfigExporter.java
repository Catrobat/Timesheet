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

package org.catrobat.jira.timesheet.helper;

import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.ConfigService;

public class CsvConfigExporter {

    private final ConfigService configService;

    public CsvConfigExporter(ConfigService configService) {
        this.configService = configService;
    }

    public String getConfigCsvData(Config config) {
        StringBuilder sb = new StringBuilder();

        //Supervisors
        sb.append("ReadOnlyUsers" + CsvConstants.DELIMITER);
        if (!config.getReadOnlyUsers().isEmpty())
            for(String userName : config.getReadOnlyUsers().split(","))
                sb.append(userName).append(CsvConstants.NEW_LINE);

        //Timesheet Admins
        sb.append("Timesheet Admins and Groups" + CsvConstants.DELIMITER);
        for (TimesheetAdmin timesheetAdmin : config.getTimesheetAdminUsers()) {
            sb.append(timesheetAdmin.getUserName()).append(CsvConstants.DELIMITER);
        }
        for (TSAdminGroup timesheetAdminGroup : config.getTimesheetAdminGroups()) {
            sb.append(timesheetAdminGroup.getGroupName()).append(CsvConstants.DELIMITER);
        }
        sb.append(CsvConstants.NEW_LINE);

        //Email Notifications
        sb.append("Email From Name" + CsvConstants.DELIMITER);
        if (!config.getMailFromName().isEmpty())
            sb.append(config.getMailFromName()).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);
        sb.append("Email From Mail-Address" + CsvConstants.DELIMITER);

        if (!config.getMailFrom().isEmpty())
            sb.append(config.getMailFrom()).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        //Email Out Of Time
        sb.append("Email Out Of Time Subject" + CsvConstants.DELIMITER);
        if (!config.getMailSubjectTime().isEmpty())
            sb.append(unescape(config.getMailSubjectTime())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        sb.append("Email Out Of Time Body" + CsvConstants.DELIMITER);
        if (!config.getMailBodyTime().isEmpty())
            sb.append(unescape(config.getMailBodyTime())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        //Email Inactive
        sb.append("Email Inactive Subject" + CsvConstants.DELIMITER);
        if (!config.getMailSubjectInactiveState().isEmpty())
            sb.append(unescape(config.getMailSubjectInactiveState())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        sb.append("Email Inactive Body" + CsvConstants.DELIMITER);
        if (!config.getMailBodyInactiveState().isEmpty())
            sb.append(unescape(config.getMailBodyInactiveState())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        //Email Offline
        sb.append("Email Offline Subject" + CsvConstants.DELIMITER);
        if (!config.getMailSubjectOfflineState().isEmpty())
            sb.append(unescape(config.getMailSubjectOfflineState())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        sb.append("Email Offline Body" + CsvConstants.DELIMITER);
        if (!config.getMailBodyOfflineState().isEmpty())
            sb.append(unescape(config.getMailBodyOfflineState())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        //Email Active
        sb.append("Email Active Subject" + CsvConstants.DELIMITER);
        if (!config.getMailSubjectActiveState().isEmpty())
            sb.append(unescape(config.getMailSubjectActiveState())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        sb.append("Email Active Body" + CsvConstants.DELIMITER);
        if (!config.getMailBodyActiveState().isEmpty())
            sb.append(unescape(config.getMailBodyActiveState())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        //Email Admin
        sb.append("Email Admin Changed Entry Subject" + CsvConstants.DELIMITER);
        if (!config.getMailSubjectEntry().isEmpty())
            sb.append(unescape(config.getMailSubjectEntry())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        sb.append("Email Admin Changed Entry Body" + CsvConstants.DELIMITER);
        if (!config.getMailBodyEntry().isEmpty())
            sb.append(unescape(config.getMailBodyEntry())).append(CsvConstants.NEW_LINE);
        else
            sb.append(CsvConstants.NEW_LINE);

        //Teams
        for (Team team : config.getTeams()) {
            sb.append(CsvConstants.NEW_LINE);

            //Append Coordinoators
            sb.append("Assigned Coordinators" + CsvConstants.DELIMITER);
            for (String userName : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.COORDINATOR)) {
                if (!unescape(userName).isEmpty())
                    sb.append(unescape(userName)).append(CsvConstants.DELIMITER);
                else
                    sb.append(CsvConstants.DELIMITER);
            }
            sb.append(CsvConstants.NEW_LINE);

            //Append Users
            sb.append("Assigned Users" + CsvConstants.DELIMITER);
            for (String userName : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.DEVELOPER)) {
                if (!unescape(userName).isEmpty())
                    sb.append(unescape(userName)).append(CsvConstants.DELIMITER);
                else
                    sb.append(CsvConstants.DELIMITER);
            }
            sb.append(CsvConstants.NEW_LINE);

            //Append Categories
            sb.append("Assigned Categories" + CsvConstants.DELIMITER);
            for (String categoryName : configService.getCategoryNamesForTeam(team.getTeamName())) {
                if (!unescape(categoryName).isEmpty())
                    sb.append(unescape(categoryName)).append(CsvConstants.DELIMITER);
                else
                    sb.append(CsvConstants.DELIMITER);
            }
            sb.append(CsvConstants.NEW_LINE);

            //Append Teamname
            sb.append("Team Name" + CsvConstants.DELIMITER);
            if (!unescape(team.getTeamName()).isEmpty())
                sb.append(unescape(team.getTeamName())).append(CsvConstants.DELIMITER);
            else
                sb.append(CsvConstants.DELIMITER + CsvConstants.NEW_LINE);
        }
        return sb.toString();
    }

    private String unescape(String escapedHtml4String) {
        return escapedHtml4String.replace(';', ' ');
    }
}
