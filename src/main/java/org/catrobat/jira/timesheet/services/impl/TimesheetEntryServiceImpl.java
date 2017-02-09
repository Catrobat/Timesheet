/*
 * Copyright 2016 Adrian Schnedlitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.service.ServiceException;
import net.java.ao.Query;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Date;

public class TimesheetEntryServiceImpl implements TimesheetEntryService {

    private final ActiveObjects ao;
    private final TimesheetService timesheetService;

    public TimesheetEntryServiceImpl(ActiveObjects ao, TimesheetService timesheetService) {
        this.ao = ao;
        this.timesheetService = timesheetService;
    }

    @Override
    public TimesheetEntry add(Timesheet sheet, Date begin, Date end, Category category, String description, int pause,
            Team team, boolean isGoogleDocImport, Date inactiveEndDate, Date deactivateEndDate, String jiraTicketID,
            String userName) {

        TimesheetEntry entry = ao.create(TimesheetEntry.class);

        entry.setTimeSheet(sheet);
        entry.setBeginDate(begin);
        entry.setEndDate(end);
        entry.setCategory(category);
        entry.setDescription(description);
        entry.setPauseMinutes(pause);
        entry.setTeam(team);
        entry.setIsGoogleDocImport(isGoogleDocImport);
        entry.setInactiveEndDate(inactiveEndDate);
        entry.setDeactivateEndDate(deactivateEndDate);
        entry.setJiraTicketID(jiraTicketID);
        entry.setPairProgrammingUserName(userName);

        entry.save();

        updateTimesheet(sheet, entry);

        return entry;
    }

    private void updateTimesheet(Timesheet sheet, TimesheetEntry entry) {
        int completedHours = getHoursOfTimesheet(sheet);
        int completedPracticeHours = getPracticeHoursOfTimesheet(sheet);
        Date latestEntryDate = getLatestEntry(sheet).getBeginDate();

        Timesheet.State state = sheet.getState();
        if ((entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0)) {
            state = Timesheet.State.INACTIVE;
        } else if ((entry.getDeactivateEndDate().compareTo(entry.getBeginDate()) > 0)) {
            state = Timesheet.State.INACTIVE_OFFLINE;
        }

        timesheetService.updateTimesheet(sheet.getID(), completedHours, completedPracticeHours, latestEntryDate, state);
    }

    private int getHoursOfTimesheet(Timesheet sheet) {
        if (sheet == null)
            return 0;
        TimesheetEntry[] entries = ao.find(TimesheetEntry.class, "TIME_SHEET_ID = ?", sheet.getID());
        int minutes = 0;
        for (TimesheetEntry entry : entries) {
            minutes += entry.getDurationMinutes();
        }
        return minutes/60;
    }

    private int getPracticeHoursOfTimesheet(Timesheet sheet) {
        if (sheet == null)
            return 0;
        TimesheetEntry[] entries = ao.find(TimesheetEntry.class, "TIME_SHEET_ID = ?", sheet.getID());
        int minutes = 0;
        for (TimesheetEntry entry : entries) {
            if (!entry.getIsTheory()) {
                minutes += entry.getDurationMinutes();
            }
        }
        return minutes/60;
    }

    @Override
    @Nullable
    public TimesheetEntry getEntryByID(int entryID) {
        return ao.get(TimesheetEntry.class, entryID);
    }

    @Override
    @Nullable
    public TimesheetEntry edit(int entryId, Timesheet sheet, Date begin, Date end, Category category,
            String description, int pause, Team team, boolean isGoogleDocImport,
            Date inactiveEndDate, Date deactivatedEndDate, String userName, String jiraTicketID) throws ServiceException {

        TimesheetEntry entry = getEntryByID(entryId);

        if (entry == null) {
            throw new ServiceException("Entry not found");
        }

        entry.setTimeSheet(sheet);
        entry.setBeginDate(begin);
        entry.setEndDate(end);
        entry.setCategory(category);
        entry.setDescription(description);
        entry.setPauseMinutes(pause);
        entry.setTeam(team);
        entry.setIsGoogleDocImport(isGoogleDocImport);
        entry.setInactiveEndDate(inactiveEndDate);
        entry.setDeactivateEndDate(deactivatedEndDate);
        entry.setJiraTicketID(jiraTicketID);
        entry.setPairProgrammingUserName(userName);

        entry.save();

        updateTimesheet(sheet, entry);

        return entry;
    }

    @Override
    public TimesheetEntry[] getEntriesBySheet(Timesheet sheet) {
        if (sheet == null) return new TimesheetEntry[0];
        return ao.find(
                TimesheetEntry.class,
                Query.select()
                        .where("TIME_SHEET_ID = ?", sheet.getID())
                        .order("BEGIN_DATE DESC")
        );
    }

    @Override
    public void delete(TimesheetEntry entry) {
        ao.delete(entry);
    }


    @Override
    public int getHoursOfLastXMonths(Timesheet sheet, int months) {
        DateTime now = DateTime.now();
        int minutes = 0;
        for (TimesheetEntry entry : getEntriesBySheet(sheet)) {
            DateTime beginDate = new DateTime(entry.getBeginDate());
            if (beginDate.plusMonths(months).compareTo(now) >= 0) {
                minutes += entry.getDurationMinutes();
            }
        }
        return minutes/60;
    }

    @Override
    public TimesheetEntry getLatestEntry(Timesheet timesheet) {
        TimesheetEntry[] entries = this.getEntriesBySheet(timesheet);
        if (entries.length == 0) {
            return null;
        }
        return entries[0];
    }

    @Override
    public TimesheetEntry getLatestInactiveEntry(Timesheet timesheet) {
        TimesheetEntry[] entries = this.getEntriesBySheet(timesheet);
        for (TimesheetEntry entry : entries) {
            if (entry.getCategory().getName().equals(SpecialCategories.INACTIVE)
                    && (entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public void replaceTeamInEntries(Team oldTeam, Team newTeam) {
        for (TimesheetEntry entry : ao.find(TimesheetEntry.class)) {
            if (entry.getTeam() != null && entry.getTeam().equals(oldTeam)) {
                entry.setTeam(newTeam);
                entry.save();
            }
        }
    }
}
