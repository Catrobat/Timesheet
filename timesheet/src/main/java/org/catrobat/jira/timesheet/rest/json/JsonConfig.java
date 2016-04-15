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
public final class JsonConfig {

    @XmlElement
    private List<JsonTeam> teams;
    @XmlElement
    private List<String> approvedGroups;
    @XmlElement
    private List<String> approvedUsers;
    @XmlElement
    private String supervisors;
    @XmlElement
    private String mailFromName;
    @XmlElement
    private String mailFrom;
    @XmlElement
    private String mailSubjectTime;
    @XmlElement
    private String mailSubjectInactive;
    @XmlElement
    private String mailSubjectEntry;
    @XmlElement
    private String mailBodyTime;
    @XmlElement
    private String mailBodyInactive;
    @XmlElement
    private String mailBodyEntry;

    public JsonConfig() {

    }

    public JsonConfig(ConfigService configService) {
        Config toCopy = configService.getConfiguration();

        Map<String, JsonTeam> teamMap = new TreeMap<String, JsonTeam>();
        for (Team team : toCopy.getTeams()) {
            teamMap.put(team.getTeamName(), new JsonTeam(team, configService));
        }

        this.teams = new ArrayList<JsonTeam>();
        this.teams.addAll(teamMap.values());

        this.approvedUsers = new ArrayList<String>();
        for (ApprovedUser approvedUser : toCopy.getApprovedUsers()) {
            if (approvedUser.getUserKey() != null) {
                approvedUsers.add(approvedUser.getUserName());
            }
        }

        this.approvedGroups = new ArrayList<String>();
        for (ApprovedGroup approvedGroup : toCopy.getApprovedGroups()) {
            approvedGroups.add(approvedGroup.getGroupName());
        }

        if(toCopy.getSupervisedUsers() != null) {
            this.supervisors = toCopy.getSupervisedUsers();
        } else {
            supervisors = "Select Supervisors";
        }

        this.mailFromName = toCopy.getMailFromName();
        this.mailFrom = toCopy.getMailFrom();

        this.mailSubjectTime = toCopy.getMailSubjectTime();
        this.mailSubjectInactive = toCopy.getMailSubjectInactive();
        this.mailSubjectEntry = toCopy.getMailSubjectEntry();

        this.mailBodyTime = toCopy.getMailBodyTime();
        this.mailBodyInactive = toCopy.getMailBodyInactive();
        this.mailBodyEntry = toCopy.getMailBodyEntry();
    }

    public List<JsonTeam> getTeams() {
        return teams;
    }

    public void setTeams(List<JsonTeam> teams) {
        this.teams = teams;
    }

    public List<String> getApprovedGroups() {
        return approvedGroups;
    }

    public void setApprovedGroups(List<String> approvedGroups) {
        this.approvedGroups = approvedGroups;
    }

    public List<String> getApprovedUsers() {
        return approvedUsers;
    }

    public void setApprovedUsers(List<String> approvedUsers) {
        this.approvedUsers = approvedUsers;
    }

    public String getSupervisors() {
        return supervisors;
    }

    public void setSupervisors(String supervisors) {
        this.supervisors = supervisors;
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
}
