package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.SchedulingService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.utility.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class OutOfTimeJob implements PluginJob {

    private static final Logger logger = LoggerFactory.getLogger(ActivityVerificationJob.class);

    @Override
    public void execute(Map<String, Object> map) {
        logger.info("OutOfTimeJob triggered at: {}", (new Date()).toString());

        TimesheetService sheetService = (TimesheetService)map.get("sheetService");
        ConfigService configService = (ConfigService)map.get("configService");
        SchedulingService schedulingService = (SchedulingService)map.get("schedulingService");
        EmailUtil emailUtil = new EmailUtil(configService);

        List<Timesheet> timesheetList = sheetService.all();
        Config config = configService.getConfiguration();

        // TODO: notify coordinator
        for (Timesheet timesheet : timesheetList) {
            String userKey = timesheet.getUserKey();
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
            if ((timesheet.getTargetHours() - timesheet.getHoursCompleted()) <= schedulingService.getScheduling().getOutOfTime()) {
                emailUtil.sendEmail(user.getEmailAddress(), config.getMailSubjectTime(), config.getMailBodyTime());
            }
        }
    }
}
