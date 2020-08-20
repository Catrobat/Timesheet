package org.catrobat.jira.timesheet.rest.json;

import org.catrobat.jira.timesheet.activeobjects.Monitoring;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("unused")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonMonitoring {

    @XmlElement
    private int period;
    private int requiredHours;
    private int exceptions;

    public JsonMonitoring(Monitoring monitoring) {
        this.period = monitoring.getPeriod();
        this.requiredHours = monitoring.getRequiredHours();
        this.exceptions = monitoring.getExceptions();
    }

    public JsonMonitoring() {

    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getRequiredHours() {
        return requiredHours;
    }

    public void setRequiredHours(int requiredHours) {
        this.requiredHours = requiredHours;
    }

    public int getExceptions() {return exceptions;}

    public void setExceptions(int exceptions) {this.exceptions = exceptions;}

}
