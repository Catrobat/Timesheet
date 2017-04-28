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
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;

import net.java.ao.DBParam;
import net.java.ao.Query;

import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class TeamServiceImpl implements TeamService {

    private final ActiveObjects ao;
    private final CategoryService categoryService;
    private final TimesheetEntryService entryService;

    private boolean isInitialised = false;
    private static final String DEFAULT_TEAM = "Default";

    public TeamServiceImpl(ActiveObjects ao, CategoryService categoryService, TimesheetEntryService entryService) {
        this.ao = ao;
        this.categoryService = categoryService;
        this.entryService = entryService;
    }

    private void initIfNotAlready() {
        if (!isInitialised) {
            Team[] found = ao.find(Team.class, "TEAM_NAME = ?", DEFAULT_TEAM);
            if (found.length == 0) {
                ao.create(Team.class,
                    new DBParam("TEAM_NAME", DEFAULT_TEAM)
                );
            }
            isInitialised = true;
        }
    }

    @Override
    public Team add(String name) {
        Team team = ao.create(Team.class,
            new DBParam("TEAM_NAME", name)
        );

        return team;
    }

    @Override
    public void removeTeam(String name) throws ServiceException {
        initIfNotAlready();
        if (name.equals(DEFAULT_TEAM)) {
            throw new ServiceException("This is the default team that cannot be deleted!");
        }
        Team[] found = ao.find(Team.class, "TEAM_NAME = ?", name);

        if(found.length == 0){
            throw new ServiceException("Team not found.");
        }

        Team team = found[0];
        if (team.getGroups().length > 0) {
            throw new ServiceException("Team still has Users belonging to it.");
        }

        CategoryToTeam[] categoryToTeamArray = ao.find(CategoryToTeam.class, Query.select().where("\"TEAM_ID\" = ?", team.getID()));
        for (CategoryToTeam categoryToTeam : categoryToTeamArray) {
            if (categoryToTeam.getTeam() != null) {
                ao.delete(categoryToTeam);
            }
        }

        Team[] defaultTeam = ao.find(Team.class, "TEAM_NAME = ?", DEFAULT_TEAM);
        entryService.replaceTeamInEntries(team, defaultTeam[0]);

        ao.delete(found);
    }

    @Override
    public List<Team> all() {
        initIfNotAlready();
        return newArrayList(ao.find(Team.class, Query.select().order("TEAM_NAME ASC")));
    }

    @Override
    public Team getTeamByID(int id) {
        return ao.get(Team.class, id);
    }

    @Override
    public Team getTeamByName(String name) throws ServiceException {
        initIfNotAlready();
        Team[] found = ao.find(Team.class, "TEAM_NAME = ?", name);

        return (found.length == 1) ? found[0] : null;
    }

    @Override
    public Set<Team> getTeamsOfUser(String userName) {
        initIfNotAlready();
        Set<Team> teams = new HashSet<>();

        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(userName);

        // TODO: improve me, this code is bad
        for (Team team : ao.find(Team.class)) {
            String teamName = team.getTeamName();

            List<String> developerUserAndGroupList = getGroupsForRole(teamName, TeamToGroup.Role.DEVELOPER);

            for (String developerUserOrGroupName : developerUserAndGroupList) {
                if (developerUserOrGroupName.equals(userName) ||
                        ComponentAccessor.getGroupManager().isUserInGroup(applicationUser,developerUserOrGroupName)) {
                    teams.add(team);
                    break;
                }
            }
        }
        return teams;
    }

    @Override
    public Set<Team> getTeamsOfCoordinator(String coordinatorsName) {
        initIfNotAlready();
        Set<Team> teams = new HashSet<>();

        System.out.println("checking for coordinators");

        for (Team team : ao.find(Team.class)) {
            String teamName = team.getTeamName();

            List<String> coordinatorAndCoordinatorGroupList = getGroupsForRole(teamName, TeamToGroup.Role.COORDINATOR);
            System.out.println("coordinator for team: " + teamName + " is " + coordinatorAndCoordinatorGroupList);

            for(String coordinatorNameOrGroup : coordinatorAndCoordinatorGroupList){
                if(ComponentAccessor.getUserManager().getUserByName(coordinatorNameOrGroup) == null){
                    Collection<String> coordinatorNames =
                            ComponentAccessor.getGroupManager().getUserNamesInGroup(coordinatorNameOrGroup);
                    for(String coordinatorName : coordinatorNames){
                        if(coordinatorName.equals(coordinatorsName)){
                            teams.add(team);
                        }
                    }
                }
                else{
                    if(coordinatorNameOrGroup.equals(coordinatorsName)){
                        teams.add(team);
                    }
                }
            }
        }
        return teams;
    }

    @Override
    public List<String> getGroupsForRole(String teamName, TeamToGroup.Role role) {
        List<String> groupList = new ArrayList<>();
        TeamToGroup[] teamToGroupArray = ao.find(TeamToGroup.class, Query.select()
                .where("\"ROLE\" = ?", role)
        );

        for (TeamToGroup teamToGroup : teamToGroupArray) {
            if (teamToGroup.getTeam().getTeamName().toLowerCase().equals(teamName.toLowerCase())) {
                groupList.add(teamToGroup.getGroup().getGroupName());
            }
        }

        return groupList;
    }

    @Override
    public void editTeam(String teamName, List<String> coordinatorGroups, List<String> developerGroups,
                         List<String> teamCategoryNames) throws ServiceException{

        teamName = teamName.trim();

        Team[] teamArray = ao.find(Team.class, Query.select().where("upper(\"TEAM_NAME\") = upper(?)", teamName));

        if (teamArray.length == 0) {
        	throw new ServiceException("Team not found. Maybe it was deleted. Please refresh the page and try again.");
        }
        
        Team team;
        if (teamArray[0].getGroups() == null) {
            team = add(teamName);
        } else {
            team = teamArray[0];
        }

        updateTeamMember(team, TeamToGroup.Role.COORDINATOR, coordinatorGroups);
        updateTeamMember(team, TeamToGroup.Role.DEVELOPER, developerGroups);

        updateTeamCategory(team, teamCategoryNames);

        team.save();
    }

    @Override
    public void checkIfCategoryIsAssociatedWithTeam(@Nullable Team team, @Nullable Category category) throws ServiceException {
        if (team == null) {
            throw new ServiceException("Team not found.");
        } else if (category == null) {
            throw new ServiceException("Category not found.");
        } else if (!Arrays.asList(team.getCategories()).contains(category)) {
            throw new ServiceException("Category is not associated with Team.");
        }
    }

    private void updateTeamMember(Team team, TeamToGroup.Role role, List<String> userList) {
        if (userList == null) {
            return;
        }

        // remove no longer existing relations
        TeamToGroup[] allTeamToGroups = ao.find(TeamToGroup.class, Query.select().where("\"TEAM_ID\" = ? AND \"ROLE\" = ?", team.getID(), role));
        for (TeamToGroup oldTeamRelation : allTeamToGroups) {
            if (!userList.contains(oldTeamRelation.getGroup().getGroupName())) {
                ao.delete(oldTeamRelation);
            }
        }

        for (String userName : userList) {
            Group[] groups = ao.find(Group.class, Query.select().where("upper(\"GROUP_NAME\") = upper(?)", userName));

            Group group;
            if (groups.length == 0) {
                group = ao.create(Group.class);
                group.setGroupName(userName);
                group.save();
            } else {
                group = groups[0];
            }

            // teamToGroup for one group
            TeamToGroup[] teamToGroups = ao.find(TeamToGroup.class, Query.select().where("\"TEAM_ID\" = ? AND \"GROUP_ID\" = ? AND \"ROLE\" = ?", team.getID(), group.getID(), role));

            // add new relation
            if (teamToGroups.length == 0) {
                ao.create(TeamToGroup.class,
                    new DBParam("GROUP_ID", group),
                    new DBParam("TEAM_ID", team),
                    new DBParam("ROLE", role)
                );
            }
        }
    }

    private void updateTeamCategory(Team team, List<String> categoryList) {
        if (categoryList == null) {
            return;
        }

        // remove no longer existing relations
        CategoryToTeam[] allCategoryToTeam = ao.find(CategoryToTeam.class, Query.select().where("\"TEAM_ID\" = ?", team.getID()));
        for (CategoryToTeam oldTeamRelation : allCategoryToTeam) {
            if (!categoryList.contains(oldTeamRelation.getCategory().getName())) {
                ao.delete(oldTeamRelation);
            }
        }

        for (String categoryName : categoryList) {
            Category category = categoryService.getCategoryByName(categoryName);

            //categoryToTeam for one category
            CategoryToTeam[] categoryToTeamArray = ao.find(CategoryToTeam.class, Query.select().where("\"TEAM_ID\" = ? AND \"CATEGORY_ID\" = ?", team.getID(), category.getID()));

            //update relation
            if (categoryToTeamArray.length == 0) {
                ao.create(CategoryToTeam.class,
                    new DBParam("TEAM_ID", team),
                    new DBParam("CATEGORY_ID", category)
                );
            }
        }
    }
}
