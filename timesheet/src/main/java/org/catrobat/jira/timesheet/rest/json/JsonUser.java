/*
 * Copyright 2014 Stephan Fellhofer
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@SuppressWarnings("unused")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class JsonUser {
  @XmlElement
  private String userName;
  @XmlElement
  private String firstName;
  @XmlElement
  private String lastName;
  @XmlElement
  private String email;
  @XmlElement
  private String displayName;
  @XmlElement
  private List<String> coordinatorList;
  @XmlElement
  private List<String> seniorList;
  @XmlElement
  private List<String> developerList;
  @XmlElement
  private boolean active;

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public List<String> getCoordinatorList() {
    return coordinatorList;
  }

  public void setCoordinatorList(List<String> coordinatorList) {
    this.coordinatorList = coordinatorList;
  }

  public List<String> getSeniorList() {
    return seniorList;
  }

  public void setSeniorList(List<String> seniorList) {
    this.seniorList = seniorList;
  }

  public List<String> getDeveloperList() {
    return developerList;
  }

  public void setDeveloperList(List<String> developerList) {
    this.developerList = developerList;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}