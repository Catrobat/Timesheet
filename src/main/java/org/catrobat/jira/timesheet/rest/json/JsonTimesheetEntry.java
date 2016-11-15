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
public final class JsonTimesheetEntry {

    @XmlElement
    private int entryID;

    @XmlElement
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
    private Date beginDate;

    @XmlElement
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
    private Date endDate;

    @XmlElement
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
    private Date inactiveEndDate;

    @XmlElement
    @JsonDeserialize(using = DateAndTimeDeserialize.class)
    private Date deactivateEndDate;

    @XmlElement
    private int pauseMinutes;

    @XmlElement
    private String description;

    @XmlElement
    private int teamID;

    @XmlElement
    private int categoryID;

    @XmlElement
    private String ticketID;

    @XmlElement
    private String partner;

    @XmlElement
    private boolean isGoogleDocImport;

    public JsonTimesheetEntry() {
    }

    public JsonTimesheetEntry(int entryID, Date beginDate, Date endDate, Date inactiveEndDate, Date deactivateEndDate, int pauseMinutes,
            String description, int teamID, int categoryID, String ticketID, String partner,
            boolean isGoogleDocImport) {
        this.entryID = entryID;
        this.beginDate = beginDate;
        this.endDate = endDate;
        this.inactiveEndDate = inactiveEndDate;
        this.deactivateEndDate = deactivateEndDate;
        this.pauseMinutes = pauseMinutes;
        this.description = description;
        this.teamID = teamID;
        this.categoryID = categoryID;
        this.ticketID = ticketID;
        this.partner = partner;
        this.isGoogleDocImport = isGoogleDocImport;
    }

    public int getEntryID() {
        return entryID;
    }

    public void setEntryID(int entryID) {
        this.entryID = entryID;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getPauseMinutes() {
        return pauseMinutes;
    }

    public void setPauseMinutes(int pauseMinutes) {
        this.pauseMinutes = pauseMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTeamID() {
        return teamID;
    }

    public void setTeamID(int teamID) {
        this.teamID = teamID;
    }

    public int getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    public Date getInactiveEndDate() {
        return inactiveEndDate;
    }

    public void setInactiveEndDate(Date inactiveEndDate) {
        this.inactiveEndDate = inactiveEndDate;
    }

    public Date getDeactivateEndDate() { return deactivateEndDate; }

    public void setDeactivateEndDate(Date deactivateEndDate) { this.deactivateEndDate = deactivateEndDate; }

    public String getTicketID() {
        return ticketID;
    }

    public void setTicketID(String ticketID) {
        this.ticketID = ticketID;
    }

    public String getPairProgrammingUserName() {
        return partner;
    }

    public void setPairProgrammingUserName(String partner) {
        this.partner = partner;
    }

    public boolean getIsGoogleDocImport() {
        return isGoogleDocImport;
    }

    public void setIsGoogleDocImport(boolean isGoogleDocImport) {
        this.isGoogleDocImport = isGoogleDocImport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonTimesheetEntry that = (JsonTimesheetEntry) o;

        if (entryID != that.entryID) return false;
        if (pauseMinutes != that.pauseMinutes) return false;
        if (teamID != that.teamID) return false;
        if (categoryID != that.categoryID) return false;
        if (isGoogleDocImport != that.isGoogleDocImport) return false;
        if (!beginDate.equals(that.beginDate)) return false;
        if (!endDate.equals(that.endDate)) return false;
        if (!inactiveEndDate.equals(that.inactiveEndDate)) return false;
        if (!deactivateEndDate.equals(that.deactivateEndDate)) return false;
        if (!partner.equals(that.partner)) return false;
        if (!ticketID.equals(that.ticketID)) return false;

        return description.equals(that.description);
    }

    @Override
    public int hashCode() {
        int result = entryID;
        result = 31 * result + beginDate.hashCode();
        result = 31 * result + endDate.hashCode();
        result = 31 * result + pauseMinutes;
        result = 31 * result + description.hashCode();
        result = 31 * result + teamID;
        result = 31 * result + categoryID;
        result = 31 * result + ticketID.hashCode();
        result = 31 * result + partner.hashCode();
        result = 31 * result + inactiveEndDate.hashCode();
        result = 31 * result + deactivateEndDate.hashCode();

        return result;
    }

    public String toReadableString() {
        return beginDate + " - " + endDate + ": " + description +
                "; TeamID: " + teamID + "; CategoryID: " + categoryID
                + "; isGoogleDocImport: " + isGoogleDocImport;
    }

    @Override
    public String toString() {
        return "JsonTimesheetEntry{" +
                "entryID=" + entryID +
                ", beginDate=" + beginDate +
                ", endDate=" + endDate +
                ", pauseMinutes=" + pauseMinutes +
                ", description='" + description + '\'' +
                ", teamID=" + teamID +
                ", categoryID=" + categoryID +
                ", isGoogleDocImprt=" + isGoogleDocImport +
                '}';
    }
}
