package org.catrobat.jira.timesheet.rest.json;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonTimesheetAndEntries {

    @XmlElement
    private JsonTimesheet jsonTimesheet;

    @XmlElement
    private List<JsonTimesheetEntry> jsonTimesheetEntryList;

    public JsonTimesheetAndEntries(JsonTimesheet jsonTimesheet, List<JsonTimesheetEntry> jsonTimesheetEntriesList) {
        this.jsonTimesheet = jsonTimesheet;
        this.jsonTimesheetEntryList = jsonTimesheetEntriesList;
    }
}
