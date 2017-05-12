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

    private static final String DELIMITER = ";";
    private static final String NEW_LINE = "\n";

    public CsvTimesheetExporter() {
    }

    public String getTimesheetCsvDataAll(List<Timesheet> timesheetList) {
        StringBuilder timesheetData = new StringBuilder();
        for (Timesheet timesheet : timesheetList) {
            timesheetData.append(getTimesheetCsvData(timesheet));
        }
        return timesheetData.toString();
    }

    public String getTimesheetCsvData(Timesheet timesheet) {
        StringBuilder sb = new StringBuilder();

        sb.append("Username" + DELIMITER +
                "Practical Hours" + DELIMITER +
                "Theory Hours" + DELIMITER +
                "Hours Done" + DELIMITER +
                "Subtracted Hours" + DELIMITER +
                "Total Hours" + DELIMITER +
                "Remaining Hours" + DELIMITER +
                "Penalty Text" + DELIMITER +
                "Lecture" + DELIMITER +
                "isMTSheet" + NEW_LINE);

        sb.append(timesheet.getUserKey()).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getHoursPracticeCompleted())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHoursTheory())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getHoursCompleted())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getHoursDeducted())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHours())).append(DELIMITER);
        sb.append(Integer.toString(timesheet.getTargetHours() - timesheet.getHoursCompleted())).append(DELIMITER);
        sb.append(timesheet.getReason()).append(DELIMITER);
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
            sb.append(unescape(timesheetEntry.getDescription())).append(DELIMITER);
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
