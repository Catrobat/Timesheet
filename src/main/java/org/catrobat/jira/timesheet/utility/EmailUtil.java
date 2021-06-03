package org.catrobat.jira.timesheet.utility;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.mail.Email;
import com.atlassian.mail.queue.SingleMailQueueItem;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailUtil {

    private ConfigService configService;
    private static final Logger logger = LoggerFactory.getLogger(EmailUtil.class);

    public EmailUtil(ConfigService configService) {
        this.configService = configService;
    }

    public void buildEmailOutOfTime(String emailTo, Timesheet sheet, ApplicationUser user) {
        Config config = configService.getConfiguration();

        String mailSubject = config.getMailSubjectTime() != null && config.getMailSubjectTime().length() != 0
                ? config.getMailSubjectTime() : "[Timesheet - Timesheet Out Of Time Notification]";
        String mailBody = config.getMailBodyTime() != null && config.getMailBodyTime().length() != 0
                ? config.getMailBodyTime() : "Hi " + user.getDisplayName() + ",\n" +
                "you have only" + sheet.getTargetHours() + " hours left! \n" +
                "Please contact you coordinator, or one of the administrators\n\n" +
                "Best regards,\n" +
                "Catrobat-Admins";


        mailBody = mailBody.replaceAll("\\{\\{name\\}\\}", user.getDisplayName());
        mailBody = mailBody.replaceAll("\\{\\{time\\}\\}", Integer.toString(sheet.getTargetHours()));

        sendEmail(emailTo, mailSubject, mailBody);
    }

    public void buildEmailAdministratorChangedEntry(String emailToAdministrator, String emailToUser, TimesheetEntry oldEntry, JsonTimesheetEntry newEntry) throws ServiceException {
        Config config = configService.getConfiguration();

        String oldEntryData = "Begin Date : " + oldEntry.getBeginDate() + "\n" +
                "End Date : " + oldEntry.getEndDate() + "\n" +
                "Pause [Minutes] : " + oldEntry.getPauseMinutes() + "\n" +
                "Team Name : " + oldEntry.getTeam().getTeamName() + "\n" +
                "Category Name : " + oldEntry.getCategory().getName() + "\n" +
                "Description : " + oldEntry.getDescription() + "\n";

        String newEntryData = "Begin Date : " + newEntry.getBeginDate() + "\n" +
                "End Date : " + newEntry.getEndDate() + "\n" +
                "Pause [Minutes] : " + newEntry.getPauseMinutes() + "\n" +
                // TODO: make this work without new Services
                //"Team Name : " + teamService.getTeamByID(newEntry.getTeamID()).getTeamName() + "\n" +
                //"Category Name : " + categoryService.getCategoryByID(newEntry.getCategoryID()).getName() + "\n" +
                "Description : " + newEntry.getDescription() + "\n";

        String mailSubject = config.getMailSubjectEntry() != null &&
                config.getMailSubjectEntry().length() != 0
                ? config.getMailSubjectEntry() : "[Timesheet - Timesheet Entry Changed Notification]";

        String mailBody = config.getMailBodyEntry() != null &&
                config.getMailBodyEntry().length() != 0
                ? config.getMailBodyEntry() : "'Timesheet - Timesheet' Entry Changed Information \n\n" +
                "Your Timesheet-Entry: \n" +
                oldEntryData +
                "\n was modyfied by an Administrator to \n" +
                newEntryData +
                "If you are not willing to accept those changes, please contact your 'Team-Coordinator', or an 'Administrator'.\n\n" +
                "Best regards,\n" +
                "Catrobat-Admins";

        mailBody = mailBody.replaceAll("\\{\\{original\\}\\}", oldEntryData);
        mailBody = mailBody.replaceAll("\\{\\{actual\\}\\}", newEntryData);

        //send Emails
        sendEmail(emailToAdministrator, mailSubject, mailBody);
        sendEmail(emailToUser, mailSubject, mailBody);
    }

    public void buildEmailAdministratorDeletedEntry(String emailToAdministrator, String emailToUser, TimesheetEntry entry) throws ServiceException {
        String mailSubject = "[Timesheet - Timesheet Entry Deleted Notification]";

        String mailBody = "'Timesheet - Timesheet' Entry Deleted Information \n\n" +
                "Your Timesheet-Entry: \n" +
                entry.getBeginDate() +
                entry.getEndDate() +
                entry.getPauseMinutes() +
                entry.getTeam().getTeamName() +
                entry.getCategory().getName() +
                entry.getDescription() +
                "\n was deleted by an Administrator.\n" +
                "If you are not willing to accept those changes, please contact your 'Team-Coordinator', or an 'Administrator'.\n\n" +
                "Best regards,\n" +
                "Catrobat-Admins";

        sendEmail(emailToAdministrator, mailSubject, mailBody);
        sendEmail(emailToUser, mailSubject, mailBody);
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
            mailBody = mailBody.replaceAll("\\{\\{date\\}\\}", sheet.getEntriesDesc()[0].getBeginDate().toString());
        }

        sendEmail(emailTo, mailSubject, mailBody);
    }

    public void sendEmail(String emailTo, String mailSubject, String mailBody) {
        logger.info("Email to: {} with subject: {} and content: {}", emailTo, mailSubject, mailBody);
        // TODO: reinsert code below, after verifying email behaviour
//        Email email = new Email(emailTo);
//        email.setSubject(mailSubject);
//        email.setBody(mailBody);
//
//        SingleMailQueueItem item = new SingleMailQueueItem(email);
//        ComponentAccessor.getMailQueue().addItem(item);
    }
}
