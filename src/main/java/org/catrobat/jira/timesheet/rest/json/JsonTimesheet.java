/*
 * Copyright 2015 Christof Rabensteiner
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
package org.catrobat.jira.timesheet.rest.json;

import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@SuppressWarnings("unused")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

/* Info: @deprecated
 * for further projects / classes use the very powerful GSON library from Google. Have a look here:
 * https://github.com/google/gson/blob/master/UserGuide.md
 * It is easier to use and you haven't create a class for each object you would like to serialise.
 * BTW: it is already included in this project, so feel free to use it up to now.
*/
public final class JsonTimesheet {

    @XmlElement
    private String userKey;
    @XmlElement
    private int timesheetID;
    @XmlElement
    private String lectures;
    @XmlElement
    private String reason;
    @XmlElement
    @JsonDeserialize(using=DateAndTimeDeserialize.class)
    private Date latestEntryDate;
    @XmlElement
    private int targetHourPractice;
    @XmlElement
    private int targetHourTheory;
    @XmlElement
    private int targetHours;
    @XmlElement
    private int targetHoursCompleted;
    @XmlElement
    private int targetHoursRemoved;
    @XmlElement
    private boolean isActive;
    @XmlElement
    private boolean isAutoInactive;
    @XmlElement
    private boolean isOffline;
    @XmlElement
    private boolean isAutoOffline;
    @XmlElement
    private boolean isEnabled;
    @XmlElement
    private boolean isMTSheet;
    @XmlElement
    private Timesheet.State state;

    /*public JsonTimesheet(int timesheetID, String lectures, String reason, Date latestEntryDate, int targetHourPractice,
            int targetHourTheory, int targetHours, int targetHoursCompleted, int targetHoursRemoved, boolean isActive,
            boolean isAutoInactive, boolean isOffline, boolean isAutoOffline, boolean isEnabled, boolean isMTSheet, Timesheet.State state) {
        this.timesheetID = timesheetID;
        // TODO: userKey missing
        this.lectures = lectures;
        this.reason = reason;
        this.latestEntryDate = latestEntryDate;
        this.targetHourPractice = targetHourPractice;
        this.targetHourTheory = targetHourTheory;
        this.targetHours = targetHours;
        this.targetHoursCompleted = targetHoursCompleted;
        this.targetHoursRemoved = targetHoursRemoved;
        this.isActive = isActive;
        this.isAutoInactive = isAutoInactive;
        this.isOffline = isOffline;
        this.isAutoOffline = isAutoOffline;
        this.isEnabled = isEnabled;
        this.isMTSheet = isMTSheet;
        this.state = state;
    }*/

    public JsonTimesheet(Timesheet timesheet) {
        this.timesheetID = timesheet.getID();
        this.userKey = timesheet.getUserKey();
        this.lectures = timesheet.getLectures();
        this.reason = timesheet.getReason();
        this.latestEntryDate = timesheet.getLatestEntryBeginDate();
        this.targetHourPractice = timesheet.getTargetHoursPractice();
        this.targetHourTheory = timesheet.getTargetHoursTheory();
        this.targetHours = timesheet.getTargetHours();
        this.targetHoursCompleted = timesheet.getTargetHoursCompleted();
        this.targetHoursRemoved = timesheet.getTargetHoursRemoved();
        this.isActive = timesheet.getIsActive();
        this.isAutoInactive = timesheet.getIsAutoInactive();
        this.isOffline = timesheet.getIsOffline();
        this.isEnabled = timesheet.getIsEnabled();
        this.isMTSheet = timesheet.getIsMasterThesisTimesheet();
        this.state = timesheet.getState();
    }

    public JsonTimesheet() {
    }

    public String getUserKey() {
        return userKey;
    }

    public int getTimesheetID() {
        return timesheetID;
    }

    public void setTimesheetID(int timesheetID) {
        this.timesheetID = timesheetID;
    }

    public String getLectures() {
        return lectures;
    }

    public void setLectures(String lectures) {
        this.lectures = lectures;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getLatestEntryDate() {
        return latestEntryDate;
    }

    public void setLatestEntryDate(Date latestEntryDate) {
        this.latestEntryDate = latestEntryDate;
    }

    public int getTargetHourPractice() {
        return targetHourPractice;
    }

    public void setTargetHourPractice(int targetHourPractice) {
        this.targetHourPractice = targetHourPractice;
    }

    public int getTargetHourTheory() {
        return targetHourTheory;
    }

    public void setTargetHourTheory(int targetHourTheory) {
        this.targetHourTheory = targetHourTheory;
    }

    public int getTargetHours() {
        return targetHours;
    }

    public void setTargetHours(int targetHours) {
        this.targetHours = targetHours;
    }

    public int getTargetHoursCompleted() {
        return targetHoursCompleted;
    }

    public void setTargetHoursCompleted(int targetHoursCompleted) {
        this.targetHoursCompleted = targetHoursCompleted;
    }

    public int getTargetHoursRemoved() {
        return targetHoursRemoved;
    }

    public void setTargetHoursRemoved(int targetHoursRemoved) {
        this.targetHoursRemoved = targetHoursRemoved;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isAutoInactive() {
        return isAutoInactive;
    }

    public void setAutoInactive(boolean autoInactive) {
        isAutoInactive = autoInactive;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public void setOffline(boolean offline) {
        isOffline = offline;
    }

    public boolean isAutoOffline() {
        return isAutoOffline;
    }

    public void setAutoOffline(boolean autoOffline) {
        isAutoOffline = autoOffline;
    }

    public boolean isMTSheet() {
        return isMTSheet;
    }

    public void setMTSheet(boolean MTSheet) {
        isMTSheet = MTSheet;
    }

    public Timesheet.State getState() {
        return state;
    }

    public void setState(Timesheet.State state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        JsonTimesheet that = (JsonTimesheet) o;

        if (timesheetID != that.timesheetID) { return false; }
        if (latestEntryDate != that.latestEntryDate) { return false; }
        if (targetHourPractice != that.targetHourPractice) { return false; }
        if (targetHourTheory != that.targetHourTheory) { return false; }
        if (targetHours != that.targetHours) { return false; }
        if (targetHoursCompleted != that.targetHoursCompleted) { return false; }
        if (targetHoursRemoved != that.targetHoursRemoved) { return false; }
        if (isActive != that.isActive) { return false; }
        if (isAutoInactive != that.isAutoInactive) { return false;}
        if (isOffline != that.isOffline) { return false;}
        if (isAutoOffline != that.isAutoOffline) { return false;}
        if (isEnabled != that.isEnabled) { return false; }

        return lectures.equals(that.lectures);
    }

    @Override
    public int hashCode() {
        int result = timesheetID;
        result = 31 * result + lectures.hashCode();
        result = 31 * result + reason.hashCode();
        //result = 31 * result + latestEntryDate.hashCode();
        result = 31 * result + targetHourPractice;
        result = 31 * result + targetHourTheory;
        result = 31 * result + targetHours;
        result = 31 * result + targetHoursCompleted;
        result = 31 * result + targetHoursRemoved;
        result = 31 * result + (isActive ? 1 : 0);
        result = 31 * result + (isAutoInactive ? 1 : 0);
        result = 31 * result + (isOffline ? 1 : 0);
        result = 31 * result + (isAutoOffline ? 1 : 0);
        result = 31 * result + (isEnabled ? 1 : 0);
        return result;
    }
}
