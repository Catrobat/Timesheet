package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Scheduling;
import org.catrobat.jira.timesheet.services.*;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TimesheetScheduler implements LifecycleAware {

    private final PluginScheduler pluginScheduler;
    private static final String ACTIVITY_VERIFICATION_JOB = TimesheetScheduler.class.getName() + "ActivityVerificationJob";
    private static final String ACTIVITY_NOTIFICATION_JOB = TimesheetScheduler.class.getName() + "ActivityNotificationJob";
    private static final String OUT_OF_TIME_JOB = TimesheetScheduler.class.getName() + "OutOfTimeJob";

    private final TimesheetService sheetService;
    private final TimesheetEntryService entryService;
    private final TeamService teamService;
    private final CategoryService categoryService;
    private final ConfigService configService;
    private final SchedulingService schedulingService;

    public TimesheetScheduler(PluginScheduler pluginScheduler, TimesheetService sheetService,
            TimesheetEntryService entryService, TeamService teamService, CategoryService categoryService,
            ConfigService configService, SchedulingService schedulingService, ActiveObjects ao) {
        this.pluginScheduler = pluginScheduler;
        this.sheetService = sheetService;
        this.entryService = entryService;
        this.teamService = teamService;
        this.categoryService = categoryService;
        this.configService = configService;
        this.schedulingService = schedulingService;
    }

    public void reschedule() {
        Map<String, Object> params = new HashMap<>();
        params.put("sheetService", sheetService);
        params.put("entryService", entryService);
        params.put("teamService", teamService);
        params.put("categoryService", categoryService);
        params.put("configService", configService);
        params.put("schedulingService", schedulingService);

        pluginScheduler.scheduleJob(
                ACTIVITY_VERIFICATION_JOB,
                ActivityVerificationJob.class,
                params,
                jobStartDate(),
                jobInterval()
        );
        pluginScheduler.scheduleJob(
                ACTIVITY_NOTIFICATION_JOB,
                ActivityVerificationJob.class,
                params,
                jobStartDate(),
                jobInterval()
        );
        pluginScheduler.scheduleJob(
                OUT_OF_TIME_JOB,
                OutOfTimeJob.class,
                params,
                jobStartDate(),
                jobInterval()
        );
    }

    private Date jobStartDate() {
        DateTime start = new DateTime().withTimeAtStartOfDay().plusDays(1);
        return start.toDate(); //TODO: make it during night
    }

    private long jobInterval() {
        //return 1000*60*60*24; // Everyday
        return 1000*120; // Testing
    }

    @Override
    public void onStart() {
        this.reschedule();
    }
}
