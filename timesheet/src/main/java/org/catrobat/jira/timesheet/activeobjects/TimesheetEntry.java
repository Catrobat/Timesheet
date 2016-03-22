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

  public Timesheet getTimeSheet();

  public void setTimeSheet(Timesheet sheet);

  public Date getBeginDate();

  public void setBeginDate(Date date);

  public Date getEndDate();

  public void setEndDate(Date date);

  public Category getCategory();

  public void setCategory(Category category);

  public boolean getIsGoogleDocImport();

  public void setIsGoogleDocImport(boolean isGoogleDocImport);

  public String getDescription();

  public void setDescription(String description);

  public int getPauseMinutes();

  public void setPauseMinutes(int pause);

  public Team getTeam();

  public void setTeam(Team team);

  public int getDurationMinutes();

  public void setDurationMinutes(int duration);
}
