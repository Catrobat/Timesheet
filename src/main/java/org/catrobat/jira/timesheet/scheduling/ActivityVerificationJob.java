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

            System.out.println("begin with print");
            for (TimesheetEntry entry : entries) {
                System.out.println("entry.getDescription() = " + entry.getDescription());
                System.out.println("entry.getBeginDate() = " + entry.getBeginDate());
            }


            // if the user is already reactivated, the coordinators should already be informed
            if (timesheet.getIsReactivated()) {
                timesheet.setIsReactivated(false);
            }

            Date latestEntryDate = entries[0].getBeginDate();
            TimesheetEntry latestInactiveEntry = getLatestInactiveEntry(timesheet);
            TimesheetEntry latestDeactivatedEntry = getLatestDeactivatedEntry(timesheet);
            if (latestDeactivatedEntry != null) {
                if (latestDeactivatedEntry.getDeactivateEndDate().compareTo(today) > 0) {// user has set himself to deactivated
                    timesheet.setIsActive(false);
                    timesheet.setIsAutoInactive(false);
                    timesheet.setIsOffline(true);
                    timesheet.setIsAutoOffline(false);
                    timesheet.save();
                    printStatusFlags(timesheet);
                    continue;
                }
            }
            if (latestInactiveEntry != null) {
                if (latestInactiveEntry.getInactiveEndDate().compareTo(today) > 0) { // user has set himself to inactive
                    timesheet.setIsActive(false);
                    timesheet.setIsAutoInactive(false);
                    timesheet.save();
                    printStatusFlags(timesheet);
                    continue;
                }
            }
            // user is active, but latest entry is older than the specified inactive limit
            if (timesheet.getIsActive() && schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                timesheet.setIsActive(false);
                timesheet.setIsAutoInactive(true);
                timesheet.save();
                Date begin = timesheet.getLatestEntryDate();
                if (begin == null) begin = new Date();
                Date end = new Date();

                Set<Team> teamsOfUser = teamService.getTeamsOfUser(timesheet.getUserKey());
                Team[] teamArray = new Team[teamsOfUser.size()];
                teamArray = teamsOfUser.toArray(teamArray);
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
            }
            // user is still inactive since the specified deactivated/offline limit
            else if (!timesheet.getIsActive() && timesheet.getIsAutoInactive() &&
                    schedulingService.isOlderThanOfflineTime(latestEntryDate)) {
                timesheet.setIsOffline(true);
                timesheet.setIsAutoOffline(true);
                timesheet.setIsAutoInactive(false);
                timesheet.save();

            }
            //user is back again
            else if (!timesheet.getIsActive() && !schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                timesheet.setIsActive(true);
                timesheet.setIsOffline(false);
                timesheet.setIsAutoInactive(false);
                timesheet.setIsAutoOffline(false);
                timesheet.setIsReactivated(true);
                timesheet.save();
            }
            // user has set himself inactive
            else if (!timesheet.getIsActive() && !timesheet.getIsAutoInactive()) {
                // user remains inactive, will be set to offline
                if (schedulingService.isOlderThanRemainingTime(latestEntryDate)) {
                    timesheet.setIsActive(false);
                    timesheet.setIsOffline(true);
                    timesheet.setIsAutoInactive(false);
                    timesheet.setIsAutoOffline(true);
                    timesheet.save();
                }
            }
            //default case: user is active
            else if (!schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                timesheet.setIsActive(true);
                timesheet.setIsOffline(false);
                timesheet.setIsAutoInactive(false);
                timesheet.setIsAutoOffline(false);
                timesheet.save();
            }

            //else: more possibilities with isActive: [false] isAutoInactive: [true]

            printStatusFlags(timesheet);
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

    private String printStatusFlags(Timesheet timesheet) {
        System.out.println("Status:     -----------------------------------------------------------------------");
        String message = "isActive: [" +
                timesheet.getIsActive() + "] isAutoInactive: [" + timesheet.getIsAutoInactive() + "] isOffline: [" +
                timesheet.getIsOffline() + "] isAutoOffline: [" + timesheet.getIsAutoOffline() + "] |";
        System.out.println(message);
        System.out.println("isReactivated: " + timesheet.getIsReactivated());
        System.out.println("END Status: -----------------------------------------------------------------------");
        return message;
    }
}
