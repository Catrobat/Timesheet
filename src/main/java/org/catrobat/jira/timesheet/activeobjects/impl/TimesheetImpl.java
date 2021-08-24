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

import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.joda.time.DateTime;

import java.time.LocalDateTime;
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

    public final TimesheetEntry[] getEntriesDesc() {
        TimesheetEntry[] entries = timesheet.getEntries();
        Arrays.sort(entries, Comparator.comparing(TimesheetEntry::getBeginDate).reversed());
        return entries;
    }

    public final TimesheetEntry[] getEntriesAsc() {
        TimesheetEntry[] entries = timesheet.getEntries();
        Arrays.sort(entries, Comparator.comparing(TimesheetEntry::getBeginDate));
        return entries;
    }


    public final int calculateViolations(int monitoring_period, int required_hours) {
        TimesheetEntry[] entries = getEntriesAsc();
        //TODO: add some checks here
        if(entries.length == 0 || monitoring_period == 0) {
            return 0;
        }
        DateTime first_entry_date = new DateTime(entries[0].getBeginDate());
        DateTime last_entry_date = new DateTime(entries[entries.length - 1].getBeginDate());

        DateTime start = first_entry_date.withDayOfMonth(1).plusMonths(1).withTimeAtStartOfDay();
        DateTime end = last_entry_date.withDayOfMonth(1).withTimeAtStartOfDay();

        DateTime start_t = start.minusMillis(1);
        DateTime end_t = start.plusMonths(monitoring_period);

        int required_minutes = required_hours * 60;
        int violations = 0;
        int i = 0;
        DateTime date;

        while(end_t.isBefore(end) || end_t.isEqual(end)) {
            int minutes = 0;

            do {
                date = new DateTime(entries[i].getBeginDate());
                if(date.isAfter(start_t) && date.isBefore(end_t)) {
                    minutes += entries[i].getDurationMinutes();
                }
                i++;
            }
            while(date.isBefore(end_t));
            i--;

            if(minutes < required_minutes) {
                violations++;
            }

            start_t =  end_t.minusMillis(1);
            end_t = end_t.plusMonths(monitoring_period);
        }
        return violations;
    }
}
