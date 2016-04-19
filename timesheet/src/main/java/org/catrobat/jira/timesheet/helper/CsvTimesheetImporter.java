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

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.service.ServiceException;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class CsvTimesheetImporter {

    private final TimesheetService timesheetService;
    private final TimesheetEntryService timesheetEntryService;
    private final CategoryService categoryService;
    private final TeamService teamService;
    private final ActiveObjects ao;

    public CsvTimesheetImporter(TimesheetService timesheetService,
                                TimesheetEntryService timesheetEntryService, CategoryService categoryService, TeamService teamService, ActiveObjects ao) {

        this.timesheetService = timesheetService;
        this.timesheetEntryService = timesheetEntryService;
        this.categoryService = categoryService;
        this.teamService = teamService;
        this.ao = ao;
    }

    public String importCsv(String csvString) throws ServiceException, ParseException {
        StringBuilder errorStringBuilder = new StringBuilder("<ul>");
        int lineNumber = 0;

        //Read lines
        for (String line : csvString.split("\\r?\\n")) {
            lineNumber++;

            // skip line if empty or comment
            if (line.length() == 0 || line.charAt(0) == '#') {
                errorStringBuilder.append("<li>comment on line ")
                        .append(lineNumber)
                        .append(" (line will be ignored)</li>");
                continue;
            }

            String[] columns = line.split(CsvExporter.DELIMITER);

            if ((columns[0].equals("Username") ||
                    columns[0].equals("Date")) &&
                    columns.length > 0) {
                continue;
            }

            if (columns.length == 11) {

                timesheetService.add(
                        columns[0],                     //userkey
                        Integer.parseInt(columns[1]),   //practical
                        Integer.parseInt(columns[2]),   //theory
                        Integer.parseInt(columns[5]),   //total
                        Integer.parseInt(columns[3]),   //completed
                        Integer.parseInt(columns[4]),   //removed
                        columns[9],                     //lectures
                        columns[7],                     //admin reason
                        Double.parseDouble(columns[8]), //ects
                        "Not Available",                //latest entry date
                        true,                           //isActice
                        true,                           //isEnabled
                        false                           //isMTSheet
                );
            } else if (columns.length == 10) {
                Timesheet timesheet = timesheetService.getTimesheetImport(columns[9]);
                SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd HH:mm");

                timesheetEntryService.add(
                        timesheet,                                     //timesheet
                        sdf.parse(columns[2]),                         //begin date
                        sdf.parse(columns[3]),                         //end date
                        categoryService.getCategoryByName(columns[6]), //category
                        columns[6],                                    //description
                        Integer.parseInt(columns[5].substring(0, 2)) +
                        Integer.parseInt(columns[5].substring(3, 5)),  //pause minutes
                        teamService.getTeamByName(columns[8]),         //team
                        true,                                          //isGoogleDocImport
                        sdf.parse(columns[0]),                         //inactive date
                        "",                                            //JIRA ticket ID
                        ""                                             //pair programming partner
                );
            }
        }

        for (Timesheet timesheet : timesheetService.all()) {
            System.out.println("ts key: " + timesheet.getUserKey() + " id: " + timesheet.getID());
            for (TimesheetEntry timesheetEntry : timesheetEntryService.getEntriesBySheet(timesheet))
                System.out.println(" entry id: " + timesheetEntry.getID());
        }


        errorStringBuilder.append("</ul>");

        return errorStringBuilder.toString();
    }
}
