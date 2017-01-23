package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.*;

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

            Date latestEntryDate = entries[0].getBeginDate();
            TimesheetEntry latestInactiveEntry = getLatestInactiveEntry(timesheet);
            TimesheetEntry latestDeactivatedEntry = getLatestDeactivatedEntry(timesheet);
            if (latestDeactivatedEntry != null) {
                if (latestDeactivatedEntry.getDeactivateEndDate().compareTo(today) > 0) {
                    timesheet.setState(Timesheet.State.INACTIVE_OFFLINE);
                    timesheet.save();
                    printStatusFlags(timesheet, "user has set himself to deactivated");
                    continue;
                }
            }
            if (latestInactiveEntry != null) {
                if (latestInactiveEntry.getInactiveEndDate().compareTo(today) > 0) {
                    timesheet.setState(Timesheet.State.INACTIVE);
                    timesheet.save();
                    printStatusFlags(timesheet, "user has set himself to inactive");
                    continue;
                }
            }
            if (timesheet.getState() == Timesheet.State.ACTIVE && schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
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
                        end,
                        "",
                        ""
                );
                printStatusFlags(timesheet, "user is active, but latest entry is older than the specified inactive limit");
            }
            /*else if (!timesheet.getIsActive() && timesheet.getIsAutoInactive() &&
                    schedulingService.isOlderThanOfflineTime(latestEntryDate)) {
                timesheet.setIsOffline(true);
                timesheet.save();
                printStatusFlags(timesheet, "user is still inactive since the specified deactivated/offline limit");
            }*/
            else if (timesheet.getState() != Timesheet.State.ACTIVE && !schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                timesheet.setState(Timesheet.State.ACTIVE);
                timesheet.save();
                printStatusFlags(timesheet, "user is back again");
            }
            // user has set himself inactive
            /*else if (!timesheet.getIsActive() && !timesheet.getIsAutoInactive()) {
                if (schedulingService.isOlderThanRemainingTime(latestEntryDate)) {
                    timesheet.setIsActive(false);
                    timesheet.setIsOffline(true);
                    timesheet.setIsAutoInactive(false);
                    timesheet.save();
                    printStatusFlags(timesheet, "user remains inactive, will be set to offline");
                }
            }*/
            else if (!schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                timesheet.setState(Timesheet.State.ACTIVE);
                timesheet.save();
                printStatusFlags(timesheet, "default case: user is active");
            }
            else {
                //more possibilities with isActive: [false] isAutoInactive: [true]
                printStatusFlags(timesheet, "nothing changed");
            }
        }
    }

    private TimesheetEntry getLatestDeactivatedEntry(Timesheet timesheet) {
        TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);
        for (TimesheetEntry entry : entries) {
            if (entry.getCategory().getName().equals("Deactivated")
                    && (entry.getDeactivateEndDate().compareTo(entry.getBeginDate()) > 0)) {
                return entry;
            }
        }
        return null;
    }

    private TimesheetEntry getLatestInactiveEntry(Timesheet timesheet) {
        TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);
        for (TimesheetEntry entry : entries) {
            if (entry.getCategory().getName().equals("Inactive")
                    && (entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0)) {
                return entry;
            }
        }
        return null;
    }

    private String printStatusFlags(Timesheet timesheet, String statusString) {
        String header = "Status: " + statusString + "  -----------------------------------------------------------------------";
        System.out.println(header);
        String message = "state: " + timesheet.getState();
        System.out.println(message);
        System.out.println("latest Entry: " + timesheet.getLatestEntryBeginDate().toString());
        System.out.println("END Status: -----------------------------------------------------------------------");
        return message;
    }
}
