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


import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.mail.Email;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.sal.api.user.UserManager;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
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
    private final UserManager userManager;

    public SchedulingRest(final ConfigService configService, final PermissionService permissionService,
            final TimesheetEntryService entryService, final TimesheetService sheetService, TeamService teamService, UserManager userManager) {
        this.configService = configService;
        this.permissionService = permissionService;
        this.entryService = entryService;
        this.sheetService = sheetService;
        this.teamService = teamService;
        this.userManager = userManager;
    }

    @GET
    @Path("/trigger/activity/notification")
    public Response activityNotification(@Context HttpServletRequest request) {

        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return Response.serverError().entity("You are not authorized to trigger jobs.").build();
        }

        List<Timesheet> timesheetList = sheetService.all();
        Set<User> userList = ComponentAccessor.getUserManager().getAllUsers();
        Config config = configService.getConfiguration();

        for (User user : userList) {
            String userKey = ComponentAccessor.getUserManager().getUserByName(user.getName()).getKey();
            for (Timesheet timesheet : timesheetList) {
                if (entryService.getEntriesBySheet(timesheet).length == 0) { // nothing to do
                    continue;
                }
                if (timesheet.getUserKey().equals(userKey)) {
                    if (!timesheet.getIsActive()) { // user is inactive
                        TimesheetEntry latestInactiveEntry = getLatestInactiveEntry(timesheet);
                        if (isDateOlderThanTwoWeeks(latestInactiveEntry.getInactiveEndDate())) {
                            //inform coordinators that he should be active since two weeks
                            for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
                                sendMail(createEmail(coordinatorMailAddress, config.getMailSubjectInactive(),
                                        config.getMailBodyInactive()));
                            }
                        }

                        if (isDateOlderThanOneMonth(latestInactiveEntry.getInactiveEndDate())) {
                            for (User administrator : ComponentAccessor.getUserUtil().getJiraSystemAdministrators()) {
                                sendMail(createEmail(administrator.getEmailAddress(), config.getMailSubjectInactive()
                                        , config.getMailBodyInactive()));
                            }
                        }
                    } else { // user is active
                        TimesheetEntry latestEntry = entryService.getEntriesBySheet(timesheet)[0];
                        if (isDateOlderThanTwoMonths(latestEntry.getBeginDate())) {
                            for (User administrator : ComponentAccessor.getUserUtil().getJiraSystemAdministrators()) {
                                sendMail(createEmail(administrator.getEmailAddress(), config.getMailSubjectInactive()
                                        , config.getMailBodyInactive()));
                            }
                        }
                    }
                }
            }
        }

        return Response.noContent().build();
    }

    private List<String> getCoordinatorsMailAddress(User user) {
        List<String> coordinatorMailAddressList = new LinkedList<String>();
        for (Team team : teamService.getTeamsOfUser(user.getName())) {
            for (String coordinator : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.COORDINATOR))
                coordinatorMailAddressList.add(ComponentAccessor.getUserManager().getUserByName(coordinator).getEmailAddress());
        }

        return coordinatorMailAddressList;
    }

    private Email createEmail(String emailAddress, String emailSubject, String emailBody) {
        Email email = new Email(emailAddress);
        email.setSubject(emailSubject);
        email.setBody(emailBody);
        return email;
    }

    private void sendMail(Email email) {
        SingleMailQueueItem item = new SingleMailQueueItem(email);
        ComponentAccessor.getMailQueue().addItem(item);
    }

    @GET
    @Path("/trigger/activity/verification")
    public Response activityVerification(@Context HttpServletRequest request) {

        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return Response.serverError().entity("You are not authorized to trigger jobs.").build();
        }

        Date today = new Date();
        List<Timesheet> timesheetList = sheetService.all();
        for (Timesheet timesheet : timesheetList) {
            TimesheetEntry latestInactiveEntry = getLatestInactiveEntry(timesheet);
            if (latestInactiveEntry != null) {
                if (latestInactiveEntry.getInactiveEndDate().compareTo(today) > 0) {
                    timesheet.setIsActive(false);
                    timesheet.save();
                }
            } else { // no inactive entry found
                timesheet.setIsActive(true);
                timesheet.save();
            }
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/trigger/out/of/time/notification")
    public Response outOfTimeNotification(@Context HttpServletRequest request) {

        Response unauthorized = permissionService.checkPermission(request);
        if (unauthorized != null) {
            return Response.serverError().entity("You are not authorized to trigger jobs.").build();
        }

        List<Timesheet> timesheetList = sheetService.all();
        Collection<User> userList = ComponentAccessor.getUserManager().getAllUsers();
        Config config = configService.getConfiguration();

        for (User user : userList) {
            for (Timesheet timesheet : timesheetList) {
                String userKey = ComponentAccessor.getUserManager().getUserByName(user.getName()).getKey();
                if (timesheet.getUserKey().equals(userKey)) {
                    if ((timesheet.getTargetHours() - timesheet.getTargetHoursCompleted()) <= 80) {
                        Email email = new Email(user.getEmailAddress());
                        email.setSubject(config.getMailSubjectTime());
                        email.setBody(config.getMailBodyTime());
                        SingleMailQueueItem item = new SingleMailQueueItem(email);
                        ComponentAccessor.getMailQueue().addItem(item);
                    }
                }
            }
        }

        return Response.noContent().build();
    }

    private boolean isDateOlderThanTwoWeeks(Date date) {
        DateTime twoWeeksAgo = new DateTime().minusWeeks(2);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoWeeksAgo) < 0);
    }

    private boolean isDateOlderThanOneMonth(Date date) {
        DateTime oneMonthAgo = new DateTime().minusMonths(1);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(oneMonthAgo) < 0);
    }

    private boolean isDateOlderThanTwoMonths(Date date) {
        DateTime twoMonthsAgo = new DateTime().minusMonths(2);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoMonthsAgo) < 0);
    }

    private TimesheetEntry getLatestInactiveEntry(Timesheet timesheet) {
        TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);
        for (TimesheetEntry entry : entries) {
            if (entry.getCategory().getName().equals("Inactive")
                    && (entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0)) {
                return entry;
            }
        }
        return null;
    }
}
