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
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import net.java.ao.DBParam;
import net.java.ao.schema.NotNull;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class TimesheetServiceImpl implements TimesheetService {

    private final ActiveObjects ao;

    public TimesheetServiceImpl(ActiveObjects ao) {
        this.ao = ao;
    }


    @Override
    public Timesheet editTimesheet(String userKey, int targetHoursPractice, int targetHoursTheory,
                                   int targetHours, int targetHoursCompleted, int targetHoursRemoved, String lectures, String reason, Date latestEntryDate,
                                   boolean isMasterThesisTimesheet, Timesheet.State state) throws ServiceException {

        ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);
        if (found.length > 2) {
            throw new ServiceException("Found more than two Timesheets with the same UserKey.");
        } else if (found.length == 0) {
            throw new ServiceException("No 'Timesheet' found for this user.");
        }
        if (lectures.length() > 255) {
            throw new ServiceException("Lectures shall not be longer than 255 characters.");
        }

        for (Timesheet aFound : found) {
            if (isMasterThesisTimesheet == aFound.getIsMasterThesisTimesheet()) {
                Timesheet sheet = aFound;

                sheet.setDisplayName(user.getDisplayName());
                sheet.setTargetHoursPractice(targetHoursPractice);
                sheet.setTargetHoursTheory(targetHoursTheory);
                sheet.setTargetHours(targetHours);
                sheet.setTargetHoursCompleted(targetHoursCompleted);
                sheet.setTargetHoursRemoved(targetHoursRemoved);
                sheet.setLectures(lectures);
                sheet.setReason(reason);
                sheet.setLatestEntryBeginDate(latestEntryDate);
                sheet.setState(state);
                sheet.save();
                return sheet;
            }
        }

        return null;
    }

    @Override
    public Timesheet updateTimesheet(int id, int targetHoursCompleted, int targetHoursPractice,  Date latestEntryDate, Timesheet.State state) {
        Timesheet timesheet = ao.get(Timesheet.class, id);
        timesheet.setTargetHoursCompleted(targetHoursCompleted);
        timesheet.setTargetHoursPractice(targetHoursPractice);
        timesheet.setLatestEntryBeginDate(latestEntryDate);
        timesheet.setState(state);
        timesheet.save();
        return timesheet;
    }

    @NotNull
    @Override
    public Timesheet add(String userKey, String displayName, int targetHoursPractice, int targetHoursTheory,
                         int targetHours, int targetHoursCompleted, int targetHoursRemoved,
                         String lectures, String reason,
                         boolean isMasterThesisTimesheet, Timesheet.State state) throws ServiceException {

        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ? AND IS_MASTER_THESIS_TIMESHEET = ?", userKey, isMasterThesisTimesheet);
        if (found.length != 0) {
            throw new ServiceException("A timesheet for user: " + userKey + " isMTSheet: " + isMasterThesisTimesheet + "already exists");
        }

        Timesheet sheet = ao.create(Timesheet.class,
            new DBParam("USER_KEY", userKey)
        );
        sheet.setDisplayName(displayName);
        sheet.setTargetHoursPractice(targetHoursPractice);
        sheet.setTargetHoursTheory(targetHoursTheory);
        sheet.setTargetHours(targetHours);
        sheet.setTargetHoursCompleted(targetHoursCompleted);
        sheet.setTargetHoursRemoved(targetHoursRemoved);
        sheet.setLectures(lectures);
        sheet.setReason(reason);
        sheet.setLatestEntryBeginDate(new Date());
        sheet.setIsMasterThesisTimesheet(isMasterThesisTimesheet);
        sheet.setState(state);
        sheet.save();
        return sheet;
    }

    @Override
    public void remove(Timesheet timesheet) throws ServiceException {
        for (TimesheetEntry entry : timesheet.getEntries()) {
            ao.delete(entry);
        }

        ao.delete(timesheet);
    }

    @NotNull
    @Override
    public List<Timesheet> all() {
        return newArrayList(ao.find(Timesheet.class));
    }

    @Nullable
    @Override
    public Timesheet updateTimesheetEnableState(int timesheetID, Boolean isEnabled) throws ServiceException {
        Timesheet sheet = ao.get(Timesheet.class, timesheetID);
        if (sheet == null) {
            throw new ServiceException("No Timesheet found for this user.");
        }

        if (!isEnabled) {
            sheet.setState(Timesheet.State.DISABLED);
        }
        if (isEnabled && sheet.getState() == Timesheet.State.DISABLED) {
            sheet.setState(Timesheet.State.ACTIVE);
        }
        sheet.save();
        return sheet;
    }

    @Override
    public Timesheet getTimesheetByUser(String userKey, Boolean isMasterThesisTimesheet) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);
        if (found.length > 2) {
            throw new ServiceException("Found more than two Timesheets with the same UserKey.");
        }

        if (isMasterThesisTimesheet) {
            for (Timesheet aFound : found) {
                if (aFound.getIsMasterThesisTimesheet()) {
                    return aFound;
                }
            }
        } else {
            for (Timesheet aFound : found) {
                if (!aFound.getIsMasterThesisTimesheet()) {
                    return aFound;
                }
            }
        }

        throw new ServiceException("No Timesheet found. Maybe user does not have one.");
    }

    @Override
    public Boolean userHasTimesheet(String userKey, Boolean isMasterThesisTimesheet) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        if (found.length > 2) {
            ao.delete(found);
            throw new ServiceException("Found more than two Timesheets with the same UserKey. All timesheets will be deleted.");
        }

        if (isMasterThesisTimesheet) {
            for (Timesheet aFound : found) {
                if (aFound.getIsMasterThesisTimesheet()) {
                    return true;
                }
            }
        } else {
            for (Timesheet aFound : found) {
                if (!aFound.getIsMasterThesisTimesheet()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Timesheet getTimesheetByID(int id) {
        return ao.get(Timesheet.class, id);
    }
}
