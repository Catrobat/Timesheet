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

package org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.helper.XlsxTimesheetExporter;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.catrobat.jira.timesheet.helper.XlsxTimesheetExporter.getTimeSheetAsWorkSheet;


public class ExportTimesheetAsXlsxServlet extends HttpServlet {

    private final TimesheetService timesheetService;
    private final PermissionService permissionService;

    public ExportTimesheetAsXlsxServlet(TimesheetService timesheetService, PermissionService permissionService) {
        this.timesheetService = timesheetService;
        this.permissionService = permissionService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        Date actualDate = new Date();
        String filename = "attachment; filename=\"" +
                actualDate.toString().substring(0, 10) +
                "-" +
                actualDate.toString().substring(25, 28) +
                "-" +
                loggedInUser.getUsername() +
                "_Timesheet.xlsx\"";

        List<Timesheet> timesheetList = timesheetService.all();

        // Exclude in pom.xml required!
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheetTimeSheet = workbook.createSheet("TimeSheets");
        XSSFSheet sheetTimeSheetEntries = workbook.createSheet("Timesheet entries");


        String id = request.getParameter("id");

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", filename);

        Timesheet timesheet = null;
        try {
            if (id == null) {
                timesheet = timesheetService.getTimesheetByUser(loggedInUser.getKey());
            } else {
                timesheet = timesheetService.getTimesheetByID(Integer.parseInt(id));
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        }


        sheetTimeSheet = getTimeSheetAsWorkSheet(sheetTimeSheet,timesheetList );


        if (!permissionService.userCanViewTimesheet(loggedInUser, timesheet)) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "You are not allowed to see the timesheet.");
        }


        workbook.write(response.getOutputStream());
        workbook.close();

    }
}