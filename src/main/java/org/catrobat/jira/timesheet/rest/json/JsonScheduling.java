package org.catrobat.jira.timesheet.rest.json;

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

    public JsonScheduling(int inactiveTime, int offlineTime) {
        this.inactiveTime = inactiveTime;
        this.offlineTime = offlineTime;
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
}
