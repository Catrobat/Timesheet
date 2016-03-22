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

package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.service.ServiceException;
import net.java.ao.Query;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.TeamToGroup;
import org.catrobat.jira.timesheet.services.TeamService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

public class TeamServiceImpl implements TeamService {

  private final ActiveObjects ao;
  private final GroupManager groupManager;
  private final ConfigService configService;

  public TeamServiceImpl(ActiveObjects ao, GroupManager gm, ConfigService cs) {
    this.ao = ao;
    this.groupManager = gm;
    this.configService = cs;
  }

  @Override
  public Team add(String name) {
    Team team = ao.create(Team.class);
    team.setTeamName(name);
    team.save();

    return team;
  }

  @Override
  public boolean removeTeam(String name) throws ServiceException {
    Team[] found = ao.find(Team.class, "TEAM_NAME = ?", name);

    if (found.length > 1) {
      throw new ServiceException("Multiple Teams with the same Name");
    }

    ao.delete(found);
    return true;
  }

  @Override
  public List<Team> all() {
    return newArrayList(ao.find(Team.class, Query.select().order("TEAM_NAME ASC")));
  }

  @Override
  public Team getTeamByID(int id) throws ServiceException {
    Team[] found = ao.find(Team.class, "ID = ?", id);

    if (found.length > 1) {
      throw new ServiceException("Multiple Teams with the same ID");
    }

    return (found.length > 0) ? found[0] : null;
  }

  @Override
  public Team getTeamByName(String name) throws ServiceException {
    Team[] found = ao.find(Team.class, "TEAM_NAME = ?", name);

    if (found.length > 1) {
      throw new ServiceException("Multiple Teams with the same Name");
    }

    return (found.length > 0) ? found[0] : null;
  }

  //LDAP access
  /*
  @Override
  public Set<Team> getTeamsOfUser(String userName) {

    Set<Team> teams = new HashSet<Team>();

    for (String groupName : userAccessor.getGroupNamesForUserName(userName)) {

      String teamName = groupName.split("-")[0];
      Team team = getTeamByName(teamName);
      if (team != null) {
        teams.add(team);
      }
    }
    return teams;
  }
  */

  //Config access
  @Override
  public Set<Team> getTeamsOfUser(String userName) {

    Set<Team> teams = new HashSet<Team>();
    Config config = configService.getConfiguration();

    for (Team team : config.getTeams()) {
      String teamName = team.getTeamName();

      List<String> developerList = configService.getGroupsForRole(teamName, TeamToGroup.Role.DEVELOPER);

      for(String developerName : developerList) {
        if(developerName.compareTo(userName) == 0) {
          if (team != null) {
            teams.add(team);
          }
        }
      }
    }
    return teams;
  }

  //LDAP access
  /*
  @Override
  public Set<Team> getCoordinatorTeamsOfUser(String userName) {
    Set<Team> teams = new HashSet<Team>();
    for (String groupName : userAccessor.getGroupNamesForUserName(userName)) {
      String[] pieces = groupName.split("-");

      String teamName = pieces[0];
      String roleName = (pieces.length > 1) ? pieces[1].toLowerCase() : "";

      if (!roleName.equalsIgnoreCase("administrators")
              && !roleName.equalsIgnoreCase("coordinators")) {
        continue;
      }

      Team team = getTeamByName(teamName);
      if (team != null) {
        teams.add(team);
      }
    }

    return teams;
  }
  */

  //Config access
  @Override
  public Set<Team> getCoordinatorTeamsOfUser(String userName) {

    Set<Team> teams = new HashSet<Team>();
    Config config = configService.getConfiguration();

    for (Team team : config.getTeams()) {
      String teamName = team.getTeamName();

      List<String> coordinatorList = configService.getGroupsForRole(teamName, TeamToGroup.Role.COORDINATOR);

      for(String coordinatorName : coordinatorList) {
        if(coordinatorName.compareTo(userName) == 0) {
          if (team != null) {
            teams.add(team);
          }
        }
      }
    }
    return teams;
  }
}
