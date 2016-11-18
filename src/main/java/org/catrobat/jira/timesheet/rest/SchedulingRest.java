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
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.scheduling.ActivityNotificationJob;
import org.catrobat.jira.timesheet.scheduling.ActivityVerificationJob;
import org.catrobat.jira.timesheet.scheduling.OutOfTimeJob;
import org.catrobat.jira.timesheet.scheduling.TimesheetScheduler;
import org.catrobat.jira.timesheet.services.*;
import org.joda.time.DateTime;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/scheduling")
@Produces({MediaType.APPLICATION_JSON})
public class SchedulingRest {
    private final ConfigService configService;
    private final PermissionService permissionService;
    private final TimesheetEntryService entryService;
    private final TimesheetService sheetService;
    private final TeamService teamService;
    private final CategoryService categoryService;
    private final TimesheetScheduler timesheetScheduler;

    public SchedulingRest(final ConfigService configService, final PermissionService permissionService,
            final TimesheetEntryService entryService, final TimesheetService sheetService, TeamService teamService,
            CategoryService categoryService, TimesheetScheduler timesheetScheduler) {
        this.configService = configService;
        this.permissionService = permissionService;
        this.entryService = entryService;
        this.sheetService = sheetService;
        this.teamService = teamService;
        this.categoryService = categoryService;
        this.timesheetScheduler = timesheetScheduler;
    }

    @GET
    @Path("/trigger/activity/notification")
    public Response activityNotification(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return unauthorized;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("sheetService", sheetService);
        params.put("entryService", entryService);
        params.put("teamService", teamService);
        params.put("configService", configService);

        ActivityNotificationJob activityNotificationJob = new ActivityNotificationJob();
        activityNotificationJob.execute(params);

        return Response.noContent().build();
    }

    private List<ApplicationUser> getCoordinators(ApplicationUser user) {
        List<ApplicationUser> coordinatorList = new LinkedList<>();
        for (Team team : teamService.getTeamsOfUser(user.getName())) {
            for (String coordinator : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.COORDINATOR)) {
                coordinatorList.add(ComponentAccessor.getUserManager().getUserByName(coordinator));
            }
        }
        return coordinatorList;
    }

    @GET
    @Path("/trigger/activity/verification")
    public Response activityVerification(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return unauthorized;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("sheetService", sheetService);
        params.put("entryService", entryService);
        params.put("teamService", teamService);
        params.put("categoryService", categoryService);

        ActivityVerificationJob activityVerificationJob = new ActivityVerificationJob();
        activityVerificationJob.execute(params);

        return Response.noContent().build();
    }

    @GET
    @Path("/trigger/out/of/time/notification")
    public Response outOfTimeNotification(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return unauthorized;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("sheetService", sheetService);
        params.put("configService", configService);

        OutOfTimeJob outOfTimeJob = new OutOfTimeJob();
        outOfTimeJob.execute(params);

        return Response.noContent().build();
    }

    private boolean isDateOlderThanOneMonth(Date date) {
        DateTime oneMonthAgo = new DateTime().minusMonths(1);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(oneMonthAgo) < 0);
    }

    private boolean isDateOlderThanXDays(Date date, int days) {
        DateTime xDaysAgo = new DateTime().minusDays(days);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(xDaysAgo) < 0);
    }
}
