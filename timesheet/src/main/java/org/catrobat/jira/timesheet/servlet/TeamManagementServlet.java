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
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.services.TeamService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class TeamManagementServlet extends HttpServlet {
    private final UserManager userManager;
    private final LoginUriProvider loginUriProvider;
    private final TemplateRenderer templateRenderer;
    private final TeamService teamService;

    public TeamManagementServlet(UserManager userManager, LoginUriProvider loginUriProvider,
                                 TemplateRenderer templateRenderer, TeamService teamService) {
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.templateRenderer = templateRenderer;
        this.teamService = checkNotNull(teamService);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(userManager.getRemoteUser(request).getUsername());
        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(userManager.getRemoteUser(request).getUsername());
        UserProfile userProfile = userManager.getUserProfile(userKey);
        String loggedInUsername = (userProfile != null) ? userProfile.getUsername() : null;

        if (loggedInUsername == null) {
            redirectToLogin(request, response);
        } else if (ComponentAccessor.getGroupManager().isUserInGroup(applicationUser, "jira-administrators")) {
            Map<String, Object> paramMap = Maps.newHashMap();
            Map<String, Object> entryMap = Maps.newTreeMap();

            for (Team team : teamService.all()) {
                String html = "<div>";
                html += team.getTeamName();
                html += "</div>";
                entryMap.put(team.getID() + "", html);
                System.out.println(team.getID() + ": " + team.getTeamName());
            }

            paramMap.put("entries", entryMap);
            response.setContentType("text/html;charset=utf-8");
            templateRenderer.render("teams.vm", paramMap, response.getWriter());
        } else {
            Map<String, Object> paramMap = Maps.newHashMap();
            paramMap.put("messageWithHtml", "You don't have sufficient permission to access this page!");
            response.setContentType("text/html;charset=utf-8");
            templateRenderer.render("error.vm", paramMap, response.getWriter());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        final String teamName = request.getParameter("team");
        if (!teamName.isEmpty())
            teamService.add(teamName);

        resp.sendRedirect(request.getContextPath() + "/plugins/servlet/teams");
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
