package org.catrobat.jira.timesheet.rest.json;

import org.catrobat.jira.timesheet.activeobjects.Scheduling;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("unused")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonScheduling {

    @XmlElement
    private int inactiveTime;
    private int offlineTime;
    private int remainingTime;

    private int outOfTime;

    public JsonScheduling(Scheduling scheduling) {
        this.inactiveTime = scheduling.getInactiveTime();
        this.offlineTime = scheduling.getOfflineTime();
        this.remainingTime = scheduling.getRemainingTime();
        this.outOfTime = scheduling.getOutOfTime();
    }

    public JsonScheduling() {

    }

    public int getInactiveTime() {
        return inactiveTime;
    }

    public void setInactiveTime(int inactiveTime) {
        this.inactiveTime = inactiveTime;
    }

    public int getOfflineTime() {
        return offlineTime;
    }

    public void setOfflineTime(int offlineTime) {
        this.offlineTime = offlineTime;
    }

    public int getRemainingTime() {return remainingTime;}

    public void setRemainingTime(int remainingTime) {this.remainingTime = remainingTime;}

    public int getOutOfTime() {
        return outOfTime;
    }

    public void setOutOfTime(int outOfTime) {
        this.outOfTime = outOfTime;
    }
}
