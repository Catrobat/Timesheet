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

package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import org.springframework.stereotype.Component;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.XlsxExportService;

import java.util.List;

@Component
public class XlsxExportServiceImpl implements XlsxExportService {

    public static final String TIMESHEET_WORKBOOK_NAME = "Timesheets";
    public static final String TIMESHEET_ENTRIES_WORKBOOK_NAME = "Timesheets entries";
    public static final String INVALID_USER_NAME = "User does not exist anymore";

    public XlsxExportServiceImpl() {
    }

    @Override
    public Workbook exportTimesheets(List<Timesheet> timesheetList) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        generateTimesheetWorksheet(workbook, timesheetList);
        generateTimesheetEntryWorksheet(workbook, timesheetList);
        return workbook;
    }

    private XSSFSheet generateTimesheetWorksheet(XSSFWorkbook workbook, List<Timesheet> timesheetList) {
        XSSFSheet worksheet = workbook.createSheet(TIMESHEET_WORKBOOK_NAME);

        String[] header = {"UserKey","Name","Hours Done","Subtracted Hours","Total Hours","Remaining Hours","Penalty Text","Lecture"};
        int rownum = 0;
        int column = 0;
        Row headerRow = worksheet.createRow(rownum);
        for (String headerColumn : header) {
            Cell cell = headerRow.createCell(column++);
            cell.setCellValue(headerColumn);
        }
        rownum++;

        if (timesheetList.size() > 0) {
            for (Timesheet timesheet : timesheetList) {
                String userKey = timesheet.getUserKey();
                ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
                String name = user == null ? INVALID_USER_NAME : user.getDisplayName();

                Row dataRow = worksheet.createRow(rownum);
                Cell cellUserName = dataRow.createCell(0);
                cellUserName.setCellValue(userKey);

                Cell cellName = dataRow.createCell(1);
                cellName.setCellValue(name);

                Cell cellHoursDone = dataRow.createCell(2);
                cellHoursDone.setCellValue(timesheet.getHoursCompleted());

                Cell cellSubtractedHours = dataRow.createCell(3);
                cellSubtractedHours.setCellValue(timesheet.getHoursDeducted());

                Cell cellTotalHours = dataRow.createCell(4);
                cellTotalHours.setCellValue(timesheet.getTargetHours());

                Cell cellRemaininglHours = dataRow.createCell(5);
                cellRemaininglHours.setCellValue(timesheet.getTargetHours() - timesheet.getHoursCompleted());

                Cell cellPenaltyText = dataRow.createCell(6);
                cellPenaltyText.setCellValue(timesheet.getReason());

                Cell cellLecture = dataRow.createCell(7);
                cellLecture.setCellValue(timesheet.getLectures());

                rownum++;
            }
        }

        return worksheet;
    }

    private XSSFSheet generateTimesheetEntryWorksheet(XSSFWorkbook workbook, List<Timesheet> timesheetList) {
        XSSFSheet worksheet = workbook.createSheet(TIMESHEET_ENTRIES_WORKBOOK_NAME);

        String[] header = {"Inactive Date","Date","Begin","End","Duration Minutes","Break Minutes","Category","Description","Team","UserKey","Name"};
        int rownum = 0;
        int column = 0;
        Row headerRow = worksheet.createRow(rownum);
        for (String headerColumn : header) {
            Cell cell = headerRow.createCell(column++);
            cell.setCellValue(headerColumn);
        }
        rownum++;

        if (timesheetList.size() > 0) {
            for (Timesheet timesheet : timesheetList) {
                for (TimesheetEntry timesheetEntry : timesheet.getEntries()) {
                    String userKey = timesheetEntry.getTimeSheet().getUserKey();
                    ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
                    String name = user == null ? INVALID_USER_NAME : user.getDisplayName();

                    Row dataRow = worksheet.createRow(rownum);
                    Cell cellInactiveDate = dataRow.createCell(0);
                    cellInactiveDate.setCellValue(timesheetEntry.getInactiveEndDate().toString());

                    Cell cellDate = dataRow.createCell(1);
                    cellDate.setCellValue(timesheetEntry.getBeginDate().toString());

                    Cell cellBegin = dataRow.createCell(2);
                    cellBegin.setCellValue(timesheetEntry.getBeginDate().toString());

                    Cell cellEnd = dataRow.createCell(3);
                    cellEnd.setCellValue(timesheetEntry.getEndDate().toString());

                    Cell cellDurationMinutes = dataRow.createCell(4);
                    cellDurationMinutes.setCellValue(timesheetEntry.getDurationMinutes());

                    Cell cellPauseMinutes = dataRow.createCell(5);
                    cellPauseMinutes.setCellValue(timesheetEntry.getPauseMinutes());

                    Cell cellCategory = dataRow.createCell(6);
                    cellCategory.setCellValue(timesheetEntry.getCategory().getName());

                    Cell cellDescription = dataRow.createCell(7);
                    cellDescription.setCellValue(timesheetEntry.getDescription());

                    Cell cellTeam = dataRow.createCell(8);
                    cellTeam.setCellValue(timesheetEntry.getTeam().getTeamName());

                    Cell cellUserKey = dataRow.createCell(9);
                    cellUserKey.setCellValue(userKey);

                    Cell cellName = dataRow.createCell(10);
                    cellName.setCellValue(name);

                    rownum++;
                }
            }
        }

        return worksheet;
    }
}
