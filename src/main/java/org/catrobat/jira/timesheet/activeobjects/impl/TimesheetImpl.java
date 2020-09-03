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
}
