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

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.catrobat.jira.timesheet.services.PermissionService;

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
    protected ApplicationUser user;

    public HighPrivilegeServlet(final LoginUriProvider loginUriProvider, final WebSudoManager webSudoManager,
            final PermissionService permissionService) {
        this.loginUriProvider = checkNotNull(loginUriProvider, "loginProvider");
        this.webSudoManager = checkNotNull(webSudoManager, "webSudoManager");
        this.permissionService = checkNotNull(permissionService, "permissionService");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        checkPermission(request, response);
        enforceLoggedIn(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        checkPermission(request, response);
        enforceLoggedIn(request, response);
    }

    protected void checkPermission(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (!permissionService.checkIfUserIsGroupMember("Timesheet") &&
                    !permissionService.checkIfUserIsGroupMember("jira-administrators") &&
                    !permissionService.checkIfUserIsGroupMember("Jira-Test-Administrators")) {
                response.sendRedirect(JIRA_LOGIN_JSP);
                return;
            }
        } catch (PermissionException e) {
            response.sendRedirect(JIRA_LOGIN_JSP);
            e.printStackTrace();
        }
        if (!webSudoManager.canExecuteRequest(request)) {
            webSudoManager.enforceWebSudoProtection(request, response);
            return;
        }

        response.setContentType("text/html;charset=utf-8");
    }

    private void enforceLoggedIn(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser() == null)  // (3)
        {
            res.sendRedirect(req.getContextPath() + "/plugins/servlet/login");
        }
    }
}