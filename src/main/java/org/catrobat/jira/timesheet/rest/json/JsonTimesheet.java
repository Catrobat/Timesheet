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
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
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
    private boolean isEnabled;
    @XmlElement
    private boolean isMTSheet;
    @XmlElement
    private Timesheet.State state;
    @XmlElement
    private String displayName;

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
        this.isMTSheet = timesheet.getIsMasterThesisTimesheet();
        this.state = timesheet.getState();
        this.displayName = timesheet.getDisplayName();
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

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
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

    public String getDisplayName() {return displayName;}

    public void setDisplayName(String displayName) { this.displayName = displayName;}

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JsonTimesheet that = (JsonTimesheet) o;

        if (timesheetID != that.timesheetID) {
            return false;
        }
        if (latestEntryDate != that.latestEntryDate) {
            return false;
        }
        if (targetHourPractice != that.targetHourPractice) {
            return false;
        }
        if (targetHourTheory != that.targetHourTheory) {
            return false;
        }
        if (targetHours != that.targetHours) {
            return false;
        }
        if (targetHoursCompleted != that.targetHoursCompleted) {
            return false;
        }
        if (targetHoursRemoved != that.targetHoursRemoved) {
            return false;
        }
        if (isEnabled != that.isEnabled) {
            return false;
        }
        if (displayName != that.displayName) {
            return false;
        }

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
        result = 31 * result + (isEnabled ? 1 : 0);
        return result;
    }
}
