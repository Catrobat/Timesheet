package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.service.ServiceException;
import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivityVerificationJob implements PluginJob {

    private TimesheetService sheetService;
    private TimesheetEntryService entryService;
    private TeamService teamService;
    private CategoryService categoryService;
    private SchedulingService schedulingService;
    private static final Logger logger = LoggerFactory.getLogger(ActivityVerificationJob.class);

    @Override
    public void execute(Map<String, Object> map) {
        logger.info("ActivityVerificationJob triggered at: {}", (new Date()).toString());

        Date today = new Date();

        sheetService = (TimesheetService) map.get("sheetService");
        entryService = (TimesheetEntryService) map.get("entryService");
        teamService = (TeamService) map.get("teamService");
        categoryService = (CategoryService) map.get("categoryService");
        schedulingService = (SchedulingService) map.get("schedulingService");

        List<Timesheet> timesheetList = sheetService.all();
        for (Timesheet timesheet : timesheetList) {
            TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);
            if (entries.length == 0) {
                continue;
            }

            String statusFlagMessage = "nothing changed";

            Date latestEntryDate = timesheet.getLatestEntryBeginDate();
            TimesheetEntry latestInactiveEntry = entryService.getLatestInactiveEntry(timesheet);
            Timesheet.State state = timesheet.getState();
            if (state == Timesheet.State.ACTIVE && schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                setAutoInactive(timesheet);
                statusFlagMessage = "user is active, but latest entry is older than the specified inactive limit";
            }
            else if (state == Timesheet.State.AUTO_INACTIVE &&
                    schedulingService.isOlderThanOfflineTime(latestEntryDate)) {
                timesheet.setState(Timesheet.State.DISABLED);
                timesheet.save();
                statusFlagMessage = "user is still inactive since the specified disabled limit";
            }
            else if (isManualInactiveState(state) && latestInactiveEntry != null &&
                    latestInactiveEntry.getInactiveEndDate().compareTo(today) < 0) {
                timesheet.setState(Timesheet.State.ACTIVE);
                timesheet.save();
                statusFlagMessage = "user is back again";
            }
            printStatusFlags(timesheet, statusFlagMessage);
        }
    }

    private boolean isManualInactiveState(Timesheet.State state) {
        return state == Timesheet.State.INACTIVE || state == Timesheet.State.INACTIVE_OFFLINE;
    }

    private void setAutoInactive(Timesheet timesheet) {
        Date begin = timesheet.getLatestEntryBeginDate();
        if (begin == null) begin = new Date();
        Date end = new Date();

        Set<Team> teamsOfUser = teamService.getTeamsOfUser(timesheet.getUserKey());
        Team[] teamArray = new Team[teamsOfUser.size()];
        teamArray = teamsOfUser.toArray(teamArray);
        if(teamArray.length == 0) return; // very rare case
        Team team = teamArray[0];

        try {
            entryService.add(
                    timesheet,
                    begin,
                    begin,
                    categoryService.getCategoryByName("Inactive"),
                    "Auto generated inactivity entry",
                    0,
                    team,
                    false,
                    end,
                    "",
                    ""
            );
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        timesheet.setState(Timesheet.State.AUTO_INACTIVE);
        timesheet.save();
    }

    private String printStatusFlags(Timesheet timesheet, String statusString) {
        String header = "Timesheet from: " + timesheet.getDisplayName() + "-------------------------------------\n";
        header += "Transition: " + statusString;
        String body = "state: " + timesheet.getState();
        body += "latest Entry: " + timesheet.getLatestEntryBeginDate().toString();
        body += "END Status -------------------------------------";
        String message = header + body;
        logger.debug(message);

        return message;
    }
}
