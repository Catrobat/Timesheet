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

package org.catrobat.jira.timesheet.activeobjects;

import net.java.ao.*;
import net.java.ao.schema.Ignore;
import net.java.ao.schema.NotNull;
import org.catrobat.jira.timesheet.activeobjects.impl.TimesheetImpl;

import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

@Implementation(TimesheetImpl.class)
public interface Timesheet extends Entity {

    @NotNull
    String getUserKey();
    void setUserKey(String key);

    String getDisplayName();
    void setDisplayName(String name);

    int getHoursCompleted();
    void setHoursCompleted(int hours);

    int getTargetHours();
    void setTargetHours(int hours);

    int getHoursDeducted();
    void setHoursDeducted(int hours);

    String getLectures();
    void setLectures(String lectures);

    String getReason();
    void setReason(String reason);

    Date getLatestEntryBeginDate();
    void setLatestEntryBeginDate(Date date);

    void setState(State state);
    State getState();

    int getHoursCurrentPeriod();
    void setHoursCurrentPeriod(int hoursCurrentMonitoringPeriod);

    int getHoursLastPeriod();
    void setHoursLastPeriod(int hoursLastMonitoringPeriod);

    int getHoursLastMonth();
    void setHoursLastMonth(int hoursLastMonth);

    int getHoursLastHalfYear();
    void setHoursLastHalfYear(int hoursLastYear);

    enum State {
        ACTIVE, INACTIVE, AUTO_INACTIVE, INACTIVE_OFFLINE, DISABLED, DONE
    }

    @OneToMany(reverse = "getTimeSheet")
    TimesheetEntry[] getEntries();

    @Ignore
    TimesheetEntry firstEntry();

    @Ignore
    int calculateTotalHours();

    @Ignore
    void updateHoursAllPeriods(LocalDate cur_interval_begin, LocalDate cur_interval_end, LocalDate prev_interval_begin, LocalDate prev_interval_end);

}
