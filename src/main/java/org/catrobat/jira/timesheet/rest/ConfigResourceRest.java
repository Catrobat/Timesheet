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

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.rest.json.JsonCategory;
import org.catrobat.jira.timesheet.rest.json.JsonConfig;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;
import org.catrobat.jira.timesheet.utility.DatabaseUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.catrobat.jira.timesheet.rest.RestUtils.convertTeamsToJSON;

@Path("/config")
@Produces({MediaType.APPLICATION_JSON})
public class ConfigResourceRest {
    private final ConfigService configService;
    private final TeamService teamService;
    private final CategoryService categoryService;
    private final PermissionService permissionService;
    private final ActiveObjects ao;

    public ConfigResourceRest(final ConfigService configService, final TeamService teamService,
            final CategoryService categoryService, final PermissionService permissionService, ActiveObjects ao) {
        this.configService = configService;
        this.teamService = teamService;
        this.categoryService = categoryService;
        this.permissionService = permissionService;
        this.ao = ao;
    }

    @GET
    @Path("/getCategories")
    public Response getCategories(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkUserPermission();
        if (unauthorized != null) {
            return unauthorized;
        }

        List<JsonCategory> categories = new LinkedList<JsonCategory>();
        List<Category> categoryList = categoryService.all();
        Collections.sort(categoryList, (o1, o2) -> o1.getName().compareTo(o2.getName()));

        for (Category category : categoryList) {
            categories.add(new JsonCategory(category.getID(), category.getName()));
        }

        return Response.ok(categories).build();
    }

    //unused
    @GET
    @Path("/getTeams")
    public Response getTeams(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkUserPermission();
        if (unauthorized != null) {
            return unauthorized;
        }

        List<JsonTeam> teams = new LinkedList<>();

        List<Team> teamList = teamService.all();
        Collections.sort(teamList, (o1, o2) -> o1.getTeamName().compareTo(o2.getTeamName()));
        convertTeamsToJSON(teamList);

        return Response.ok(teams).build();
    }

    @GET
    @Path("/getConfig")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkUserPermission();
        if (unauthorized != null) {
            return unauthorized;
        }
        return Response.ok(new JsonConfig(configService)).build();
    }

    @PUT
    @Path("/saveConfig")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setConfig(final JsonConfig jsonConfig, @Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkRootPermission();
        if (unauthorized != null) {
            return unauthorized;
        }

        configService.editMail(jsonConfig.getMailFromName(), jsonConfig.getMailFrom(),
                jsonConfig.getMailSubjectTime(), jsonConfig.getMailSubjectInactive(),
                jsonConfig.getMailSubjectOffline(), jsonConfig.getMailSubjectActive(), jsonConfig.getMailSubjectEntry(), jsonConfig.getMailBodyTime(),
                jsonConfig.getMailBodyInactive(), jsonConfig.getMailBodyOffline(), jsonConfig.getMailBodyActive(), jsonConfig.getMailBodyEntry());

        configService.editReadOnlyUsers(jsonConfig.getReadOnlyUsers());

        //clear fields
        configService.clearTimesheetAdminGroups();
        configService.clearTimesheetAdmins();

        // add TimesheetAdmin group
        if (jsonConfig.getTimesheetAdminGroups() != null) {
            for (String approvedGroupName : jsonConfig.getTimesheetAdminGroups()) {
                configService.addTimesheetAdminGroup(approvedGroupName);
                // add all users in group
                Collection<ApplicationUser> usersInGroup = ComponentAccessor.getGroupManager().getUsersInGroup(approvedGroupName);
                for (ApplicationUser user : usersInGroup) {
                    configService.addTimesheetAdmin(user);
                }

            }
        }

        // add TimesheetAdmins
        if (jsonConfig.getTimesheetAdmins() != null) {
            for (String username : jsonConfig.getTimesheetAdmins()) {
                ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(username);
                if (user != null) {
                    configService.addTimesheetAdmin(user);
                    //RestUtils.getInstance().printUserInformation(username, user);
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

    @GET
    @Path("/dropTables")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dropTables(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkRootPermission();
        if (unauthorized != null) {
            return unauthorized;
        }

        DatabaseUtil db = new DatabaseUtil(ao);
        db.clearAllTimesheetTables();
        return Response.status(Response.Status.OK).entity("All timesheet tables has been dropped!").build();
    }

    @PUT
    @Path("/addTeamPermission")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addTeamPermission(final String teamName, @Context HttpServletRequest request) {
        Response unauthorized = checkParam();
        if (unauthorized != null) {
            return unauthorized;
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

    private Response checkParam(String... strings) {
        Response unauthorized = permissionService.checkRootPermission();
        if (unauthorized != null) {
            return unauthorized;
        }
        for (String param : strings) {
            if (param.isEmpty()) {
                return Response.status(Response.Status.FORBIDDEN).entity("Name must not be empty.").build();
            }
        }
        return null;
    }

    @PUT
    @Path("/editTeamName")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editTeamPermission(final String[] teams, @Context HttpServletRequest request) {
        Response unauthorized = checkParam(teams);
        if (unauthorized != null) {
            return unauthorized;
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
        Response unauthorized = checkParam(teamName);
        if (unauthorized != null) {
            return unauthorized;
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
        Response unauthorized = checkParam(categoryName);
        if (unauthorized != null) {
            return unauthorized;
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
        Response unauthorized = checkParam(categories);
        if (unauthorized != null) {
            return unauthorized;
        } else if (categories == null || categories.length != 2) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Not enough arguments.").build();
        } else if (categories[1].trim().isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("Category name must not be empty.").build();
        } else if (categories[1].equals(categories[0])) {
            return Response.status(Response.Status.FORBIDDEN).entity("New category name must be different.").build();
        }

        //categories[0] old name
        //categories[1] new name

        if (categoryService.getCategoryByName(categories[1]) != null) {
            return Response.status(Response.Status.CONFLICT).entity("Category name already exists.").build();
        }

        if (SpecialCategories.LIST.contains(categories[0])) {
            return Response.status(Response.Status.CONFLICT).entity("Special categories cannot be renamed.").build();
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
    public Response removeCategory(final String removeCategory, @Context HttpServletRequest request) throws ServiceException {
        Response unauthorized = checkParam(removeCategory);
        if (unauthorized != null) {
            return unauthorized;
        }

        boolean successful = categoryService.removeCategory(removeCategory);

        if (successful) {
            return Response.noContent().build();
        }

        return Response.status(Response.Status.CONFLICT).entity("Could not remove Category.").build();
    }
}
