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
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserProfile;
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

public class TimesheetServlet extends HttpServlet {

    private final LoginUriProvider loginUriProvider;
    private final TemplateRenderer templateRenderer;
    private final TimesheetService sheetService;
    private final PermissionService permissionService;

    public TimesheetServlet(final LoginUriProvider loginUriProvider, final TemplateRenderer templateRenderer,
                            final TimesheetService sheetService, final PermissionService permissionService) {
        this.loginUriProvider = loginUriProvider;
        this.templateRenderer = templateRenderer;
        this.sheetService = sheetService;
        this.permissionService = permissionService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            if (!permissionService.checkIfUserIsGroupMember(request, "AtlassianDevelopers") &&
                    !permissionService.checkIfUserIsGroupMember(request, "Timesheet") &&
                    !permissionService.checkIfUserIsGroupMember(request, "jira-administrators")) {
                throw new ServletException("User is no Timesheet-Group member, or Administrator.");
            }

            UserProfile userProfile = permissionService.checkIfUserExists(request);
            String userKey = ComponentAccessor.
                    getUserKeyService().getKeyForUsername(userProfile.getUsername());
            Map<String, Object> paramMap = Maps.newHashMap();
            Timesheet sheet;
            if (permissionService.checkIfUserIsGroupMember(request, "jira-administrators")) {
                paramMap.put("isadmin", true);
                sheet = sheetService.getAdministratorTimesheet(userKey);
            } else {
                paramMap.put("isadmin", false);
                sheet = sheetService.getTimesheetByUser(userKey, false);
            }

            if (sheet == null) {
                sheet = sheetService.add(userKey, 0, 0, 150, 0, 0, "Bachelor Thesis",
                        "Hint: Do not make other people angry.", 5, "Not Available", true, true, false);
            }

            paramMap.put("timesheetid", sheet.getID());
            paramMap.put("ismasterthesistimesheet", false);
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
