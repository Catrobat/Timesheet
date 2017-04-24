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
import net.java.ao.Implementation;
import org.catrobat.jira.timesheet.activeobjects.impl.TimesheetEntryImpl;

import java.util.Date;

@Implementation(TimesheetEntryImpl.class)
public interface TimesheetEntry extends Entity {

    Timesheet getTimeSheet();
    void setTimeSheet(Timesheet sheet);

    Date getBeginDate();
    void setBeginDate(Date date);

    Date getEndDate();
    void setEndDate(Date date);

    Category getCategory();
    void setCategory(Category category);

    boolean getIsGoogleDocImport();
    void setIsGoogleDocImport(boolean isGoogleDocImport);

    String getDescription();
    void setDescription(String description);

    int getPauseMinutes();
    void setPauseMinutes(int pause);

    Team getTeam();
    void setTeam(Team team);

    int getDurationMinutes();
    void setDurationMinutes(int duration);

    Date getInactiveEndDate();
    void setInactiveEndDate(Date date);

    String getPairProgrammingUserName();
    void setPairProgrammingUserName(String userName);

    String getJiraTicketID();
    void setJiraTicketID(String ticketID);
}
