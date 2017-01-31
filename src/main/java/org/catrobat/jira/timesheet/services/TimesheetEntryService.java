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

package org.catrobat.jira.timesheet.services;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.service.ServiceException;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;

import java.util.Date;

@Transactional
public interface TimesheetEntryService {
    TimesheetEntry add(Timesheet sheet, Date begin, Date end, Category category, String description, int pause,
            Team team, boolean isGoogleDocImport, Date inactiveEndDate, Date deactivateEndDate, String jiraTicketID, String userName);

    TimesheetEntry edit(int entryID, Timesheet sheet, Date begin, Date end, Category category, String description,
            int pause, Team team, boolean isGoogleDocImport, Date inactiveEndDate, Date deactivatedEndDate, String userName, String jiraTicketID) throws ServiceException;

    TimesheetEntry getEntryByID(int entryID) throws ServiceException;

    TimesheetEntry[] getEntriesBySheet(Timesheet sheet);

    void delete(TimesheetEntry entry);

    int getHoursOfLastXMonths(Timesheet sheet, int months);

    TimesheetEntry getLatestEntry(Timesheet timesheet);

    TimesheetEntry getLatestInactiveEntry(Timesheet timesheet);
}