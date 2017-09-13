package org.catrobat.jira.timesheet.rest.json;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JsonTimesheetReasonData {

    @XmlElement
    private String reason;

    @XmlElement
    private int hours;

    public JsonTimesheetReasonData() {
    }

    public JsonTimesheetReasonData(String reason, int hours){
        this.reason = reason;
        this.hours = hours;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }
}
