package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.utility.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ActivityNotificationJob implements PluginJob {

    private TimesheetService sheetService;
    private TimesheetEntryService entryService;
    private TeamService teamService;
    private ConfigService configService;
    private SchedulingService schedulingService;
    private EmailUtil emailUtil;
    private static final Logger logger = LoggerFactory.getLogger(ActivityNotificationJob.class);

    @Override
    public void execute(Map<String, Object> map) {
        logger.info("ActivityNotificationJob triggered at: {}", (new Date()).toString());

        sheetService = (TimesheetService) map.get("sheetService");
        entryService = (TimesheetEntryService) map.get("entryService");
        teamService = (TeamService) map.get("teamService");
        configService = (ConfigService) map.get("configService");
        schedulingService = (SchedulingService) map.get("schedulingService");
        emailUtil = new EmailUtil(configService);

        List<Timesheet> timesheetList = sheetService.all();
        Config config = configService.getConfiguration();
        for (Timesheet timesheet : timesheetList) {
            String userKey = timesheet.getUserKey();
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
            
            if (timesheet.getEntries().length == 0) { // nothing to do
                continue;
            }
            if (timesheet.getState() == Timesheet.State.INACTIVE_OFFLINE) {  // user is offline
                informCoordinatorsOffline(user, config);
                informTimesheetAdminsOffline(config);
            } else if (timesheet.getState() == Timesheet.State.INACTIVE) { // user is inactive
                // FIXME: check is not needed if state is correct
                TimesheetEntry latestInactiveEntry = entryService.getLatestInactiveEntry(timesheet);
                if (latestInactiveEntry != null) {
                    if (schedulingService.isOlderThanInactiveTime(latestInactiveEntry.getInactiveEndDate())) {
                        informCoordinatorsInactive(user, config);
                    }
                }
            }
        }
    }

    private void informCoordinatorsInactive(ApplicationUser user, Config config) {
        for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
            emailUtil.sendEmail(coordinatorMailAddress, config.getMailSubjectInactiveState(),
                    config.getMailBodyInactiveState());
        }
    }

    private void informCoordinatorsOffline(ApplicationUser user, Config config) {
        for (String coordinatorMailAddress : getCoordinatorsMailAddress(user)) {
            emailUtil.sendEmail(coordinatorMailAddress, config.getMailSubjectOfflineState(),
                    config.getMailBodyOfflineState());
        }
    }

    private void informTimesheetAdminsOffline(Config config) {
        TimesheetAdmin[] timesheetAdmins = config.getTimesheetAdminUsers();
        for (TimesheetAdmin timesheetAdmin : timesheetAdmins) {
            emailUtil.sendEmail(timesheetAdmin.getEmailAddress(), config.getMailSubjectOfflineState(),
                    config.getMailBodyOfflineState());
        }
    }

    private List<String> getCoordinatorsMailAddress(ApplicationUser user) {
        List<String> coordinatorMailAddressList = new LinkedList<>();
        for (Team team : teamService.getTeamsOfUser(user.getName())) {
            for (String coordinator : teamService.getGroupsForRole(team.getTeamName(), TeamToGroup.Role.COORDINATOR))
                coordinatorMailAddressList.add(ComponentAccessor.getUserManager().getUserByName(coordinator).getEmailAddress());
        }

        return coordinatorMailAddressList;
    }
}
