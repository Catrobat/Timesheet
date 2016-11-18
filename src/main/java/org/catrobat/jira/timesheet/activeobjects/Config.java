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

package org.catrobat.jira.timesheet.activeobjects;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
@Preload
public interface Config extends Entity {

    String getMailFromName();

    void setMailFromName(String fromName);

    String getMailFrom();

    void setMailFrom(String from);

    @StringLength(StringLength.UNLIMITED)
    String getMailSubjectTime();

    @StringLength(StringLength.UNLIMITED)
    void setMailSubjectTime(String subject);

    @StringLength(StringLength.UNLIMITED)
    String getMailSubjectInactiveState();

    @StringLength(StringLength.UNLIMITED)
    String getMailSubjectOfflineState();

    @StringLength(StringLength.UNLIMITED)
    void setMailSubjectOfflineState(String subject);

    @StringLength(StringLength.UNLIMITED)
    String getMailSubjectActiveState();

    @StringLength(StringLength.UNLIMITED)
    void setMailSubjectActiveState(String subject);

    @StringLength(StringLength.UNLIMITED)
    void setMailSubjectInactiveState(String subject);

    @StringLength(StringLength.UNLIMITED)
    String getMailSubjectEntry();

    @StringLength(StringLength.UNLIMITED)
    void setMailSubjectEntry(String subject);

    @StringLength(StringLength.UNLIMITED)
    String getMailBodyTime();

    @StringLength(StringLength.UNLIMITED)
    void setMailBodyTime(String body);

    @StringLength(StringLength.UNLIMITED)
    String getMailBodyInactiveState();

    @StringLength(StringLength.UNLIMITED)
    void setMailBodyInactiveState(String body);

    @StringLength(StringLength.UNLIMITED)
    String getMailBodyOfflineState();

    @StringLength(StringLength.UNLIMITED)
    void setMailBodyOfflineState(String body);

    @StringLength(StringLength.UNLIMITED)
    String getMailBodyActiveState();

    @StringLength(StringLength.UNLIMITED)
    void setMailBodyActiveState(String body);

    @StringLength(StringLength.UNLIMITED)
    String getMailBodyEntry();

    @StringLength(StringLength.UNLIMITED)
    void setMailBodyEntry(String body);

    @StringLength(StringLength.UNLIMITED)
    String getReadOnlyUsers();

    @StringLength(StringLength.UNLIMITED)
    void setReadOnlyUsers(String readOnlyUsers);

    @OneToMany(reverse = "getConfiguration")
    TSAdminGroup[] getTimesheetAdminGroups();

    @OneToMany(reverse = "getConfiguration")
    TimesheetAdmin[] getTimesheetAdminUsers();

    @OneToMany(reverse = "getConfiguration")
    Team[] getTeams();
}
