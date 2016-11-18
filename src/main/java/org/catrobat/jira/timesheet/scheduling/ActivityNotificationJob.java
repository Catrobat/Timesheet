package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.mail.Email;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.joda.time.DateTime;

import java.util.*;

public class ActivityNotificationJob implements PluginJob {

    private TimesheetService sheetService;
    private TimesheetEntryService entryService;
    private TeamService teamService;
    private ConfigService configService;

    @Override
    public void execute(Map<String, Object> map) {
        System.out.println((new Date()).toString() + " ActivityNotificationJob");

        sheetService = (TimesheetService)map.get("sheetService");
        entryService = (TimesheetEntryService)map.get("entryService");
        teamService = (TeamService)map.get("teamService");
        configService = (ConfigService)map.get("configService");

        List<Timesheet> timesheetList = sheetService.all();
        Config config = configService.getConfiguration();
        for (Timesheet timesheet : timesheetList) {
            String userKey = timesheet.getUserKey();
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
            if (entryService.getEntriesBySheet(timesheet).length == 0) { // nothing to do
                continue;
            }
            if (timesheet.getIsOffline()) {  // user is offline

                //inform coordinators
                for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
                    sendMail(createEmail(coordinatorMailAddress, config.getMailSubjectOfflineState(),
                            config.getMailBodyOfflineState()));
                    System.out.println("Coordinator-email: " + coordinatorMailAddress);
                }

                //Todo: inform all user in approved group
                TimesheetAdmin[] timesheetAdmins = config.getTimesheetAdminUsers();
                //inform timesheet admins (approved users)
                for (TimesheetAdmin timesheetAdmin : timesheetAdmins) {
                    System.out.println("timesheetAdmin: = " + timesheetAdmin.getUserName());
                    sendMail(createEmail(timesheetAdmin.getEmailAddress(), config.getMailSubjectOfflineState(),
                            config.getMailBodyOfflineState()));
                }
            } else if (!timesheet.getIsActive()) { // user is inactive
                //inform coordinators
                TimesheetEntry latestInactiveEntry = getLatestInactiveEntry(timesheet);
                if (latestInactiveEntry != null) {
                    if (isDateOlderThanTwoWeeks(latestInactiveEntry.getInactiveEndDate())) {
                        //inform coordinators that he should be active since two weeks
                        for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
                            sendMail(createEmail(coordinatorMailAddress, config.getMailSubjectInactiveState(),
                                    config.getMailBodyInactiveState()));
                            System.out.println("Coordinator-email: " + coordinatorMailAddress);
                        }
                    }
                }
            } else { // user is active again
                        /*for (Map.Entry<ApplicationUser, Vector<ApplicationUser>> entry : notifyUsersMap.entrySet()) {
                            List<ApplicationUser> appUserList = entry.getValue();
                            for (ApplicationUser appUser : appUserList) {
                                sendMail(createEmail(appUser.getEmailAddress(), config.getMailSubjectActiveState(),
                                        config.getMailBodyActiveState()));
                            }
                        }*/
            }
        }

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

    private boolean isDateOlderThanTwoWeeks(Date date) {
        DateTime twoWeeksAgo = new DateTime().minusWeeks(2);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoWeeksAgo) < 0);
    }

    private List<String> getCoordinatorsMailAddress(ApplicationUser user) {
        List<String> coordinatorMailAddressList = new LinkedList<String>();
        for (Team team : teamService.getTeamsOfUser(user.getName())) {
            for (String coordinator : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.COORDINATOR))
                coordinatorMailAddressList.add(ComponentAccessor.getUserManager().getUserByName(coordinator).getEmailAddress());
        }

        return coordinatorMailAddressList;
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
