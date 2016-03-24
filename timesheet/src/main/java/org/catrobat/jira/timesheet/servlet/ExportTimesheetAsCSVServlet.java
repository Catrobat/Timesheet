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

import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.helper.CsvExporterTimesheet;
import org.catrobat.jira.timesheet.services.TimesheetService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;

public class ExportTimesheetAsCSVServlet extends HelperServlet {

    private final TimesheetService timesheetService;
    private final UserManager userManager;

    public ExportTimesheetAsCSVServlet(UserManager userManager, LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                       GroupManager groupManager, ConfigService configurationService,
                                       TimesheetService timesheetService) {
        super(userManager, loginUriProvider, webSudoManager, groupManager, configurationService);
        this.timesheetService = timesheetService;
        this.userManager = userManager;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"timesheets.csv\"");

        Timesheet timesheet = null;
        try {
            timesheet = timesheetService.getTimesheetByUser(userManager.getRemoteUser(request).getUserKey().getStringValue());
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        CsvExporterTimesheet csvExporterTimesheet = new CsvExporterTimesheet(timesheet, userManager);
        PrintStream printStream = new PrintStream(response.getOutputStream(), false, "UTF-8");
        printStream.print(csvExporterTimesheet.getCsvString());
        printStream.flush();
        printStream.close();
    }
}