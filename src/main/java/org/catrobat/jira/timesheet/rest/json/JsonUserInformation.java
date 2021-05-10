package org.catrobat.jira.timesheet.rest.json;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import com.atlassian.jira.component.ComponentAccessor;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonUserInformation {
    @XmlElement
    private String userName;
    @XmlElement
    private Timesheet.State state;
    @XmlElement
    private int hoursPerMonth;
    @XmlElement
    private int hoursPerHalfYear;
    @XmlElement
    private int hoursPerMonitoringPeriod;
    @XmlElement
    private int hoursPerLastMonitoringPeriod;
    @XmlElement
    private int remainingHours;
    @XmlElement
    private int targetTotalHours;
    @XmlElement
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
    private Date firstEntryDate;
    @XmlElement
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
    private Date latestEntryDate;
    @XmlElement
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
    private Date inactiveEndDate;
    @XmlElement
    private int totalHours;
    @XmlElement
    private int latestEntryHours;
    @XmlElement
    private String latestEntryDescription;
    @XmlElement
    private String email;
    @XmlElement
    private String teams;
    @XmlElement
    private int timesheetID;
    @XmlElement
    private boolean isEnabled;

    public JsonUserInformation (Timesheet timesheet) {
    	
    	this.userName = ComponentAccessor.getUserManager().getUserByKey(timesheet.getUserKey()).getName();
    	this.state = timesheet.getState();
        this.latestEntryDate = timesheet.getLatestEntryBeginDate();

        this.remainingHours = (timesheet.getTargetHours() - timesheet.getHoursCompleted() 
				+ timesheet.getHoursDeducted());
        this.targetTotalHours = timesheet.getTargetHours();

        this.totalHours = timesheet.getHoursCompleted();
        this.email = ComponentAccessor.getUserManager().getUserByKey(timesheet.getUserKey()).getEmailAddress();
        this.timesheetID = timesheet.getID();

        if (timesheet.firstEntry() != null) {
            this.firstEntryDate = timesheet.firstEntry().getBeginDate();
        }
    }

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

    public int getHoursPerMonitoringPeriod(){
        return  hoursPerMonitoringPeriod;
    }

    public void setHoursPerMonitoringPeriod(int hoursPerMonitoringPeriod) {
        this.hoursPerMonitoringPeriod = hoursPerMonitoringPeriod;
    }

    public int getHoursPerLastMonitoringPeriod(){
        return  hoursPerLastMonitoringPeriod;
    }

    public void setHoursPerLastMonitoringPeriod(int hoursPerLastMonitoringPeriod) {
        this.hoursPerLastMonitoringPeriod = hoursPerLastMonitoringPeriod;
    }

    public int getHoursPerHalfYear() {
        return hoursPerHalfYear;
    }

    public void setHoursPerHalfYear(int hoursPerHalfYear) {
        this.hoursPerHalfYear = hoursPerHalfYear;
    }

    public int getRemainingHours() {
        return remainingHours;
    }

    public void setRemainingHours(int remainingHours) {
        this.remainingHours = remainingHours;
    }
    
    public int getTargetTotalHours() {
        return targetTotalHours;
    }

    public void setTargetTotalHours(int targetTotalHours) {
        this.targetTotalHours = targetTotalHours;
    }

    public Date getFirstEntryDate() {
        return firstEntryDate;
    }

    public void setFirstEntryDate(Date firstEntryDate) {
        this.firstEntryDate = firstEntryDate;
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

    public int getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(int totalHours) {
        this.totalHours = totalHours;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTeams() {
        return teams;
    }

    public void setTeams(String teams) {
        this.teams = teams;
    }

    public int getTimesheetID() {
        return timesheetID;
    }

    public void setTimesheetID(int timesheetID) {
        this.timesheetID = timesheetID;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
