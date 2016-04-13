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

import java.util.List;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

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

        //Approved Users
        sb.append("Approved Users and Groups" + DELIMITER);
        for (ApprovedUser approvedUser : config.getApprovedUsers()) {
            sb.append(approvedUser.getUserName()).append(DELIMITER);
        }
        for (ApprovedGroup approvedGroup : config.getApprovedGroups()) {
            sb.append(approvedGroup.getGroupName()).append(DELIMITER);
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
        if (!config.getMailSubjectInactive().isEmpty())
            sb.append(unescape(config.getMailSubjectInactive())).append(NEW_LINE);
        else
            sb.append(DEFAULT_VALUE + NEW_LINE);

        sb.append("Email Inactive Body" + DELIMITER);
        if (!config.getMailBodyInactive().isEmpty())
            sb.append(unescape(config.getMailBodyInactive())).append(NEW_LINE);
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
                "Lecture" + NEW_LINE);

        sb.append(timesheet.getUserKey()).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursPractice())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursTheory())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursCompleted())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursRemoved())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHours())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHours() - timesheet.getTargetHoursCompleted())).append(DELIMITER);
        sb.append(timesheet.getReason()).append(DELIMITER);
        sb.append(Double.toString(timesheet.getEcts())).append(DELIMITER);
        sb.append(timesheet.getLectures()).append(NEW_LINE);

        sb.append("Date" + DELIMITER +
                "Begin" + DELIMITER +
                "End" + DELIMITER +
                "Pause Minutes" + DELIMITER +
                "Duration Minutes" + DELIMITER +
                "Team" + DELIMITER +
                "Category" + DELIMITER +
                "Description" + NEW_LINE);

        for (TimesheetEntry timesheetEntry : timesheet.getEntries()) {
            Integer hours = 0;
            Integer minutes = timesheetEntry.getDurationMinutes();

            while (minutes - 60 >= 0) {
                minutes = minutes - 60;
                hours++;
            }
            String duration = hours + ":" + minutes;

            sb.append(unescape(timesheetEntry.getBeginDate().toString().subSequence(0, 10).toString())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getBeginDate().toString().subSequence(12, 17).toString())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getEndDate().toString().subSequence(12, 17).toString())).append(DELIMITER);
            sb.append(unescape(Integer.toString(timesheetEntry.getPauseMinutes()))).append(DELIMITER);
            sb.append(unescape(duration)).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getTeam().getTeamName())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getCategory().getName())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getDescription().toString())).append(NEW_LINE);
        }
        sb.append(NEW_LINE);

        return sb.toString();
    }

    private String unescape(String escapedHtml4String) {
        if (escapedHtml4String == null || escapedHtml4String.trim().length() == 0) {
            return "\"\"";
        } else return "\"" + unescapeHtml4(escapedHtml4String).replaceAll("\"", "\"\"") + "\"";
    }
}
