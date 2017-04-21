package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.service.ServiceException;
import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;

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

    @Override
    public void execute(Map<String, Object> map) {
        System.out.println((new Date()).toString() + " ActivityVerificationJob");

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

            String statusFlagMessage;

            Date latestEntryDate = timesheet.getLatestEntryBeginDate();
            TimesheetEntry latestInactiveEntry = getLatestInactiveEntry(timesheet);
            if (latestInactiveEntry != null && latestInactiveEntry.getInactiveEndDate().compareTo(today) > 0) {
                timesheet.setState(Timesheet.State.INACTIVE); // FIXME: should already be set
                timesheet.save();
                statusFlagMessage = "user has set himself to inactive";
            }
            else if (timesheet.getState() == Timesheet.State.ACTIVE && schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                setAutoInactive(timesheet);
                statusFlagMessage = "user is active, but latest entry is older than the specified inactive limit";
            }
            else if (timesheet.getState() == Timesheet.State.AUTO_INACTIVE &&
                    schedulingService.isOlderThanOfflineTime(latestEntryDate)) {
                timesheet.setState(Timesheet.State.DISABLED);
                timesheet.save();
                statusFlagMessage = "user is still inactive since the specified disabled limit";
            }
            else if (timesheet.getState() != Timesheet.State.ACTIVE && !schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                timesheet.setState(Timesheet.State.ACTIVE);
                timesheet.save();
                statusFlagMessage = "user is back again";
            }
            // user has set himself inactive
            else if (timesheet.getState() == Timesheet.State.INACTIVE && schedulingService.isOlderThanRemainingTime(latestEntryDate)) {
                timesheet.setState(Timesheet.State.AUTO_INACTIVE);
                timesheet.save();
                statusFlagMessage = "user remains inactive, will be set to auto inactive";
            }
            else if (!schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                timesheet.setState(Timesheet.State.ACTIVE);
                timesheet.save();
                statusFlagMessage = "default case: user is active";
            }
            else {
                statusFlagMessage = "nothing changed";
            }
            printStatusFlags(timesheet, statusFlagMessage);
        }
    }

    private void setAutoInactive(Timesheet timesheet) {
        timesheet.setState(Timesheet.State.AUTO_INACTIVE);
        timesheet.save();
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
    }

    private TimesheetEntry getLatestInactiveEntry(Timesheet timesheet) {
        TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);
        for (TimesheetEntry entry : entries) {
            if (entry.getCategory().getName().equals(SpecialCategories.INACTIVE)
                    && (entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0)) {
                return entry;
            }
        }
        return null;
    }

    private String printStatusFlags(Timesheet timesheet, String statusString) {
        String header = "Timesheet from: " + timesheet.getDisplayName() + "-------------------------------------\n";
        header += "Transition: " + statusString;
        System.out.println(header);
        String message = "state: " + timesheet.getState();
        System.out.println(message);
        System.out.println("latest Entry: " + timesheet.getLatestEntryBeginDate().toString());
        System.out.println("END Status: -------------------------------------");
        return message;
    }
}
