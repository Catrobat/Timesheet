package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.sal.api.scheduling.PluginJob;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.*;
import org.joda.time.DateTime;

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

        sheetService = (TimesheetService)map.get("sheetService");
        entryService = (TimesheetEntryService)map.get("entryService");
        teamService = (TeamService)map.get("teamService");
        categoryService = (CategoryService)map.get("categoryService");
        schedulingService = (SchedulingService)map.get("schedulingService");

        List<Timesheet> timesheetList = sheetService.all();
        for (Timesheet timesheet : timesheetList) {
            TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);
            if (entries.length == 0) {
                continue;
            }
            TimesheetEntry latestInactiveEntry = getLatestInactiveEntry(timesheet);
            if (latestInactiveEntry != null) {
                if (latestInactiveEntry.getInactiveEndDate().compareTo(today) > 0) { // user has set himself to inactive
                    timesheet.setIsActive(false);
                    timesheet.setIsAutoInactive(false);
                    timesheet.save();
                    printStatusFlags(timesheet);
                    continue;
                }
            }
            // user is active, but latest entry is older than 2 weeks
            if (timesheet.getIsActive() && schedulingService.isOlderThanInactiveTime(entries[0].getBeginDate())) {
                timesheet.setIsActive(false);
                timesheet.setIsAutoInactive(true);
                timesheet.save();
                Date begin = timesheet.getLatestEntryDate();
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
            // user is still inactive since 2 months
            else if (!timesheet.getIsActive() && timesheet.getIsAutoInactive() && isDateOlderThanTwoMonths(entries[0].getBeginDate())) {
                timesheet.setIsOffline(true);
                timesheet.setIsAutoOffline(true);
                timesheet.setIsAutoInactive(false);
                timesheet.save();

            }
            //user is back again
            else if (!schedulingService.isOlderThanInactiveTime(entries[0].getBeginDate())) {
                timesheet.setIsActive(true);
                timesheet.setIsOffline(false);
                timesheet.setIsAutoInactive(false);
                timesheet.setIsAutoOffline(false);
                timesheet.save();
            }
            // user has set himself inactive
            else if (!timesheet.getIsActive() && !timesheet.getIsAutoInactive()) {
                // user remains inactive, will be set to offline
                if (isDateOlderThanOneWeek(entries[0].getBeginDate())) {
                    timesheet.setIsActive(false);
                    timesheet.setIsOffline(true);
                    timesheet.setIsAutoInactive(false);
                    timesheet.setIsAutoOffline(true);
                    timesheet.save();
                }
            }

            //else: more possibilities with isActive: [false] isAutoInactive: [true]

            printStatusFlags(timesheet);
        }
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

    private boolean isDateOlderThanTwoMonths(Date date) {
        DateTime twoMonthsAgo = new DateTime().minusMonths(2);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoMonthsAgo) < 0);
    }

    private boolean isDateOlderThanOneWeek(Date date) {
        DateTime oneWeekAgo = new DateTime().minusWeeks(1);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(oneWeekAgo) < 0);
    }

    private String printStatusFlags(Timesheet timesheet) {
        System.out.println("Status:     -----------------------------------------------------------------------");
        String message = "isActive: [" +
                timesheet.getIsActive() + "] isAutoInactive: [" + timesheet.getIsAutoInactive() + "] isOffline: [" +
                timesheet.getIsOffline() + "] isAutoOffline: [" + timesheet.getIsAutoOffline() + "] |";
        System.out.println(message);
        System.out.println("END Status: -----------------------------------------------------------------------");
        return message;
    }
}
