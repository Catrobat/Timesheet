package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.service.ServiceException;
import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.SchedulingRest;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ActivityVerificationJob implements PluginJob {

    private TimesheetService sheetService;
    private TimesheetEntryService entryService;
    private TeamService teamService;
    private CategoryService categoryService;
    private SchedulingService schedulingService;
    private static final Logger logger = LoggerFactory.getLogger(ActivityVerificationJob.class);
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.LogManager.getLogger(ActivityVerificationJob.class);

    @Override
    public void execute(Map<String, Object> map) {
        LOGGER.error("ActivityVerificationJob triggered at: {}" + (new Date()).toString());

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
        LOGGER.error("Setting timesheet autoinactive of " + timesheet.getDisplayName());

        Date begin = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Date end = cal.getTime();

        Team team = entryService.getLatestEntry(timesheet).getTeam();
        if(team == null){
            LOGGER.error("we got no team to add the entry to!!");
            return;
        }

        try {
            LOGGER.error("adding auto inactive entry to team: " + team.getTeamName());

            entryService.add(
                    timesheet,
                    begin,
                    end,
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
            LOGGER.error("in autoinactive");
            LOGGER.error(e.getMessage());
        }
        timesheet.setState(Timesheet.State.AUTO_INACTIVE);
        timesheet.save();
    }

    private String printStatusFlags(Timesheet timesheet, String statusString) {
        String header = "\n Timesheet from: " + timesheet.getDisplayName() + "\n-------------------------------------\n";
        header += ("Transition: " + statusString) + "\n";
        String body = "state: " + timesheet.getState() + "\n";
        body += "latest Entry: " + timesheet.getLatestEntryBeginDate().toString() + "\n";
        body += "END Status \n-------------------------------------" + "\n" ;
        String message = header + body + "\n";
        LOGGER.error(message);

        return message;
    }
}
