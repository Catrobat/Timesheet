package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.mail.Email;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ActivityNotificationJob implements PluginJob {

    private TimesheetService sheetService;
    private TimesheetEntryService entryService;
    private TeamService teamService;
    private ConfigService configService;
    private SchedulingService schedulingService;

    @Override
    public void execute(Map<String, Object> map) {
        System.out.println((new Date()).toString() + " ActivityNotificationJob");

        sheetService = (TimesheetService) map.get("sheetService");
        entryService = (TimesheetEntryService) map.get("entryService");
        teamService = (TeamService) map.get("teamService");
        configService = (ConfigService) map.get("configService");
        schedulingService = (SchedulingService) map.get("schedulingService");

        List<Timesheet> timesheetList = sheetService.all();
        Config config = configService.getConfiguration();
        for (Timesheet timesheet : timesheetList) {
            String userKey = timesheet.getUserKey();
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
            if (entryService.getEntriesBySheet(timesheet).length == 0) { // nothing to do
                continue;
            }
            if (timesheet.getState() == Timesheet.State.INACTIVE_OFFLINE) {  // user is offline
                //inform coordinators
                for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
                    sendMail(createEmail(coordinatorMailAddress, config.getMailSubjectOfflineState(),
                            config.getMailBodyOfflineState()));
                    System.out.println("Coordinator-email: " + coordinatorMailAddress);
                }

                //inform timesheet admins
                TimesheetAdmin[] timesheetAdmins = config.getTimesheetAdminUsers();
                for (TimesheetAdmin timesheetAdmin : timesheetAdmins) {
                    System.out.println("timesheetAdmin: = " + timesheetAdmin.getUserName());
                    sendMail(createEmail(timesheetAdmin.getEmailAddress(), config.getMailSubjectOfflineState(),
                            config.getMailBodyOfflineState()));
                }
            } else if (timesheet.getState() == Timesheet.State.INACTIVE) { // user is inactive
                //inform coordinators
                TimesheetEntry latestInactiveEntry = entryService.getLatestInactiveEntry(timesheet);
                if (latestInactiveEntry != null) {
                    if (schedulingService.isOlderThanInactiveTime(latestInactiveEntry.getInactiveEndDate())) {
                        //inform coordinators that he should be active since two weeks
                        for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
                            sendMail(createEmail(coordinatorMailAddress, config.getMailSubjectInactiveState(),
                                    config.getMailBodyInactiveState()));
                            System.out.println("Coordinator-email: " + coordinatorMailAddress);
                        }
                    }
                }
            } /*else if (timesheet.getIsReactivated()) {
                // user is active again
                //inform coordinators that the user is back and active now
                for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
                    sendMail(createEmail(coordinatorMailAddress, config.getMailSubjectActiveState(),
                            config.getMailBodyActiveState()));
                }

                // user was offline
                TimesheetEntry latestOfflineEntry = getLatestOfflineEntry(timesheet);
                if (schedulingService.isOlderThanOfflineTime(latestOfflineEntry.getDeactivateEndDate())) {
                    //inform timesheet admins
                    TimesheetAdmin[] timesheetAdmins = config.getTimesheetAdminUsers();
                    for (TimesheetAdmin timesheetAdmin : timesheetAdmins) {
                        System.out.println("timesheetAdmin: = " + timesheetAdmin.getUserName());
                        sendMail(createEmail(timesheetAdmin.getEmailAddress(), config.getMailSubjectActiveState(),
                                config.getMailBodyActiveState()));
                    }
                }
            } */else {
                System.out.println("User is still active and no reaction is necessary.");
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

    private List<String> getCoordinatorsMailAddress(ApplicationUser user) {
        List<String> coordinatorMailAddressList = new LinkedList<>();
        for (Team team : teamService.getTeamsOfUser(user.getName())) {
            for (String coordinator : configService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.COORDINATOR))
                coordinatorMailAddressList.add(ComponentAccessor.getUserManager().getUserByName(coordinator).getEmailAddress());
        }

        return coordinatorMailAddressList;
    }

    private void buildEmailInactive(String emailTo, Timesheet sheet, ApplicationUser user) {
        Config config = configService.getConfiguration();

        String mailSubject = config.getMailSubjectInactiveState() != null && config.getMailSubjectInactiveState().length() != 0
                ? config.getMailSubjectInactiveState() : "[Timesheet - Timesheet Inactive Notification]";
        String mailBody = config.getMailBodyInactiveState() != null && config.getMailBodyInactiveState().length() != 0
                ? config.getMailBodyInactiveState() : "Hi " + user.getDisplayName() + ",\n" +
                "we could not see any activity in your timesheet since the last two weeks.\n" +
                "Information: an inactive entry was created automatically.\n\n" +
                "Best regards,\n" +
                "Catrobat-Admins";

        mailBody = mailBody.replaceAll("\\{\\{name\\}\\}", user.getDisplayName());
        if (sheet.getEntries().length > 0) {
            mailBody = mailBody.replaceAll("\\{\\{date\\}\\}", sheet.getEntries()[0].getBeginDate().toString());
        }

        Email email = createEmail(emailTo, mailSubject, mailBody);
        sendMail(email);
    }
}
