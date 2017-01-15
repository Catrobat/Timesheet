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
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.helper.CsvTimesheetExporter;
import org.catrobat.jira.timesheet.services.TimesheetService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

public class ExportMasterThesisTimesheetAsCSVServlet extends HttpServlet {

    private final TimesheetService timesheetService;

    public ExportMasterThesisTimesheetAsCSVServlet(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
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
                "_Timesheet_MT_Timesheet.csv\"";

        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", filename);

        Timesheet timesheet = null;
        try {
            timesheet = timesheetService.getTimesheetByUser(loggedInUser.getKey(), true);
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        CsvTimesheetExporter csvTimesheetExporterSingle = new CsvTimesheetExporter();
        PrintStream printStream = new PrintStream(response.getOutputStream(), false, "UTF-8");
        printStream.print(csvTimesheetExporterSingle.getTimesheetCsvData(timesheet));
        printStream.flush();
        printStream.close();
    }
}