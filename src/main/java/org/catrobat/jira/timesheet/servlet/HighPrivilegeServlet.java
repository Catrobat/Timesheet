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

import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class HighPrivilegeServlet extends HttpServlet {

    protected static final String JIRA_LOGIN_JSP = "/jira/login.jsp";
    protected final LoginUriProvider loginUriProvider;
    protected final WebSudoManager webSudoManager;
    protected final PermissionService permissionService;
    private final ConfigService configService;
    private static final Logger logger = LoggerFactory.getLogger(HighPrivilegeServlet.class);

    public HighPrivilegeServlet(final LoginUriProvider loginUriProvider, final WebSudoManager webSudoManager,
            final PermissionService permissionService, ConfigService configService) {
        this.loginUriProvider = checkNotNull(loginUriProvider, "loginProvider");
        this.webSudoManager = checkNotNull(webSudoManager, "webSudoManager");
        this.permissionService = checkNotNull(permissionService, "permissionService");
        this.configService = checkNotNull(configService, "configService");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        checkPermission(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        checkPermission(request, response);
    }

    private void checkPermission(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");

        TimesheetAdmin[] timesheetAdmins = configService.getConfiguration().getTimesheetAdminUsers();
        ApplicationUser user;
        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            response.sendRedirect(JIRA_LOGIN_JSP);
            return;
        }

        if (timesheetAdmins.length > 0) {
            for (TimesheetAdmin timesheetAdmin : timesheetAdmins) {
                if (timesheetAdmin.getUserKey().equals(user.getKey())) {
                    logger.info("User: {} is approved to access!", timesheetAdmin.getUserName());
                    enforceWebSudoProtection(request, response);
                    return;
                }
            }
        } else if (permissionService.isJiraAdministrator(user)) {
            enforceWebSudoProtection(request, response);
            return;
        }
        response.sendRedirect(JIRA_LOGIN_JSP);
    }

    private void enforceWebSudoProtection(HttpServletRequest request, HttpServletResponse response) {
        if (!webSudoManager.canExecuteRequest(request)) {
            webSudoManager.enforceWebSudoProtection(request, response);
        }
    }
}