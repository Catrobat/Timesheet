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

import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.rest.TimesheetRest;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class TimesheetServlet extends HttpServlet {

    private final LoginUriProvider loginUriProvider;
    private final TemplateRenderer templateRenderer;
    private final TimesheetService sheetService;
    private final PermissionService permissionService;
    private static final Logger logger = LoggerFactory.getLogger(TimesheetServlet.class);
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(TimesheetServlet.class);

    public TimesheetServlet(final LoginUriProvider loginUriProvider, final TemplateRenderer templateRenderer,
            final TimesheetService sheetService, final PermissionService permissionService) {
        this.loginUriProvider = loginUriProvider;
        this.templateRenderer = templateRenderer;
        this.sheetService = sheetService;
        this.permissionService = permissionService;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Response unauthorized = permissionService.checkUserPermission();
        if (unauthorized != null) {
            redirectToLogin(request, response);
            return;
        }
        super.service(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            ApplicationUser user = permissionService.checkIfUserExists();
            if (!permissionService.isUserEligibleForTimesheet(user)) {
                // Maybe send Error here, do not redirect to login since the user is already logged in.
                return;
            }
            String userKey = user.getKey();
            Map<String, Object> paramMap = Maps.newHashMap();
            Timesheet timesheet = null;

            //request with parameter getting sheet
            if(request.getParameterMap().get("timesheetID") != null){
                if(!permissionService.isUserEligibleForTimesheet(user)){
                    response.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
                    return;
                }
                if(request.getParameterMap().get("timesheetID").length != 1) {
                    response.setStatus(Response.Status.CONFLICT.getStatusCode());
                    return;
                }
                try{
                    LOGGER.error("we have got a timesheet Request for: " + request.getParameterMap().get("timesheetID")[0]);
                    timesheet = sheetService.getTimesheetByID(Integer.parseInt(request.getParameterMap().get("timesheetID")[0]));

                    if(timesheet == null){
                        response.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
                        return;
                    }if (!permissionService.userCanViewTimesheet(user,timesheet)){
                        response.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
                        return;
                    }

                    if (timesheet.getTargetHours() == 0 && timesheet.getLectures().isEmpty())
                        paramMap.put("isInit", true);
                    else
                        paramMap.put("isInit", false);

                    paramMap.put("external", true);

                }catch (NumberFormatException e){
                    response.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
                    return;
                }
            }else {
                //getting sheet for current user
                if (sheetService.userHasTimesheet(userKey)) {
                    timesheet = sheetService.getTimesheetByUser(userKey);
                    LOGGER.error("Current user (user key: " + userKey + ") has an active timesheet (timesheet id: " + timesheet.getID() + ")");

                    if (timesheet.getTargetHours() == 0 && timesheet.getLectures().isEmpty())
                        paramMap.put("isInit", true);
                    else
                        paramMap.put("isInit", false);
                }
                if (timesheet == null) {
                    timesheet = sheetService.add(userKey, user.getDisplayName(),  0, 0, 0, "",
                            "", Timesheet.State.ACTIVE);
                    LOGGER.error("We created a new Timesheet for user with key: " + userKey);
                    paramMap.put("isInit", true);
                }
                paramMap.put("external", false);
            }

            if (permissionService.timesheetAdminExists()) {
                paramMap.put("isAdmin", permissionService.isTimesheetAdmin(user));
            } else {
                paramMap.put("isAdmin", permissionService.isJiraAdministrator(user));
            }

            paramMap.put("isCoordinator", permissionService.isUserTeamCoordinator(user));
            paramMap.put("isReadOnlyUser", permissionService.isReadOnlyUser(user));
            paramMap.put("timesheetID", timesheet.getID());

            response.setContentType("text/html;charset=utf-8");
            templateRenderer.render("timesheet.vm", paramMap, response.getWriter());
        } catch (ServiceException e) {
            redirectToLogin(request, response);
        } catch (PermissionException e) {
            redirectToLogin(request, response);
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
