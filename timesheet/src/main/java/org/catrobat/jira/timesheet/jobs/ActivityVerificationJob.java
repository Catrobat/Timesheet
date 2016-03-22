package org.catrobat.jira.timesheet.jobs;
import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.service.AbstractService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Job for doing something on a regular basis.
 */
public class ActivityVerificationJob extends AbstractService {

    private  TimesheetService timesheetService;
    private TimesheetEntryService timesheetEntryService;
    public ActivityVerificationJob(TimesheetService timesheetService, TimesheetEntryService timesheetEntryService) {

        this.timesheetService = timesheetService;
        this.timesheetEntryService = timesheetEntryService;
    }

    //public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //ActivityVerificationJobDetail jobDetail = (ActivityVerificationJobDetail) jobExecutionContext.getJobDetail();
        //List<Timesheet> timesheetList = jobDetail.getTimesheetService().all();

    public void run() {
    List<Timesheet> timesheetList = timesheetService.all();

        System.out.println(timesheetList);

        for (Timesheet timesheet : timesheetList) {
            if (timesheet.getEntries().length > 0) {
                TimesheetEntry[] entries = timesheetEntryService.getEntriesBySheet(timesheet);
                if (dateIsOlderThanTwoWeeks(entries[0].getBeginDate())) {
                    timesheet.setIsActive(false);
                    timesheet.save();
                } else {
                    //latest entry is not older than 2 weeks
                    timesheet.setIsActive(true);
                    timesheet.save();
                }
            } else {
                //no entry available
                timesheet.setIsActive(false);
                timesheet.save();
            }
        }
    }

    private boolean dateIsOlderThanTwoWeeks(Date date) {
        DateTime twoWeeksAgo = new DateTime().minusWeeks(2);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoWeeksAgo) < 0);
    }

    public ObjectConfiguration getObjectConfiguration() throws ObjectConfigurationException {
        return this.getObjectConfiguration("MAILQUEUESERVICE", "services/com/atlassian/jira/service/services/mail/mailservice.xml", (Map)null);
    }
}
