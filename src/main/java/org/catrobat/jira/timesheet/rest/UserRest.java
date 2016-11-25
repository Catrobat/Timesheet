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
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserUtil;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.rest.json.JsonUser;
import org.catrobat.jira.timesheet.services.PermissionService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Path("/user")
public class UserRest {
    public static final String DISABLED_GROUP = "Disabled";
    private final ConfigService configService;
    private final PermissionService permissionService;

    public UserRest(final ConfigService configService, PermissionService permissionService) {
        this.configService = configService;
        this.permissionService = permissionService;
    }

    @GET
    @Path("/getUsers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers(@Context HttpServletRequest request) {
        Response response = permissionService.checkGlobalPermission();
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
}
