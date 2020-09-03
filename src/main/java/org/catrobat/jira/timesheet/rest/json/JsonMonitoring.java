package org.catrobat.jira.timesheet.rest.json;

import org.catrobat.jira.timesheet.activeobjects.Monitoring;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDate;

@SuppressWarnings("unused")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonMonitoring {

    @XmlElement
    private int period;
    private String periodTime;
    private int requiredHours;
    private int exceptions;

    public JsonMonitoring(Monitoring monitoring) {
        this.period = monitoring.getPeriod();
        this.periodTime = "";
        this.requiredHours = monitoring.getRequiredHours();
        this.exceptions = monitoring.getExceptions();
    }

    public JsonMonitoring() {

    }

    public int getPeriod() {
        return period;
    }

    public String getPeriodTime() {
        return periodTime;
    }

    public void setPeriodTime(String periodTime) {
        this.periodTime = periodTime;
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
