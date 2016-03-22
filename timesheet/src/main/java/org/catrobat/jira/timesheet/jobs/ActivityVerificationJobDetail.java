package org.catrobat.jira.timesheet.jobs;

import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.quartz.JobDetail;

/**
 * This class allows Spring dependencies to be injected into {@link ActivityVerificationJob}.
 * A bug in Confluence's auto-wiring prevents  Job components from being auto-wired.
 */
public class ActivityVerificationJobDetail extends JobDetail {
    private final TimesheetService ts;
    private final TimesheetEntryService te;

    public ActivityVerificationJobDetail(TimesheetService ts, TimesheetEntryService te) {
        super();
        this.ts = ts;
        this.te = te;

        setName(ActivityVerificationJobDetail.class.getSimpleName());
        setJobClass(ActivityVerificationJob.class);
    }

    public TimesheetService getTimesheetService() {
        return ts;
    }

    public TimesheetEntryService getTimesheetEntryService() {
        return te;
    }
}
