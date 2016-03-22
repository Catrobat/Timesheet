package org.catrobat.jira.timesheet.jobs;

import com.atlassian.jira.component.ComponentAccessor;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.quartz.JobDetail;

/**
 * This class allows Spring dependencies to be injected into {@link ActivityVerificationJob}.
 * A bug in Confluence's auto-wiring prevents  Job components from being auto-wired.
 */
public class OutOfTimeVerificationJobDetail extends JobDetail {
    private final TimesheetService ts;
    private final ComponentAccessor ca;

    public OutOfTimeVerificationJobDetail(TimesheetService ts, ComponentAccessor ca) {
        super();
        this.ts = ts;
        this.ca = ca;

        setName(OutOfTimeVerificationJobDetail.class.getSimpleName());
        setJobClass(OutOfTimeVerificationJob.class);
    }

    public ComponentAccessor getComponentAccessor() {
        return ca;
    }

    public TimesheetService getTimesheetService() {
        return ts;
    }
}
