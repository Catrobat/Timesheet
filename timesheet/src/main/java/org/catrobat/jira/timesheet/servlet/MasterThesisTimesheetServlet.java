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
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class MasterThesisTimesheetServlet extends HttpServlet {

    private final LoginUriProvider loginUriProvider;
    private final TemplateRenderer templateRenderer;
    private final TimesheetService sheetService;
    private final PermissionService permissionService;

    public MasterThesisTimesheetServlet(final LoginUriProvider loginUriProvider, final TemplateRenderer templateRenderer,
            final TimesheetService sheetService, final PermissionService permissionService) {
        this.loginUriProvider = loginUriProvider;
        this.templateRenderer = templateRenderer;
        this.sheetService = sheetService;
        this.permissionService = permissionService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            if (!permissionService.checkIfUserIsGroupMember(request, "Master-Students")) {
                throw new ServletException("User is no Master-Students-Group member.");
            }

            ApplicationUser user = permissionService.checkIfUserExists(request);
            String userKey = ComponentAccessor.
                    getUserKeyService().getKeyForUsername(user.getUsername());
            Timesheet sheet = null;
            if (sheetService.userHasTimesheet(userKey, true)) {
                sheet = sheetService.getTimesheetByUser(userKey, true);
            }

            if (sheet == null) {
                sheet = sheetService.add(userKey, 0, 0, 900, 0, 0, "Master Thesis",
                        "Hint: Good Luck & Have Fun.", 30, true, true, true);
            }

            Map<String, Object> paramMap = Maps.newHashMap();
            paramMap.put("timesheetid", sheet.getID());
            if (permissionService.checkIfUserIsGroupMember(request, "jira-administrators") ||
                    permissionService.checkIfUserIsGroupMember(request, "Jira-Test-Administrators")) {
                paramMap.put("isadmin", true);
            } else {
                paramMap.put("isadmin", false);
            }
            if (permissionService.checkIfUserIsGroupMember(request, "Coordinators")) {
                paramMap.put("iscoordinator", true);
            } else {
                paramMap.put("iscoordinator", false);
            }

            paramMap.put("ismasterthesistimesheet", true);
            response.setContentType("text/html;charset=utf-8");
            templateRenderer.render("timesheet.vm", paramMap, response.getWriter());
        } catch (ServletException e) {
            redirectToLogin(request, response);
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }

    private URI getUri(HttpServletRequest request) {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null) {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }

}
