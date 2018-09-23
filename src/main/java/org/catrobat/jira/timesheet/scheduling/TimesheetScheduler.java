package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TimesheetScheduler implements LifecycleAware {

	private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.LogManager.getLogger(TimesheetScheduler.class);
	
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
    private final PermissionService permissionService;

    public TimesheetScheduler(PluginScheduler pluginScheduler, TimesheetService sheetService,
            TimesheetEntryService entryService, TeamService teamService, CategoryService categoryService,
            ConfigService configService, SchedulingService schedulingService, PermissionService permissionService) {
        this.pluginScheduler = pluginScheduler;
        this.sheetService = sheetService;
        this.entryService = entryService;
        this.teamService = teamService;
        this.categoryService = categoryService;
        this.configService = configService;
        this.schedulingService = schedulingService;
        this.permissionService = permissionService;
    }

    public void reschedule() {
        Map<String, Object> params = new HashMap<>();
        params.put("sheetService", sheetService);
        params.put("entryService", entryService);
        params.put("teamService", teamService);
        params.put("categoryService", categoryService);
        params.put("configService", configService);
        params.put("schedulingService", schedulingService);
        params.put("permissionService", permissionService);

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
        LocalDateTime start = LocalDate.now().atStartOfDay().plusDays(1);
        return Date.from(start.atZone(ZoneId.systemDefault()).toInstant()); //TODO: make it during night
    }

    private long jobInterval() {
    	
        return 1000*60*60; // Every hour
//        return 1000*60*60*24; // Everyday
    }

    @Override
    public void onStart() {
        this.reschedule();
    }
}
