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
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
        Collection<User> userList = ComponentAccessor.getUserManager().getAllUsers();
        Config config = configService.getConfiguration();

        for (User user : userList) {
            for (Timesheet timesheet : timesheetList) {
                if (timesheet.getUserKey().equals(ComponentAccessor.getUserManager().getUserByName(user.getName()).getKey())) {
                    if (!timesheet.getIsActive()) {
                        //check if user made an inactive entry and entry date is equal to now
                        if (entryService.getEntriesBySheet(timesheet)[0].getCategory().getName().equals("Inactive") &&
                                entryService.getEntriesBySheet(timesheet)[0].getBeginDate().equals(new DateTime())) {
                            //inform coordinators that he should be active by now
                            for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
                                sendMail(createEmail(coordinatorMailAddress, config.getMailSubjectInactive(),
                                        "User: " + user.getName() + " should be 'active' since today, but is not."));
                            }
                        }

                        //Email to users coodinators
                        for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
                            sendMail(createEmail(coordinatorMailAddress, config.getMailSubjectInactive(),
                                    config.getMailBodyInactive()));
                        }

                        //Email to admins
                        for (User administrator : ComponentAccessor.getUserUtil().getJiraSystemAdministrators()) {
                            sendMail(createEmail(administrator.getEmailAddress(), config.getMailSubjectInactive()
                                    , config.getMailBodyInactive()));
                        }
                    } else {
                        if (entryService.getEntriesBySheet(timesheet).length > 0)
                            if (dateIsOlderThanTwoMonths(entryService.getEntriesBySheet(timesheet)[0].getBeginDate())) {
                                //Email to admins
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

        List<Timesheet> timesheetList = sheetService.all();
        for (Timesheet timesheet : timesheetList) {
            if (timesheet.getEntries().length > 0) {
                TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);
                if (dateIsOlderThanTwoWeeks(entries[0].getBeginDate())) {
                    timesheet.setIsActive(false);
                    timesheet.save();
                } else if (entries[0].getCategory().getName().equals("Inactive")) {
                    timesheet.setIsActive(false);
                    timesheet.save();
                } else {
                    //latest entry is not older than 2 weeks and user did not add inactive entry
                    timesheet.setIsActive(true);
                    timesheet.save();
                }
            } else {
                //no entry available
                timesheet.setIsActive(false);
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
                if (timesheet.getUserKey().equals(
                        ComponentAccessor.getUserManager().getUserByName(user.getName()).getKey())) {
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

    private boolean dateIsOlderThanTwoWeeks(Date date) {
        DateTime twoWeeksAgo = new DateTime().minusWeeks(2);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoWeeksAgo) < 0);
    }

    private boolean dateIsOlderThanTwoMonths(Date date) {
        DateTime twoMonthsAgo = new DateTime().minusMonths(2);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoMonthsAgo) < 0);
    }
}
