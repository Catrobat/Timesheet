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

import com.atlassian.jira.service.ServiceException;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.helper.CsvTimesheetExporterSingle;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

public class ExportMasterThesisTimesheetAsCSVServlet extends HelperServlet {

    private final TimesheetService timesheetService;
    private final UserManager userManager;
    private final ConfigService configService;

    public ExportMasterThesisTimesheetAsCSVServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                                   TimesheetService timesheetService, ConfigService configService,
                                                   PermissionService permissionService, UserManager userManager) {
        super(loginUriProvider, webSudoManager, permissionService);
        this.timesheetService = timesheetService;
        this.userManager = userManager;
        this.configService = configService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        Date actualDate =  new Date();
        String filename = "attachment; filename=\"" +
                actualDate.toString().substring(0,10) +
                "-" +
                actualDate.toString().substring(29,34) +
                "-" +
                userManager.getRemoteUser(request).getUsername() +
                "_TimePunch_MT_Timesheet.csv\"";

        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", filename);

        Timesheet timesheet = null;
        try {
            timesheet = timesheetService.getTimesheetByUser(userManager.getRemoteUser(request).getUserKey().getStringValue(), true);
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        CsvTimesheetExporterSingle csvTimesheetExporterSingle = new CsvTimesheetExporterSingle(configService);
        PrintStream printStream = new PrintStream(response.getOutputStream(), false, "UTF-8");
        printStream.print(csvTimesheetExporterSingle.getTimesheetCsvData(timesheet));
        printStream.flush();
        printStream.close();
    }
}