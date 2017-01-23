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
import net.java.ao.OneToMany;

import java.util.Date;

public interface Timesheet extends Entity {

    String getUserKey();

    void setUserKey(String key);

    int getTargetHoursPractice();

    void setTargetHoursPractice(int hours);

    int getTargetHoursTheory();

    void setTargetHoursTheory(int hours);

    int getTargetHours();

    void setTargetHours(int hours);

    int getTargetHoursCompleted();

    void setTargetHoursCompleted(int hours);

    int getTargetHoursRemoved();

    void setTargetHoursRemoved(int hours);

    boolean getIsActive();

    void setIsActive(boolean isActive);

    boolean getIsAutoInactive();

    void setIsAutoInactive(boolean isAutoInactive);

    boolean getIsOffline();

    void setIsOffline(boolean isOffline);

    boolean getIsEnabled();

    void setIsEnabled(boolean isEnabled);

    String getLectures();

    void setLectures(String lectures);

    String getReason();

    void setReason(String reason);

    Date getLatestEntryBeginDate();

    void setLatestEntryBeginDate(Date date);

    boolean getIsMasterThesisTimesheet();

    void setIsMasterThesisTimesheet(boolean isMasterThesisTimesheet);

    void setIsReactivated(boolean reactivated);

    boolean getIsReactivated();

    void setState(State state);

    State getState();

    enum State {
        ACTIVE, INACTIVE, AUTO_INACTIVE, INACTIVE_OFFLINE
    }

    @OneToMany(reverse = "getTimeSheet")
    TimesheetEntry[] getEntries();

}
