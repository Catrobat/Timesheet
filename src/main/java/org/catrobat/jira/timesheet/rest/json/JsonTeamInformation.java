package org.catrobat.jira.timesheet.rest.json;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonTeamInformation {
    @XmlElement
    private String userName;
    @XmlElement
    private Timesheet.State state;
    @XmlElement
    private int hoursPerMonth;
    @XmlElement
    private int hoursPerHalfYear;
    @XmlElement
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
    private Date latestEntryDate;
    @XmlElement
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
    private Date inactiveEndDate;
    @XmlElement
    private int totalPracticeHours;
    @XmlElement
    private int latestEntryHours;
    @XmlElement
    private String latestEntryDescription;


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Timesheet.State getState() {
        return state;
    }

    public void setState(Timesheet.State state) {
        this.state = state;
    }

    public int getHoursPerMonth() {
        return hoursPerMonth;
    }

    public void setHoursPerMonth(int hoursPerMonth) {
        this.hoursPerMonth = hoursPerMonth;
    }

    public int getHoursPerHalfYear() {
        return hoursPerHalfYear;
    }

    public void setHoursPerHalfYear(int hoursPerHalfYear) {
        this.hoursPerHalfYear = hoursPerHalfYear;
    }

    public Date getLatestEntryDate() {
        return latestEntryDate;
    }

    public void setLatestEntryDate(Date latestEntryDate) {
        this.latestEntryDate = latestEntryDate;
    }

    public Date getInactiveEndDate() {
        return inactiveEndDate;
    }

    public void setInactiveEndDate(Date inactiveEndDate) {
        this.inactiveEndDate = inactiveEndDate;
    }

    public int getTotalPracticeHours() {
        return totalPracticeHours;
    }

    public void setTotalPracticeHours(int totalPracticeHours) {
        this.totalPracticeHours = totalPracticeHours;
    }

    public int getLatestEntryHours() {
        return latestEntryHours;
    }

    public void setLatestEntryHours(int latestEntryHours) {
        this.latestEntryHours = latestEntryHours;
    }

    public String getLatestEntryDescription() {
        return latestEntryDescription;
    }

    public void setLatestEntryDescription(String latestEntryDescription) {
        this.latestEntryDescription = latestEntryDescription;
    }
}
