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

import net.java.ao.Entity;
import net.java.ao.Implementation;
import net.java.ao.OneToMany;
import net.java.ao.Transient;
import net.java.ao.schema.Ignore;
import net.java.ao.schema.NotNull;
import org.catrobat.jira.timesheet.activeobjects.impl.TimesheetImpl;

import java.util.Date;

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

    enum State {
        ACTIVE, INACTIVE, AUTO_INACTIVE, INACTIVE_OFFLINE, DISABLED, DONE
    }

    @OneToMany(reverse = "getTimeSheet")
    TimesheetEntry[] getEntries();

    @Ignore
    TimesheetEntry firstEntry();

    @Ignore
    int calculateTotalHours();

}
