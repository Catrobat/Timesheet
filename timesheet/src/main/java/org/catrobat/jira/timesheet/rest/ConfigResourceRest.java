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

package org.catrobat.jira.timesheet.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.rest.json.JsonCategory;
import org.catrobat.jira.timesheet.rest.json.JsonConfig;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Path("/config")
@Produces({MediaType.APPLICATION_JSON})
public class ConfigResourceRest {
    private final ConfigService configService;
    private final UserManager userManager;
    private final TeamService teamService;
    private final CategoryService categoryService;
    private final PermissionService permissionService;

    public ConfigResourceRest(final UserManager userManager, final ConfigService configService,
                              final TeamService teamService, final CategoryService categoryService,
                              final PermissionService permissionService) {
        this.configService = configService;
        this.teamService = teamService;
        this.userManager = userManager;
        this.categoryService = categoryService;
        this.permissionService = permissionService;
    }

    @GET
    @Path("/getCategories")
    public Response getCategories(@Context HttpServletRequest request) {

        List<JsonCategory> categories = new LinkedList<JsonCategory>();

        for (Category category : categoryService.all()) {
            categories.add(new JsonCategory(category.getID(), category.getName()));
        }

        return Response.ok(categories).build();
    }

    @GET
    @Path("/getTeams")
    public Response getTeams(@Context HttpServletRequest request) throws ServiceException {
        if (permissionService.checkIfUserExists(request) == null) {
            Response.serverError().entity("Access denied.");
        }

        List<JsonTeam> teams = new LinkedList<JsonTeam>();

        for (Team team : teamService.all()) {
            Category[] categories = team.getCategories();
            int[] categoryIDs = new int[categories.length];
            for (int i = 0; i < categories.length; i++) {
                categoryIDs[i] = categories[i].getID();
            }
            teams.add(new JsonTeam(team.getID(), team.getTeamName(), categoryIDs));
        }

        return Response.ok(teams).build();
    }

    @GET
    @Path("/getTeamList")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTeamList(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return unauthorized;
        }

        List<String> teamList = new ArrayList<String>();
        for (Team team : configService.getConfiguration().getTeams()) {
            teamList.add(team.getTeamName());
        }

        return Response.ok(teamList).build();
    }

    @GET
    @Path("/getConfig")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@Context HttpServletRequest request) {

        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return unauthorized;
        }

        return Response.ok(new JsonConfig(configService)).build();
    }

    @PUT
    @Path("/saveConfig")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setConfig(final JsonConfig jsonConfig, @Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return unauthorized;
        }

        configService.editMail(jsonConfig.getMailFromName(), jsonConfig.getMailFrom(),
                jsonConfig.getMailSubjectTime(), jsonConfig.getMailSubjectInactive(),
                jsonConfig.getMailSubjectEntry(), jsonConfig.getMailBodyTime(),
                jsonConfig.getMailBodyInactive(), jsonConfig.getMailBodyEntry());

        configService.editSupervisedUsers(jsonConfig.getSupervisors());

        if (jsonConfig.getApprovedGroups() != null) {
            configService.clearApprovedGroups();
            for (String approvedGroupName : jsonConfig.getApprovedGroups()) {
                configService.addApprovedGroup(approvedGroupName);
            }
        }

        if (jsonConfig.getApprovedUsers() != null) {
            configService.clearApprovedUsers();
            for (String approvedUserName : jsonConfig.getApprovedUsers()) {
                UserProfile userProfile = userManager.getUserProfile(approvedUserName);
                if (userProfile != null) {
                    configService.addApprovedUser(userProfile.getUsername(), ComponentAccessor.
                            getUserKeyService().getKeyForUsername(userProfile.getUsername()));
                }
            }
        }

        if (jsonConfig.getTeams() != null) {
            for (JsonTeam jsonTeam : jsonConfig.getTeams()) {
                configService.editTeam(jsonTeam.getTeamName(), jsonTeam.getCoordinatorGroups(),
                        jsonTeam.getDeveloperGroups(), jsonTeam.getTeamCategoryNames());
            }
        }

        return Response.noContent().build();
    }

    @PUT
    @Path("/addTeamPermission")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addTeamPermission(final String teamName, @Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkPermission(request);

        if (unauthorized != null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Access denied.").build();
        } else if (teamName.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("Team name must not be empty.").build();
        }

        Team[] teams = configService.getConfiguration().getTeams();
        for (Team team : teams) {
            if (team.getTeamName().compareTo(teamName) == 0) {
                return Response.status(Response.Status.FORBIDDEN).entity("Team already exists.").build();
            }
        }

        boolean successful = configService.addTeam(teamName, null, null, null) != null;

        if (successful) {
            return Response.noContent().build();
        }

        return Response.serverError().build();
    }

    @PUT
    @Path("/editTeamName")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editTeamPermission(final String[] teams, @Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkPermission(request);

        if (unauthorized != null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Access denied.").build();
        } else if (teams == null || teams.length != 2) {
            return Response.serverError().build();
        } else if (teams[1].trim().isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("Team name must not be empty.").build();
        } else if (teams[1].equals(teams[0])) {
            return Response.status(Response.Status.FORBIDDEN).entity("New team name must be different.").build();
        }

        boolean successful = configService.editTeamName(teams[0], teams[1]) != null;

        if (successful) {
            return Response.ok().build();
        }

        return Response.serverError().build();
    }

    @PUT
    @Path("/removeTeamPermission")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeTeamPermission(final String teamName, @Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkPermission(request);

        if (unauthorized != null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Access denied.").build();
        } else if (teamName.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("Team name must not be empty.").build();
        }

        boolean successful = configService.removeTeam(teamName) != null;

        if (successful) {
            return Response.noContent().build();
        }

        return Response.serverError().build();
    }

    @PUT
    @Path("/addCategory")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCategory(final String categoryName, @Context HttpServletRequest request) throws ServiceException {
        Response unauthorized = permissionService.checkPermission(request);

        if (unauthorized != null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Access denied.").build();
        } else if (categoryName.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("Category name must not be empty.").build();
        }

        try {
            categoryService.add(categoryName);
        } catch (ServiceException e) {
            return Response.status(Response.Status.CONFLICT).entity("Category already exists.").build();
        }
        return Response.ok().build();
    }

    @PUT
    @Path("/editCategoryName")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editCategoryName(final String[] categories, @Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkPermission(request);

        if (unauthorized != null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Access denied.").build();
        } else if (categories == null || categories.length != 2) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Not enough arguments.").build();
        } else if (categories[1].trim().isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("Category name must not be empty.").build();
        } else if (categories[1].equals(categories[0])) {
            return Response.status(Response.Status.FORBIDDEN).entity("New category name must be different.").build();
        }

        List<Category> categoryNames = categoryService.all();
        for (Category category : categoryNames) {
            if (category.getName().equals(categories[1]))
                return Response.status(Response.Status.CONFLICT).entity("Category name already exists.").build();
        }

        boolean successful = configService.editCategoryName(categories[0], categories[1]) != null;

        if (successful) {
            return Response.noContent().build();
        }

        return Response.status(Response.Status.FORBIDDEN).entity("Could not edit Category.").build();
    }

    @PUT
    @Path("/removeCategory")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeCategory(final String modifyCategory, @Context HttpServletRequest request) throws ServiceException {
        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Access denied.").build();
        }

        boolean successful = categoryService.removeCategory(modifyCategory);

        if (successful) {
            return Response.noContent().build();
        }

        return Response.status(Response.Status.CONFLICT).entity("Could not remove Category.").build();
    }
}
