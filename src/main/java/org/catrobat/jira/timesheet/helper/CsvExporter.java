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

import java.util.List;

public abstract class CsvExporter {

    public static final String DELIMITER = ";";
    public static final String NEW_LINE = "\n";
    private static final String DEFAULT_VALUE = "Not Available";

    private final ConfigService configService;

    public CsvExporter(ConfigService configService) {
        this.configService = configService;
    }

    public String getTimesheetCsvData(Timesheet timesheet) {
        return fetchTimesheetData(timesheet);
    }

    public String getTimesheetCsvDataAll(List<Timesheet> timesheetList) {
        return fetchTimesheetDataAll(timesheetList);
    }

    public String getConfigCsvData(Config config) {
        return fetchConfigData(config);
    }

    private String fetchConfigData(Config config) {
        StringBuilder sb = new StringBuilder();

        //Supervisors
        sb.append("ReadOnlyUsers" + DELIMITER);
        if (!config.getReadOnlyUsers().isEmpty())
            for(String userName : config.getReadOnlyUsers().split(","))
                sb.append(userName).append(NEW_LINE);

        //Timesheet Admins
        sb.append("Timesheet Admins and Groups" + DELIMITER);
        for (TimesheetAdmin timesheetAdmin : config.getTimesheetAdminUsers()) {
            sb.append(timesheetAdmin.getUserName()).append(DELIMITER);
        }
        for (TSAdminGroup timesheetAdminGroup : config.getTimesheetAdminGroups()) {
            sb.append(timesheetAdminGroup.getGroupName()).append(DELIMITER);
        }
        sb.append(NEW_LINE);

        //Email Notifications
        sb.append("Email From Name" + DELIMITER);
        if (!config.getMailFromName().isEmpty())
            sb.append(config.getMailFromName()).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);
        sb.append("Email From Mail-Address" + DELIMITER);

        if (!config.getMailFrom().isEmpty())
            sb.append(config.getMailFrom()).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        //Email Out Of Time
        sb.append("Email Out Of Time Subject" + DELIMITER);
        if (!config.getMailSubjectTime().isEmpty())
            sb.append(unescape(config.getMailSubjectTime())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        sb.append("Email Out Of Time Body" + DELIMITER);
        if (!config.getMailBodyTime().isEmpty())
            sb.append(unescape(config.getMailBodyTime())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        //Email Inactive
        sb.append("Email Inactive Subject" + DELIMITER);
        if (!config.getMailSubjectInactiveState().isEmpty())
            sb.append(unescape(config.getMailSubjectInactiveState())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        sb.append("Email Inactive Body" + DELIMITER);
        if (!config.getMailBodyInactiveState().isEmpty())
            sb.append(unescape(config.getMailBodyInactiveState())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        //Email Offline
        sb.append("Email Offline Subject" + DELIMITER);
        if (!config.getMailSubjectOfflineState().isEmpty())
            sb.append(unescape(config.getMailSubjectOfflineState())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        sb.append("Email Offline Body" + DELIMITER);
        if (!config.getMailBodyOfflineState().isEmpty())
            sb.append(unescape(config.getMailBodyOfflineState())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        //Email Active
        sb.append("Email Active Subject" + DELIMITER);
        if (!config.getMailSubjectActiveState().isEmpty())
            sb.append(unescape(config.getMailSubjectActiveState())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        sb.append("Email Active Body" + DELIMITER);
        if (!config.getMailBodyActiveState().isEmpty())
            sb.append(unescape(config.getMailBodyActiveState())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        //Email Admin
        sb.append("Email Admin Changed Entry Subject" + DELIMITER);
        if (!config.getMailSubjectEntry().isEmpty())
            sb.append(unescape(config.getMailSubjectEntry())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        sb.append("Email Admin Changed Entry Body" + DELIMITER);
        if (!config.getMailBodyEntry().isEmpty())
            sb.append(unescape(config.getMailBodyEntry())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        //Teams
        for (Team team : config.getTeams()) {
            sb.append(NEW_LINE);

            //Append Coordinoators
            sb.append("Assigned Coordinators" + DELIMITER);
            for (String userName : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.COORDINATOR)) {
                if (!unescape(userName).isEmpty())
                    sb.append(unescape(userName)).append(DELIMITER);
                else
                    sb.append(DEFAULT_VALUE + DELIMITER);
            }
            sb.append(NEW_LINE);

            //Append Users
            sb.append("Assigned Users" + DELIMITER);
            for (String userName : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.DEVELOPER)) {
                if (!unescape(userName).isEmpty())
                    sb.append(unescape(userName)).append(DELIMITER);
                else
                    sb.append(DEFAULT_VALUE + DELIMITER);
            }
            sb.append(NEW_LINE);

            //Append Categories
            sb.append("Assigned Categories" + DELIMITER);
            for (String categoryName : configService.getCategoryNamesForTeam(team.getTeamName())) {
                if (!unescape(categoryName).isEmpty())
                    sb.append(unescape(categoryName)).append(DELIMITER);
                else
                    sb.append(DEFAULT_VALUE + DELIMITER);
            }
            sb.append(NEW_LINE);

            //Append Teamname
            sb.append("Team Name" + DELIMITER);
            if (!unescape(team.getTeamName()).isEmpty())
                sb.append(unescape(team.getTeamName())).append(DELIMITER);
            else
                sb.append(DEFAULT_VALUE + DELIMITER + NEW_LINE);
        }
        return sb.toString();
    }

    private String fetchTimesheetDataAll(List<Timesheet> timesheetList) {
        String timesheetData = "";
        for (Timesheet timesheet : timesheetList) {
            timesheetData = timesheetData + fetchTimesheetData(timesheet);
        }
        return timesheetData;
    }

    private String fetchTimesheetData(Timesheet timesheet) {
        StringBuilder sb = new StringBuilder();

        sb.append("Username" + DELIMITER +
                "Practical Hours" + DELIMITER +
                "Theory Hours" + DELIMITER +
                "Hours Done" + DELIMITER +
                "Substracted Hours" + DELIMITER +
                "Total Hours" + DELIMITER +
                "Remaining Hours" + DELIMITER +
                "Penalty Text" + DELIMITER +
                "ECTS" + DELIMITER +
                "Lecture" + DELIMITER +
                "isMTSheet" + NEW_LINE);

        sb.append(timesheet.getUserKey()).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursPractice())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursTheory())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursCompleted())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursRemoved())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHours())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHours() - timesheet.getTargetHoursCompleted())).append(DELIMITER);
        sb.append(timesheet.getReason()).append(DELIMITER);
        sb.append(Double.toString(timesheet.getEcts())).append(DELIMITER);
        sb.append(timesheet.getLectures()).append(DELIMITER);
        sb.append(timesheet.getIsMasterThesisTimesheet()).append(NEW_LINE);

        sb.append("Inactive Date" + DELIMITER +
                "Date" + DELIMITER +
                "Begin" + DELIMITER +
                "End" + DELIMITER +
                "Duration Minutes" + DELIMITER +
                "Pause Minutes" + DELIMITER +
                "Category" + DELIMITER +
                "Description" + DELIMITER +
                "Team" + DELIMITER +
                "UserKey" + NEW_LINE);

        for (TimesheetEntry timesheetEntry : timesheet.getEntries()) {
            Integer hours = 0;
            Integer minutes = timesheetEntry.getDurationMinutes();
            if(minutes < 0)
                minutes = minutes * (-1);
            while(minutes > 0) {
                if(minutes - 60 < 0)
                    break;
                minutes = minutes - 60;
                hours++;
            }
            Integer remainingMinutes = timesheetEntry.getDurationMinutes() % 60;
            if(remainingMinutes < 0)
                remainingMinutes = remainingMinutes * (-1);
            String duration = Integer.toString(hours) + ":" + Integer.toString(remainingMinutes);

            Integer pauseHours = 0;
            Integer pauseMinutes = timesheetEntry.getPauseMinutes();
            while(pauseMinutes > 0) {
                if(pauseMinutes - 60 < 0)
                    break;
                pauseMinutes = pauseMinutes - 60;
                pauseHours++;
            }
            Integer remainingPauseMinutes = timesheetEntry.getPauseMinutes() % 60;
            String pauseDuration = Integer.toString(pauseHours) + ":" + Integer.toString(remainingPauseMinutes);
            sb.append(unescape(timesheetEntry.getInactiveEndDate().toString())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getBeginDate().toString())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getBeginDate().toString())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getEndDate().toString())).append(DELIMITER);
            //works with google doc import
            /*
             sb.append(unescape(timesheetEntry.getBeginDate().toString().subSequence(0, 10).toString())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getBeginDate().toString().subSequence(11, 16).toString())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getEndDate().toString().subSequence(11, 16).toString())).append(DELIMITER);
             */
            sb.append(unescape(duration)).append(DELIMITER);
            sb.append(unescape(pauseDuration)).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getCategory().getName())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getDescription().toString())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getTeam().getTeamName())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getTimeSheet().getUserKey())).append(NEW_LINE);
        }
        sb.append(NEW_LINE);

        return sb.toString();
    }

    private String unescape(String escapedHtml4String) {
        return escapedHtml4String.replace(';', ' ');
    }
}
