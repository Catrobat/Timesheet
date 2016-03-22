package org.catrobat.jira.timesheet.jobs;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.joda.time.DateTime;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Job for doing something on a regular basis.
 */
public class ActivityNotificationJob implements Job {
    private String emailTo = "";
    private String mailSubject = "";
    private String mailBody = "";

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ActivityNotificationJobDetail jobDetail = (ActivityNotificationJobDetail) jobExecutionContext.getJobDetail();
        List<Timesheet> timesheetList = jobDetail.getTimesheetService().all();
        Collection<User> userList = ComponentAccessor.getUserManager().getAllUsers();

        for (User user : userList) {
            for (Timesheet timesheet : timesheetList) {
                if (timesheet.getUserKey().equals(ComponentAccessor.getUserManager().
                        getUserByName(user.getName()).getKey())) {
                    if (!timesheet.getIsActive()) {
                        //email to admin + coordinators
                        //MailQueueItem item = new ConfluenceMailQueueItem(emailTo, mailSubject, mailBody, MIME_TYPE_TEXT);
                        //jobDetail.getMailService().sendEmail(item);
                        // https://answers.atlassian.com/questions/266166/jira-doesnt-show-plugin-job-on-scheduled-jobs-page
                    } else {
                        if (jobDetail.getTimesheetEntryService().getEntriesBySheet(timesheet).length > 0)
                            if (dateIsOlderThanTwoMonths(jobDetail.getTimesheetEntryService().getEntriesBySheet(timesheet)[0].getBeginDate())) {
                                //email to admin after 2 monts
                                //MailQueueItem item = new ConfluenceMailQueueItem(emailTo, mailSubject, mailBody, MIME_TYPE_TEXT);
                                //jobDetail.getMailService().sendEmail(item);
                            }
                    }
                }
            }
        }
        System.out.println("ActivityNotificationJob: " + jobExecutionContext.getFireTime());
        System.out.println("ActivityNotificationJob next: " + jobExecutionContext.getNextFireTime());
    }

    private boolean dateIsOlderThanTwoMonths(Date date) {
        DateTime twoMonthsAgo = new DateTime().minusMonths(2);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoMonthsAgo) < 0);
    }
}
