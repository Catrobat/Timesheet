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
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class TimesheetServiceImpl implements TimesheetService {

    private final ActiveObjects ao;

    public TimesheetServiceImpl(ActiveObjects ao) {
        this.ao = ao;
    }

    @Override
    public Timesheet editTimesheet(String userKey, int targetHoursPractice, int targetHoursTheory,
                                   int targetHours, int targetHoursCompleted, int targetHoursRemoved,
                                   String lectures, String reason, int ects, String latestEntryDate,
                                   Boolean isActive, Boolean isEnabled, Boolean isMasterThesisTimesheet) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        if (found.length > 2) {
            throw new ServiceException("Found more than two Timesheets with the same UserKey.");
        } else if (found.length == 0) {
            throw new ServiceException("Access denied. No 'Timesheet' found for this user.");
        }

        for(int i = 0; i < found.length; i++) {
            if(isMasterThesisTimesheet.equals(found[i].getIsMasterThesisTimesheet())) {
                Timesheet sheet = found[i];

                sheet.setUserKey(userKey);
                sheet.setTargetHoursPractice(targetHoursPractice);
                sheet.setTargetHoursTheory(targetHoursTheory);
                sheet.setTargetHours(targetHours);
                sheet.setTargetHoursCompleted(targetHoursCompleted);
                sheet.setTargetHoursRemoved(targetHoursRemoved);
                sheet.setLectures(lectures);
                sheet.setReason(reason);
                sheet.setEcts(ects);
                sheet.setLatestEntryDate(latestEntryDate);
                sheet.setIsActive(isActive);
                sheet.setIsEnabled(isEnabled);
                sheet.save();
                return sheet;
            }
        }

        return null;
    }

    @Override
    public Timesheet add(String userKey, int targetHoursPractice, int targetHoursTheory,
                         int targetHours, int targetHoursCompleted, int targetHoursRemoved,
                         String lectures, String reason, int ects, String latestEntryDate,
                         Boolean isActive, Boolean isEnabled, Boolean isMasterThesisTimesheet) {
        Timesheet sheet = ao.create(Timesheet.class);
        sheet.setUserKey(userKey);
        sheet.setTargetHoursPractice(targetHoursPractice);
        sheet.setTargetHoursTheory(targetHoursTheory);
        sheet.setTargetHours(targetHours);
        sheet.setTargetHoursCompleted(targetHoursCompleted);
        sheet.setTargetHoursRemoved(targetHoursRemoved);
        sheet.setLectures(lectures);
        sheet.setReason(reason);
        sheet.setEcts(ects);
        sheet.setLatestEntryDate(new DateTime().toString());
        sheet.setIsActive(isActive);
        sheet.setIsEnabled(isEnabled);
        sheet.setIsMasterThesisTimesheet(isMasterThesisTimesheet);
        sheet.save();
        return sheet;
    }

    @Override
    public List<Timesheet> all() {
        return newArrayList(ao.find(Timesheet.class));
    }

    @Nullable
    @Override
    public Timesheet updateTimesheetEnableState(int timesheetID, Boolean isEnabled) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "ID = ?", timesheetID);
        if ((found.length == 1)) {
            Timesheet sheet = found[0];
            sheet.setIsEnabled(isEnabled);
            sheet.save();
            return sheet;
        } else  {
            throw new ServiceException("Access denied. No 'Timesheet' found for this user.");
        }
    }

    @Override
    public Timesheet getTimesheetByUser(String userKey, Boolean isMasterThesisTimesheet) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        if (found.length > 2) {
            throw new ServiceException("Found more than two Timesheets with the same UserKey.");
        } else if (found.length == 0) {
            throw new ServiceException("Access denied. No 'Timesheet' found for this user.");
        }

        if (isMasterThesisTimesheet) {
            for (int i = 0; i < found.length; i++) {
                if (found[i].getIsMasterThesisTimesheet()) {
                    return found[i];
                }
            }
        }

        if (!isMasterThesisTimesheet) {
            for (int i = 0; i < found.length; i++) {
                if (!found[i].getIsMasterThesisTimesheet()) {
                    return found[i];
                }
            }
        }

        return null;
    }

    @Override
    public Timesheet getAdministratorTimesheet(String userKey) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        if (found.length > 1) {
            throw new ServiceException("Found more than two Timesheets with the same UserKey.");
        } else if (found.length == 0) {
            return null;
        } else {
            return found[0];
        }
    }

    @Override
    public Timesheet getTimesheetByID(int id) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "ID = ?", id);

        if (found.length > 1) {
            throw new ServiceException("Multiple Timesheets with the same ID.");
        } else if (found.length == 0) {
            throw new ServiceException("Access denied. No 'Timesheet' found for this user.");
        }

        return (found.length > 0) ? found[0] : null;
    }
}
