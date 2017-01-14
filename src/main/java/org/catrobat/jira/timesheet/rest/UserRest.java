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

package org.catrobat.jira.timesheet.rest;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserUtil;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.rest.json.JsonUser;
import org.catrobat.jira.timesheet.rest.json.JsonUserInformation;
import org.catrobat.jira.timesheet.services.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/user")
public class UserRest {
    public static final String DISABLED_GROUP = "Disabled";
    private final ConfigService configService;
    private final PermissionService permissionService;
    private final TimesheetService timesheetService;
    private final TimesheetEntryService timesheetEntryService;
    private final TeamService teamService;

    public UserRest(ConfigService configService, PermissionService permissionService,
            TimesheetService timesheetService, TimesheetEntryService timesheetEntryService, TeamService teamService) {
        this.configService = configService;
        this.permissionService = permissionService;
        this.timesheetService = timesheetService;
        this.timesheetEntryService = timesheetEntryService;
        this.teamService = teamService;
    }

    @GET
    @Path("/getUsers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers(@Context HttpServletRequest request) {
        // TODO: check whether user permission is still needed
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        UserUtil userUtil = ComponentAccessor.getUserUtil();
        List<JsonUser> jsonUserList = new ArrayList<JsonUser>();
        Set<ApplicationUser> allUsers = ComponentAccessor.getUserManager().getAllUsers();
        TreeSet<ApplicationUser> allSortedUsers = RestUtils.getInstance().getSortedUsers(allUsers);
        for (ApplicationUser user : allSortedUsers) {
            JsonUser jsonUser = new JsonUser();
            jsonUser.setEmail(user.getEmailAddress());
            jsonUser.setUserName(user.getName());

            String displayName = user.getDisplayName();
            int lastSpaceIndex = displayName.lastIndexOf(' ');
            if (lastSpaceIndex >= 0) {
                jsonUser.setFirstName(displayName.substring(0, lastSpaceIndex));
                jsonUser.setLastName(displayName.substring(lastSpaceIndex + 1));
            } else {
                jsonUser.setFirstName(displayName);
            }

            boolean isActive = true;
            for (Group group : userUtil.getGroupsForUser(user.getName())) {
                if (group.getName().toLowerCase().equals(DISABLED_GROUP.toLowerCase())) {
                    isActive = false;
                    break;
                }
            }

            jsonUser.setActive(isActive);
            jsonUserList.add(jsonUser);
        }

        return Response.ok(jsonUserList).build();
    }

    @GET
    @Path("/getUsersForCoordinator")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersForCoordinator(@Context HttpServletRequest request) {
        ApplicationUser user;
        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        if (!(permissionService.isUserTeamCoordinator(user) || permissionService.isReadOnlyUser(user))) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not a team coordinator.").build();
        }

        List<JsonUserInformation> jsonUserInformationList = new ArrayList<>();

        for (Timesheet timesheet : timesheetService.all()) {
            if (permissionService.isUserCoordinatorOfTimesheet(user, timesheet)) {
                JsonUserInformation jsonUserInformation = new JsonUserInformation();
                // TODO: check whether user key == name
                jsonUserInformation.setUserName(getUserNameOfUserKey(timesheet.getUserKey()));
                // TODO: if else cascade for all combinations
                if (timesheet.getIsActive()) {
                    jsonUserInformation.setState("Active");
                } else {
                    jsonUserInformation.setState("Inactive");
                }
                jsonUserInformation.setLatestEntryDate(timesheet.getLatestEntryBeginDate());
                jsonUserInformation.setHoursPerHalfYear(timesheetEntryService.getHoursOfLastXMonths(timesheet, 6));
                jsonUserInformation.setHoursPerMonth(timesheetEntryService.getHoursOfLastXMonths(timesheet, 1));
                jsonUserInformationList.add(jsonUserInformation);
            }
        }

        return Response.ok(jsonUserInformationList).build();
    }

    private String getUserNameOfUserKey(String userKey) {
        return ComponentAccessor.getUserManager().getUserByKey(userKey).getName();
    }

    @GET
    @Path("/getPairProgrammingUsers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPairProgrammingUsers(@Context HttpServletRequest request) {
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        String pairProgrammingGroup = configService.getConfiguration().getPairProgrammingGroup();
        if (pairProgrammingGroup.isEmpty()) {
            return getUsers(request);
        }

        List<String> jsonUserList = new ArrayList<String>();
        Collection<ApplicationUser> allUsers = ComponentAccessor.getGroupManager().getUsersInGroup(pairProgrammingGroup);
        for (ApplicationUser user : allUsers) {
            jsonUserList.add(user.getName());
        }

        return Response.ok(jsonUserList).build();
    }

    @GET
    @Path("/getGroups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups(@Context HttpServletRequest request) {
        Response response = permissionService.checkRootPermission();
        if (response != null) {
            return response;
        }

        List<String> groupList = new ArrayList<String>();
        Collection<Group> allGroups = ComponentAccessor.getGroupManager().getAllGroups();
        for (Group group : allGroups) {
            groupList.add(group.getName());
        }

        return Response.ok(groupList).build();
    }
}
