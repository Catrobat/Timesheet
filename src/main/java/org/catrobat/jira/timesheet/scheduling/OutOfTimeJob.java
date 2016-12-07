package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.mail.Email;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.SchedulingService;
import org.catrobat.jira.timesheet.services.TimesheetService;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class OutOfTimeJob implements PluginJob {

    @Override
    public void execute(Map<String, Object> map) {
        System.out.println((new Date()).toString() + " OutOfTimeJob");

        TimesheetService sheetService = (TimesheetService)map.get("sheetService");
        ConfigService configService = (ConfigService)map.get("configService");
        SchedulingService schedulingService = (SchedulingService)map.get("schedulingService");

        List<Timesheet> timesheetList = sheetService.all();
        Config config = configService.getConfiguration();

        // TODO: notify coordinator
        for (Timesheet timesheet : timesheetList) {
            String userKey = timesheet.getUserKey();
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
            if ((timesheet.getTargetHours() - timesheet.getTargetHoursCompleted()) <= 80) {
                Email email = new Email(user.getEmailAddress());
                email.setSubject(config.getMailSubjectTime());
                email.setBody(config.getMailBodyTime());
                SingleMailQueueItem item = new SingleMailQueueItem(email);
                ComponentAccessor.getMailQueue().addItem(item);
            }
        }
    }
}
