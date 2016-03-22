package org.catrobat.jira.timesheet.jobs;

import com.atlassian.crowd.embedded.api.User;

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
public class OutOfTimeVerificationJob implements Job {
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        OutOfTimeVerificationJobDetail jobDetail = (OutOfTimeVerificationJobDetail) jobExecutionContext.getJobDetail();
        List<Timesheet> timesheetList = jobDetail.getTimesheetService().all();
        Collection<User> userList = jobDetail.getComponentAccessor().getUserManager().getAllUsers();

        for (User user : userList) {
            for (Timesheet timesheet : timesheetList) {
                if (timesheet.getUserKey().equals(jobDetail.getComponentAccessor().getUserManager().
                        getUserByName(user.getName()).getKey().toString())) {
                    //if ((timesheet.getHours() - timesheet.getHoursDone) < 80) {
                    //MailQueueItem item = new ConfluenceMailQueueItem(emailTo, mailSubject, mailBody, MIME_TYPE_TEXT);
                    //jobDetail.getMailService().sendEmail(item);
                    //}
                }
            }
        }
        System.out.println("OutOfTimeVerificationJob: " + jobExecutionContext.getFireTime());
        System.out.println("OutOfTimeVerificationJob next: " + jobExecutionContext.getNextFireTime());
    }

    private boolean dateIsOlderThanTwoWeeks(Date date) {
        DateTime twoWeeksAgo = new DateTime().minusWeeks(2);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoWeeksAgo) < 0);
    }
}
