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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("unused")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class JsonTimesheet {

    @XmlElement
    private int timesheetID;
    @XmlElement
    private String lectures;
    @XmlElement
    private String reason;
    @XmlElement
    private double ects;
    @XmlElement
    private String latestEntryDate;
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
    private boolean isEnabled;
    @XmlElement
    private boolean isMTSheet;

    public JsonTimesheet(int timesheetID, String lectures, String reason, double ects, String latestEntryDate, int targetHourPractice,
                         int targetHourTheory, int targetHours, int targetHoursCompleted, int targetHoursRemoved, boolean isActive,
                         boolean isEnabled, boolean isMTSheet) {
        this.timesheetID = timesheetID;
        this.lectures = lectures;
        this.reason = reason;
        this.ects = ects;
        this.latestEntryDate = latestEntryDate;
        this.targetHourPractice = targetHourPractice;
        this.targetHourTheory = targetHourTheory;
        this.targetHours = targetHours;
        this.targetHoursCompleted = targetHoursCompleted;
        this.targetHoursRemoved = targetHoursRemoved;
        this.isActive = isActive;
        this.isEnabled = isEnabled;
        this.isMTSheet = isMTSheet;
    }

    public JsonTimesheet() {
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

    public double getEcts() {
        return ects;
    }

    public void setEcts(double ects) {
        this.ects = ects;
    }

    public String getLatestEntryDate() {
        return latestEntryDate;
    }

    public void setLatestEntryDate(String latestEntryDate) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonTimesheet that = (JsonTimesheet) o;

        if (timesheetID != that.timesheetID) return false;
        if (ects != that.ects) return false;
        if (latestEntryDate != that.latestEntryDate) return false;
        if (targetHourPractice != that.targetHourPractice) return false;
        if (targetHourTheory != that.targetHourTheory) return false;
        if (targetHours != that.targetHours) return false;
        if (targetHoursCompleted != that.targetHoursCompleted) return false;
        if (targetHoursRemoved != that.targetHoursRemoved) return false;
        if (isActive != that.isActive) return false;
        if (isEnabled != that.isEnabled) return false;

        return lectures.equals(that.lectures);
    }

    @Override
    public int hashCode() {
        int result = timesheetID;
        result = 31 * result + lectures.hashCode();
        result = 31 * result + reason.hashCode();
        result = 31 * result + (int)ects;
        //result = 31 * result + latestEntryDate.hashCode();
        result = 31 * result + targetHourPractice;
        result = 31 * result + targetHourTheory;
        result = 31 * result + targetHours;
        result = 31 * result + targetHoursCompleted;
        result = 31 * result + targetHoursRemoved;
        result = 31 * result + (isActive ? 1 : 0);
        result = 31 * result + (isEnabled ? 1 : 0);
        return result;
    }
}
