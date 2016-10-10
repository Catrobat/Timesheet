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

package org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.helper.CsvTimesheetImporter;
import org.catrobat.jira.timesheet.services.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

public class ImportTimesheetCsvServlet extends HelperServlet {

    private final ConfigService configService;
    private final TimesheetService timesheetService;
    private final TimesheetEntryService timesheetEntryService;
    private final CategoryService categoryService;
    private final TeamService teamService;
    private final ActiveObjects activeObjects;

    public ImportTimesheetCsvServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                     ConfigService configService, TimesheetService timesheetService,
                                     TimesheetEntryService timesheetEntryService,
                                     ActiveObjects activeObjects, PermissionService permissionService, CategoryService categoryService, TeamService teamService) {
        super(loginUriProvider, webSudoManager, permissionService);
        this.configService = configService;
        this.timesheetService = timesheetService;
        this.timesheetEntryService = timesheetEntryService;
        this.activeObjects = activeObjects;
        this.categoryService = categoryService;
        this.teamService = teamService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        // Dangerous servlet - should be forbidden in production use
        /*if (!timesheetService.all().isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Importing Timesheets is not possible if timesheets exist");
            return;
        }*/

        PrintWriter writer = response.getWriter();
        writer.print("<html>" +
                "<body>" +
                "<h1>Dangerzone!</h1>" +
                "Just upload files when you know what you're doing - this upload will manipulate the database!<br />" +
                "<form action=\"timesheets\" method=\"post\"><br />" +
                "<textarea name=\"csv\" rows=\"20\" cols=\"175\" wrap=\"off\">" +
                "# lines beginning with '#' are comments and will be ignored\n" +
                "# a valid configuration file must exist before importing timesheets\n" +
                "# ensure that all teams, team members and categories are assigned correctly" +
                "</textarea><br />\n" +
                "<input type=\"checkbox\" name=\"drop\" value=\"drop\">Drop existing timesheets and entries<br /><br />\n" +
                "<input type=\"submit\" />" +
                "</form>" +
                "</body>" +
                "</html>");
        writer.flush();
        writer.close();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);

        // Dangerous servlet - should be forbidden in production use
        /*if (!timesheetService.all().isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Importing Timesheets is not possible if timesheets exist");
            return;
        }*/

        String csvString = request.getParameter("csv");

        if (request.getParameter("drop") != null && request.getParameter("drop").equals("drop")) {
            dropEntries();
        }

        CsvTimesheetImporter csvTimesheetImporter = new CsvTimesheetImporter(timesheetService, timesheetEntryService, categoryService, teamService, activeObjects);
        String errorString = null;
        try {
            errorString = csvTimesheetImporter.importCsv(csvString);
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        response.getWriter().print("Successfully executed following string:<br />" +
                "<textarea rows=\"20\" cols=\"200\" wrap=\"off\" disabled>" + csvString + "</textarea>" +
                "<br /><br />" +
                "Following errors occurred:<br />" + errorString);
    }

    private void dropEntries() {
        activeObjects.deleteWithSQL(TimesheetEntry.class, "1=?", "1");
        activeObjects.deleteWithSQL(Timesheet.class, "1=?", "1");
    }
}