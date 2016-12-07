/*
 * Copyright 2016 Adrian Schnedlitz
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

import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.ConfigService;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("unused")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

/* Info: @deprecated
 * for further projects / classes use the very powerful GSON library from Google. Have a look here:
 * https://github.com/google/gson/blob/master/UserGuide.md
 * It is easier to use and you haven't create a class for each object you would like to serialise.
 * BTW: it is already included in this project, so feel free to use it up to now.
*/
public final class JsonConfig {

    @XmlElement
    private List<JsonTeam> teams;
    @XmlElement
    private List<String> timesheetAdminGroups;
    @XmlElement
    private List<String> timesheetAdmins;
    @XmlElement
    private String readOnlyUsers;
    @XmlElement
    private String mailFromName;
    @XmlElement
    private String mailFrom;
    @XmlElement
    private String mailSubjectTime;
    @XmlElement
    private String mailSubjectInactive;
    @XmlElement
    private String mailSubjectOffline;
    @XmlElement
    private String mailSubjectActive;
    @XmlElement
    private String mailSubjectEntry;
    @XmlElement
    private String mailBodyTime;
    @XmlElement
    private String mailBodyInactive;
    @XmlElement
    private String mailBodyOffline;
    @XmlElement
    private String mailBodyActive;
    @XmlElement
    private String mailBodyEntry;

    public JsonConfig() {

    }

    public JsonConfig(ConfigService configService) {
        Config toCopy = configService.getConfiguration();

        Map<String, JsonTeam> teamMap = new TreeMap<>();
        for (Team team : toCopy.getTeams()) {
            teamMap.put(team.getTeamName(), new JsonTeam(team, configService));
        }

        this.teams = new ArrayList<>();
        this.teams.addAll(teamMap.values());

        this.timesheetAdmins = new ArrayList<>();
        for (TimesheetAdmin timesheetAdmin : toCopy.getTimesheetAdminUsers()) {
            if (timesheetAdmin.getUserKey() != null) {
                timesheetAdmins.add(timesheetAdmin.getUserName());
            }
        }

        this.timesheetAdminGroups = new ArrayList<>();
        for (TSAdminGroup timesheetAdminGroup : toCopy.getTimesheetAdminGroups()) {
            timesheetAdminGroups.add(timesheetAdminGroup.getGroupName());
        }

        this.readOnlyUsers = toCopy.getReadOnlyUsers();

        this.mailFromName = toCopy.getMailFromName();
        this.mailFrom = toCopy.getMailFrom();

        this.mailSubjectTime = toCopy.getMailSubjectTime();
        this.mailSubjectInactive = toCopy.getMailSubjectInactiveState();
        this.mailSubjectOffline = toCopy.getMailSubjectOfflineState();
        this.mailSubjectActive = toCopy.getMailSubjectActiveState();
        this.mailSubjectEntry = toCopy.getMailSubjectEntry();

        this.mailBodyTime = toCopy.getMailBodyTime();
        this.mailBodyInactive = toCopy.getMailBodyInactiveState();
        this.mailBodyOffline = toCopy.getMailBodyOfflineState();
        this.mailBodyActive = toCopy.getMailBodyActiveState();
        this.mailBodyEntry = toCopy.getMailBodyEntry();
    }

    public List<JsonTeam> getTeams() {
        return teams;
    }

    public void setTeams(List<JsonTeam> teams) {
        this.teams = teams;
    }

    public List<String> getTimesheetAdminGroups() {
        return timesheetAdminGroups;
    }

    public void setTimesheetAdminGroups(List<String> timesheetAdminGroups) {
        this.timesheetAdminGroups = timesheetAdminGroups;
    }

    public List<String> getTimesheetAdmins() {
        return timesheetAdmins;
    }

    public void setTimesheetAdmins(List<String> timesheetAdmins) {
        this.timesheetAdmins = timesheetAdmins;
    }

    public String getReadOnlyUsers() {
        return readOnlyUsers;
    }

    public void setReadOnlyUsers(String readOnlyUsers) {
        this.readOnlyUsers = readOnlyUsers;
    }

    public String getMailFromName() {
        return mailFromName;
    }

    public void setMailFromName(String mailFromName) {
        this.mailFromName = mailFromName;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getMailSubjectTime() {
        return mailSubjectTime;
    }

    public void setMailSubjectTime(String mailSubjectTime) {
        this.mailSubjectTime = mailSubjectTime;
    }

    public String getMailSubjectInactive() {
        return mailSubjectInactive;
    }

    public void setMailSubjectInactive(String mailSubjectInactive) {
        this.mailSubjectInactive = mailSubjectInactive;
    }

    public String getMailSubjectEntry() {
        return mailSubjectEntry;
    }

    public void setMailSubjectEntry(String mailSubjectEntry) {
        this.mailSubjectEntry = mailSubjectEntry;
    }

    public String getMailBodyTime() {
        return mailBodyTime;
    }

    public void setMailBodyTime(String mailBodyTime) {
        this.mailBodyTime = mailBodyTime;
    }

    public String getMailBodyInactive() {
        return mailBodyInactive;
    }

    public void setMailBodyInactive(String mailBodyInactive) {
        this.mailBodyInactive = mailBodyInactive;
    }

    public String getMailBodyEntry() {
        return mailBodyEntry;
    }

    public void setMailBodyEntry(String mailBodyEntry) {
        this.mailBodyEntry = mailBodyEntry;
    }

    public String getMailSubjectOffline() {
        return mailSubjectOffline;
    }

    public void setMailSubjectOffline(String mailSubjectOffline) {
        this.mailSubjectOffline = mailSubjectOffline;
    }

    public String getMailSubjectActive() {
        return mailSubjectActive;
    }

    public void setMailSubjectActive(String mailSubjectActive) {
        this.mailSubjectActive = mailSubjectActive;
    }

    public String getMailBodyOffline() {
        return mailBodyOffline;
    }

    public void setMailBodyOffline(String mailBodyOffline) {
        this.mailBodyOffline = mailBodyOffline;
    }

    public String getMailBodyActive() {
        return mailBodyActive;
    }

    public void setMailBodyActive(String mailBodyActive) {
        this.mailBodyActive = mailBodyActive;
    }
}
