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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;

import java.util.List;

public class XlsxTimesheetExporter {


    public XlsxTimesheetExporter() {
    }


    public static XSSFSheet getTimeSheetAsWorkSheet(XSSFSheet workTimeSheet, List<Timesheet> timesheetList)    {

        String[] header = {"Username","Practical Hours","Hours Done","Subtracted Hours","Total Hours","Remaining Hours","Penalty Text","Lecture"};
        int rownum = 0;
        int column = 0;
        Row headerRow = workTimeSheet.createRow(rownum);
        for (String headerColumn : header) {
            Cell cell = headerRow.createCell(column++);
            cell.setCellValue((String) headerColumn);
        }
        rownum++;

        if (timesheetList.size() > 0) {
            for (Timesheet timesheet : timesheetList) {
                Row dataRow = workTimeSheet.createRow(rownum);
                Cell cellUserName = dataRow.createCell(0);
                cellUserName.setCellValue((String) timesheet.getUserKey());

                Cell cellPracticalHours = dataRow.createCell(1);
                cellPracticalHours.setCellValue((int) timesheet.getHoursPracticeCompleted());

                Cell cellHoursDone = dataRow.createCell(2);
                cellHoursDone.setCellValue((int) timesheet.getHoursCompleted());

                Cell cellSubtractedHours = dataRow.createCell(3);
                cellSubtractedHours.setCellValue((int) timesheet.getHoursDeducted());

                Cell cellTotalHours = dataRow.createCell(4);
                cellTotalHours.setCellValue((int) timesheet.getTargetHours());

                Cell cellRemaininglHours = dataRow.createCell(5);
                cellRemaininglHours.setCellValue((int) timesheet.getTargetHours() - timesheet.getHoursCompleted());

                Cell cellPenaltyText = dataRow.createCell(6);
                cellPenaltyText.setCellValue((String) timesheet.getReason());

                Cell cellLecture = dataRow.createCell(7);
                cellLecture.setCellValue((String) timesheet.getLectures());

                rownum++;
            }
        }

        return workTimeSheet;
    }

    public static XSSFSheet getTimeSheetEntityAsWorkSheet(XSSFSheet workTimeSheet, List<Timesheet> timesheetList)
    {
        String[] header = {"Inactive Date","Date","Begin","End","Duration Minutes","Pause Minutes","Category","Description","Team","UserKey"};
        int rownum = 0;
        int column = 0;
        Row headerRow = workTimeSheet.createRow(rownum);
        for (String headerColumn : header) {
            Cell cell = headerRow.createCell(column++);
            cell.setCellValue((String) headerColumn);
        }
        rownum++;

        if (timesheetList.size() > 0) {
            for (Timesheet timesheet : timesheetList) {
                for (TimesheetEntry timesheetEntry : timesheet.getEntries()) {
                    Row dataRow = workTimeSheet.createRow(rownum);
                    Cell cellInactiveDate = dataRow.createCell(0);
                    cellInactiveDate.setCellValue((String) timesheetEntry.getInactiveEndDate().toString());

                    Cell cellDate = dataRow.createCell(1);
                    cellDate.setCellValue((String) timesheetEntry.getBeginDate().toString());

                    Cell cellBegin = dataRow.createCell(2);
                    cellBegin.setCellValue((String) timesheetEntry.getBeginDate().toString());

                    Cell cellEnd = dataRow.createCell(3);
                    cellEnd.setCellValue((String) timesheetEntry.getEndDate().toString());

                    Cell cellDurationMinutes = dataRow.createCell(4);
                    cellDurationMinutes.setCellValue((int) timesheetEntry.getDurationMinutes());

                    Cell cellPauseMinutes = dataRow.createCell(5);
                    cellPauseMinutes.setCellValue((int) timesheetEntry.getPauseMinutes());

                    Cell cellCategory = dataRow.createCell(6);
                    cellCategory.setCellValue((String) timesheetEntry.getCategory().getName());

                    Cell cellDescription = dataRow.createCell(7);
                    cellDescription.setCellValue((String) timesheetEntry.getDescription());

                    Cell cellTeam = dataRow.createCell(8);
                    cellTeam.setCellValue((String) timesheetEntry.getTeam().getTeamName());

                    Cell cellUserKey = dataRow.createCell(9);
                    cellUserKey.setCellValue((String) timesheetEntry.getTimeSheet().getUserKey());

                    rownum++;
                }
            }
        }

        return workTimeSheet;
    }


}
