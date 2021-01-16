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
import net.java.ao.schema.NotNull;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetReasonData;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

@Transactional
public interface TimesheetService {
    /**
     * Edits a existing Timesheet
     * @param userKey             identifies the user
     * @param lectures            describes the lecture in which the user is enrolled
     * @return the new Timesheet, or null          */
    @Nullable
    Timesheet editTimesheets(String userKey,
                            int targetHours, int targetHoursCompleted, int targetHoursRemoved, String lectures,
                            String reason, Date latestEntryDate,
                            Timesheet.State state) throws ServiceException;

    Timesheet updateTimesheet(int id, int targetHoursCompleted,  Date latestEntryDate, Timesheet.State state);

    /**
     * Adds a new Timesheet
     * @param userKey             identifies the user
     * @param displayName           the name displayed for the timesheet
     * @param lectures            describes the lecture in which the user is enrolled
     * @return the new Timesheet        */
    @NotNull
    Timesheet add(String userKey, String displayName,
                  int targetHours, int targetHoursCompleted, int targetHoursRemoved, String lectures,
                  String reason,
                  Timesheet.State state) throws ServiceException;

    @NotNull
    List<Timesheet> all();

    @Nullable
    Timesheet updateTimesheetEnableState(int timesheetID, Boolean isEnabled) throws ServiceException;

    void updateTimesheetReasonData(Timesheet sheet, JsonTimesheetReasonData jsonTimesheetReasonData);

    void deleteLecture(Timesheet sheet, JsonTimesheetReasonData jsonTimesheetReasonData);

    /**
     * Returns Timesheet corresponding to a User
     *
     * @return Timesheet, null if unknown user
     */
    @Nullable
    Timesheet getTimesheetByUser(String userKey) throws ServiceException;

    /**
     * Returns true if the user has a timesheet, otherwise false
     *
     * @return Boolean
     */
    Boolean userHasTimesheet(String userKey) throws ServiceException;

    @Nullable
    Timesheet getTimesheetByID(int id);

    void remove(Timesheet timesheet) throws ServiceException;
}
