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

package org.catrobat.jira.timesheet.activeobjects.impl;

import jdk.nashorn.internal.objects.annotations.Setter;
import net.java.ao.Accessor;
import net.java.ao.schema.Ignore;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class TimesheetImpl {

    private final Timesheet timesheet;

    public TimesheetImpl(Timesheet timesheet) {
        this.timesheet = timesheet;
    }

    public TimesheetEntry firstEntry() {
        TimesheetEntry[] entryArray = timesheet.getEntries();

        if (entryArray == null || entryArray.length == 0) {
            return null;
        }

        List<TimesheetEntry> entries = Arrays.asList(entryArray);
        entries.sort(Comparator.comparing(TimesheetEntry::getBeginDate));

        return entries.get(0);
    }

    public final int calculateTotalHours()     {
        TimesheetEntry[] entries = timesheet.getEntries();

        int sumTotalMinutes = 0;

        if (entries == null || entries.length == 0) {
            return 0;
        }

        for (TimesheetEntry entry : entries)  {
            sumTotalMinutes = sumTotalMinutes + entry.getDurationMinutes();
        }

        return (sumTotalMinutes / 60);
    }

    public final void updateHoursAllPeriods(LocalDate cur_interval_begin, LocalDate cur_interval_end, LocalDate last_interval_begin, LocalDate last_interval_end) {
        TimesheetEntry[] entries = timesheet.getEntries();

        if (entries == null || entries.length == 0) {
            timesheet.setHoursCompleted(0);
            timesheet.setHoursLastMonth(0);
            timesheet.setHoursLastHalfYear(0);
            timesheet.setHoursCurrentPeriod(0);
            timesheet.setHoursLastPeriod(0);
            return;
        }

        LocalDate last_month = LocalDate.now().minusMonths(1);
        LocalDate last_half_year = LocalDate.now().minusMonths(6);

        int minutes_sum = 0;
        int minutes_l_month = 0;
        int minutes_l_h_year = 0;
        int minutes_c_period = 0;
        int minutes_l_period = 0;

        for (TimesheetEntry entry : entries)  {
            LocalDate beginDate = entry.getBeginDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            minutes_sum += entry.getDurationMinutes();
            if (beginDate.isAfter(last_month)) {
                minutes_l_month += entry.getDurationMinutes();
            }
            if (beginDate.isAfter(last_half_year)) {
                minutes_l_h_year += entry.getDurationMinutes();
            }
            if (cur_interval_begin != null && cur_interval_end != null &&
                    (beginDate.isAfter(cur_interval_begin) || beginDate.isEqual(cur_interval_begin)) &&
                    (beginDate.isBefore(cur_interval_end) || beginDate.isEqual(cur_interval_end))) {
                minutes_c_period += entry.getDurationMinutes();
            }
            if (last_interval_begin != null && last_interval_end != null &&
                    (beginDate.isAfter(last_interval_begin) || beginDate.isEqual(last_interval_begin)) &&
                    (beginDate.isBefore(last_interval_end) || beginDate.isEqual(last_interval_end))) {
                minutes_l_period += entry.getDurationMinutes();
            }
        }
        timesheet.setHoursCompleted(minutes_sum / 60);
        timesheet.setHoursLastMonth(minutes_l_month / 60);
        timesheet.setHoursLastHalfYear(minutes_l_h_year / 60);
        timesheet.setHoursCurrentPeriod(minutes_c_period / 60);
        timesheet.setHoursLastPeriod(minutes_l_period / 60);
    }
}
