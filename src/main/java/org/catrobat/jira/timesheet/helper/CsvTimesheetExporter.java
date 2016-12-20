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

import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;

import java.util.List;

public class CsvTimesheetExporter {

    public CsvTimesheetExporter() {
    }

    public String getTimesheetCsvDataAll(List<Timesheet> timesheetList) {
        String timesheetData = "";
        for (Timesheet timesheet : timesheetList) {
            timesheetData = timesheetData + getTimesheetCsvData(timesheet);
        }
        return timesheetData;
    }

    public String getTimesheetCsvData(Timesheet timesheet) {
        StringBuilder sb = new StringBuilder();

        sb.append("Username" + CsvConstants.DELIMITER +
                "Practical Hours" + CsvConstants.DELIMITER +
                "Theory Hours" + CsvConstants.DELIMITER +
                "Hours Done" + CsvConstants.DELIMITER +
                "Substracted Hours" + CsvConstants.DELIMITER +
                "Total Hours" + CsvConstants.DELIMITER +
                "Remaining Hours" + CsvConstants.DELIMITER +
                "Penalty Text" + CsvConstants.DELIMITER +
                "ECTS" + CsvConstants.DELIMITER +
                "Lecture" + CsvConstants.DELIMITER +
                "isMTSheet" + CsvConstants.NEW_LINE);

        sb.append(timesheet.getUserKey()).append(CsvConstants.DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursPractice())).append(CsvConstants.DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursTheory())).append(CsvConstants.DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursCompleted())).append(CsvConstants.DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursRemoved())).append(CsvConstants.DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHours())).append(CsvConstants.DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHours() - timesheet.getTargetHoursCompleted())).append(CsvConstants.DELIMITER);
        sb.append(timesheet.getReason()).append(CsvConstants.DELIMITER);
        sb.append(timesheet.getLectures()).append(CsvConstants.DELIMITER);
        sb.append(timesheet.getIsMasterThesisTimesheet()).append(CsvConstants.NEW_LINE);

        sb.append("Inactive Date" + CsvConstants.DELIMITER +
                "Date" + CsvConstants.DELIMITER +
                "Begin" + CsvConstants.DELIMITER +
                "End" + CsvConstants.DELIMITER +
                "Duration Minutes" + CsvConstants.DELIMITER +
                "Pause Minutes" + CsvConstants.DELIMITER +
                "Category" + CsvConstants.DELIMITER +
                "Description" + CsvConstants.DELIMITER +
                "Team" + CsvConstants.DELIMITER +
                "UserKey" + CsvConstants.NEW_LINE);

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
            sb.append(unescape(timesheetEntry.getInactiveEndDate().toString())).append(CsvConstants.DELIMITER);
            sb.append(unescape(timesheetEntry.getBeginDate().toString())).append(CsvConstants.DELIMITER);
            sb.append(unescape(timesheetEntry.getBeginDate().toString())).append(CsvConstants.DELIMITER);
            sb.append(unescape(timesheetEntry.getEndDate().toString())).append(CsvConstants.DELIMITER);
            //works with google doc import
            /*
             sb.append(unescape(timesheetEntry.getBeginDate().toString().subSequence(0, 10).toString())).append(CsvConstants.DELIMITER);
            sb.append(unescape(timesheetEntry.getBeginDate().toString().subSequence(11, 16).toString())).append(CsvConstants.DELIMITER);
            sb.append(unescape(timesheetEntry.getEndDate().toString().subSequence(11, 16).toString())).append(CsvConstants.DELIMITER);
             */
            sb.append(unescape(duration)).append(CsvConstants.DELIMITER);
            sb.append(unescape(pauseDuration)).append(CsvConstants.DELIMITER);
            sb.append(unescape(timesheetEntry.getCategory().getName())).append(CsvConstants.DELIMITER);
            sb.append(unescape(timesheetEntry.getDescription().toString())).append(CsvConstants.DELIMITER);
            sb.append(unescape(timesheetEntry.getTeam().getTeamName())).append(CsvConstants.DELIMITER);
            sb.append(unescape(timesheetEntry.getTimeSheet().getUserKey())).append(CsvConstants.NEW_LINE);
        }
        sb.append(CsvConstants.NEW_LINE);

        return sb.toString();
    }

    private String unescape(String escapedHtml4String) {
        return escapedHtml4String.replace(';', ' ');
    }
}
