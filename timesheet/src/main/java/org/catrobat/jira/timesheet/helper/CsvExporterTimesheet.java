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

import com.atlassian.sal.api.user.UserManager;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

public class CsvExporterTimesheet {

    public static final String DELIMITER = ";";
    public static final String NEW_LINE = "\n";
    private final Timesheet timesheet;
    private final UserManager userManager;

    public CsvExporterTimesheet(final Timesheet timesheet, final UserManager userManager) {
        this.timesheet = timesheet;
        this.userManager = userManager;
    }

    public String getCsvString() {
        StringBuilder sb = new StringBuilder();

        sb.append("User Key" + DELIMITER +
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
        sb.append(Integer.toString(timesheet.getEcts())).append(DELIMITER);
        sb.append(timesheet.getLectures()).append(NEW_LINE);

        for (TimesheetEntry timesheetEntry : timesheet.getEntries()) {
            sb.append("Begin Date" + DELIMITER +
                    "End Date" + DELIMITER +
                    "Pause Minutes" + DELIMITER +
                    "Duration Minutes" + DELIMITER +
                    "Team" + DELIMITER +
                    "Category" + DELIMITER +
                    "Description" + NEW_LINE);

            Integer hours = 0;
            Integer minutes = timesheetEntry.getDurationMinutes();

            while(minutes - 60 >= 0) {
                minutes = minutes - 60;
                hours++;
            }
            String duration = hours + ":" + minutes;

            sb.append(unescape(timesheetEntry.getBeginDate().toString())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getEndDate().toString())).append(DELIMITER);
            sb.append(unescape(Integer.toString(timesheetEntry.getPauseMinutes()))).append(DELIMITER);
            sb.append(unescape(duration)).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getTeam().getTeamName())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getCategory().getName())).append(DELIMITER);
            sb.append(unescape(timesheetEntry.getDescription().toString())).append(NEW_LINE);
        }

        return sb.toString();
    }

    private String unescape(String escapedHtml4String) {
        if (escapedHtml4String == null || escapedHtml4String.trim().length() == 0) {
            return "\"\"";
        } else {
            return "\"" + unescapeHtml4(escapedHtml4String).replaceAll("\"", "\"\"") + "\"";
        }
    }
}
