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
import org.ofbiz.core.entity.jdbc.SQLProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class TimesheetEntryServiceImpl implements TimesheetEntryService {

    private final ActiveObjects ao;
    private final TimesheetService timesheetService;

    public TimesheetEntryServiceImpl(ActiveObjects ao, TimesheetService timesheetService) {
        this.ao = ao;
        this.timesheetService = timesheetService;
    }

    @Override
    public TimesheetEntry add(Timesheet sheet, Date begin, Date end, Category category, String description, int pause,
            Team team, boolean isGoogleDocImport, Date inactiveEndDate, String jiraTicketID,
            String userName, boolean teamroom) throws ServiceException {

        if (ao.find(TimesheetEntry.class, "TIME_SHEET_ID = ? AND BEGIN_DATE < ? AND END_DATE > ?", sheet.getID(), end, begin).length != 0) {
            throw new ServiceException("TimesheetEntries are not allowed to overlap.");
        }

        TimesheetEntry entry = ao.create(TimesheetEntry.class);
        return setTimesheetEntryData(sheet, begin, end, category, description, pause, team, isGoogleDocImport, 
        		inactiveEndDate, jiraTicketID, userName, teamroom, entry);
    }

    private TimesheetEntry setTimesheetEntryData(Timesheet sheet, Date begin, Date end, Category category, String description,
            int pause, Team team, boolean isGoogleDocImport, Date inactiveEndDate, String jiraTicketID, String userName,
            boolean teamroom, TimesheetEntry entry) throws ServiceException {

        checkParams(begin, end, category, description, team, jiraTicketID, userName);

        entry.setTimeSheet(sheet);
        entry.setBeginDate(begin);
        entry.setEndDate(end);
        entry.setCategory(category);
        entry.setDescription(description);
        entry.setPauseMinutes(pause);
        entry.setTeam(team);
        entry.setIsGoogleDocImport(isGoogleDocImport);
        entry.setTeamroom(teamroom);
        entry.setInactiveEndDate(inactiveEndDate);
        entry.setJiraTicketID(jiraTicketID);
        entry.setPairProgrammingUserName(userName);
        entry.save();
        updateTimesheet(sheet, entry);
        return entry;
    }

    private void checkParams(Date begin, Date end, Category category, String description,
            Team team, String jiraTicketID,
            String userName) throws ServiceException {
        if (team == null) {
            throw new ServiceException("TimesheetEntry is not allowed with null Team.");
        }
        if (category == null) {
            throw new ServiceException("TimesheetEntry is not allowed with null Category.");
        }
        if (description.length() > 255) {
            throw new ServiceException("Description shall not be longer than 255 characters.");
        }
        if (jiraTicketID.length() > 255) {
            throw new ServiceException("JiraTicketID shall not be longer than 255 characters.");
        }
        if (userName.length() > 255) {
            throw new ServiceException("Pair Programming User Name shall not be longer than 255 characters.");
        }
        if (begin.compareTo(end) > 0) {
            throw new ServiceException("Begin Date must be before End Date.");
        }
    }

    private void updateTimesheet(Timesheet sheet, TimesheetEntry entry) throws ServiceException {
        int completedHours = getHoursOfTimesheet(sheet);
        int completedPracticeHours = getPracticeHoursOfTimesheet(sheet);
        Date latestEntryDate = getLatestEntry(sheet).getBeginDate();

        Timesheet.State state = sheet.getState();
        if ((entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0)) {
            if (entry.getCategory().getName().equals(SpecialCategories.INACTIVE)) {
                state = Timesheet.State.INACTIVE;
            } else if (entry.getCategory().getName().equals(SpecialCategories.INACTIVE_OFFLINE)) {
                state = Timesheet.State.INACTIVE_OFFLINE;
            } else {
                throw new ServiceException("Inactive Date set, without selecting an Inactive Category.");
            }
        }

        if (sheet.getState() == Timesheet.State.AUTO_INACTIVE) {
            state = Timesheet.State.ACTIVE;
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
        return minutes / 60;
    }

    private int getPracticeHoursOfTimesheet(Timesheet sheet) {
        if (sheet == null)
            return 0;
        TimesheetEntry[] entries = ao.find(TimesheetEntry.class, "TIME_SHEET_ID = ?", sheet.getID());
        int minutes = 0;
        for (TimesheetEntry entry : entries) {
            minutes += entry.getDurationMinutes();            
        }
        return minutes / 60;
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
            Date inactiveEndDate, String jiraTicketID, String userName, boolean teamroom) throws ServiceException {

        TimesheetEntry entry = getEntryByID(entryId);

        if (entry == null) {
            throw new ServiceException("Entry not found");
        }
        return setTimesheetEntryData(sheet, begin, end, category, description, pause, team, isGoogleDocImport, 
        		inactiveEndDate, jiraTicketID, userName, teamroom, entry);
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
        ZonedDateTime xMonthsAgo = ZonedDateTime.now().minusMonths(months);
        int minutes = 0;
        for (TimesheetEntry entry : getEntriesBySheet(sheet)) {
            Instant instant = entry.getBeginDate().toInstant();
            ZonedDateTime beginDate = instant.atZone(ZoneId.systemDefault());
            if (beginDate.isAfter(xMonthsAgo)) {
                minutes += entry.getDurationMinutes();
            }
        }
        return minutes / 60;
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
            String categoryName = entry.getCategory().getName();
            if ((categoryName.equals(SpecialCategories.INACTIVE) || categoryName.equals(SpecialCategories.INACTIVE_OFFLINE))
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
