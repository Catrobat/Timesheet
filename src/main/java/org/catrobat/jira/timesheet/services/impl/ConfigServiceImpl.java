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

package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.ApplicationUser;
import net.java.ao.Query;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.TeamService;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigServiceImpl implements ConfigService {

    private final ActiveObjects ao;
    private final CategoryService cs;
    private final TeamService teamService;

    public ConfigServiceImpl(ActiveObjects ao, CategoryService cs, TeamService teamService) {
        this.ao = ao;
        this.cs = cs;
        this.teamService = teamService;
    }

    @Override
    public Config editMail(String mailFromName, String mailFrom, String mailSubjectTime,
            String mailSubjectInactive, String mailSubjectOffline, String mailSubjectActive, String mailSubjectEntry,
            String mailBodyTime, String mailBodyInactive, String mailBodyOffline, String mailBodyActive, String mailBodyEntry) {
        Config config = getConfiguration();
        config.setMailFromName(mailFromName);
        config.setMailFrom(mailFrom);

        config.setMailSubjectTime(mailSubjectTime);
        config.setMailSubjectInactiveState(mailSubjectInactive);
        config.setMailSubjectOfflineState(mailSubjectOffline);
        config.setMailSubjectActiveState(mailSubjectActive);
        config.setMailSubjectEntry(mailSubjectEntry);

        config.setMailBodyTime(mailBodyTime);
        config.setMailBodyInactiveState(mailBodyInactive);
        config.setMailBodyOfflineState(mailBodyOffline);
        config.setMailBodyActiveState(mailBodyActive);
        config.setMailBodyEntry(mailBodyEntry);

        config.save();

        return config;
    }

    @Override
    public Config editReadOnlyUsers(String readOnlyUsers) {
        Config config = getConfiguration();
        config.setReadOnlyUsers(readOnlyUsers);

        config.save();

        return config;
    }

    @Override
    public Config getConfiguration() {
        Config[] config = ao.find(Config.class);
        if (config.length == 0) {
            ao.create(Config.class).save();
            config = ao.find(Config.class);
        }

        return config[0];
    }

    @Override
    public Team addTeam(String teamName, List<String> coordinatorGroups, List<String> developerGroups,
            List<String> teamCategoryNames) {
        if (teamName == null || teamName.trim().length() == 0) {
            return null;
        }
        teamName = teamName.trim();

        Team[] teamArray = ao.find(Team.class, Query.select().where("upper(\"TEAM_NAME\") = upper(?)", teamName));
        if (teamArray.length != 0) {
            return null;
        }

        Config configuration = getConfiguration();
        Team team = ao.create(Team.class);
        team.setConfiguration(configuration);
        team.setTeamName(teamName);

        fillTeam(team, TeamToGroup.Role.COORDINATOR, coordinatorGroups);
        fillTeam(team, TeamToGroup.Role.DEVELOPER, developerGroups);

        fillCategory(team, teamCategoryNames);
        fillCategory(team, SpecialCategories.DefaultCategories);
        team.save();

        return team;
    }

    private void fillCategory(Team team, List<String> categoryList) {
        if (categoryList == null) {
            return;
        }

        for (String categoryName : categoryList) {
            Category category = cs.getCategoryByName(categoryName);
            CategoryToTeam mapper = ao.create(CategoryToTeam.class);
            mapper.setTeam(team);
            mapper.setCategory(category);
            mapper.save();
        }
    }

    private void updateTeamCategory(Team team, List<String> categoryList) {
        if (categoryList == null) {
            return;
        }

        Set<CategoryToTeam> addedRelations = new HashSet<>();

        for (String categoryName : categoryList) {
            Category category = cs.getCategoryByName(categoryName);
            if (category != null) {
                category.setName(categoryName);
                category.save();
            }

            //categoryToTeam for one category
            CategoryToTeam[] categoryToTeamArray = ao.find(CategoryToTeam.class, Query.select().where("\"CATEGORY_ID\" = ?", category != null ? category.getID() : 0));

            //update relation
            CategoryToTeam categoryToTeam;
            if ((categoryToTeamArray.length == 0) || (categoryToTeamArray[0].getTeam().getTeamName().equals(team.getTeamName()))) {
                categoryToTeam = ao.create(CategoryToTeam.class);
            } else {
                categoryToTeam = categoryToTeamArray[0];
            }

            categoryToTeam.setTeam(team);
            categoryToTeam.setCategory(category);
            categoryToTeam.save();

            addedRelations.add(categoryToTeam);
        }

        //update all existing categoryToTeam relations of a team
        CategoryToTeam[] allCategoryToTeam = ao.find(CategoryToTeam.class, Query.select().where("\"TEAM_ID\" = ?", team.getID()));
        for (CategoryToTeam oldTeamRelation : allCategoryToTeam) {
            if (!addedRelations.contains(oldTeamRelation)) {
                ao.delete(oldTeamRelation);
            }
        }
    }

    private void fillTeam(Team team, TeamToGroup.Role role, List<String> teamList) {
        if (teamList == null) {
            return;
        }

        for (String groupName : teamList) {
            Group[] groupArray = ao.find(Group.class, Query.select().where("upper(\"GROUP_NAME\") = upper(?)", groupName));

            Group group;
            if (groupArray.length == 0) {
                group = ao.create(Group.class);
            } else {
                group = groupArray[0];
            }
            group.setGroupName(groupName);
            group.save();

            TeamToGroup mapper = ao.create(TeamToGroup.class);
            mapper.setGroup(group);
            mapper.setTeam(team);
            mapper.setRole(role);
            mapper.save();
        }
    }

    private void updateTeamMember(Team team, TeamToGroup.Role role, List<String> userList) {
        if (userList == null) {
            return;
        }

        //comparison list of TeamToGroups
        Set<TeamToGroup> addedRelations = new HashSet<>();

        for (String userName : userList) {
            Group[] groups = ao.find(Group.class, Query.select().where("upper(\"GROUP_NAME\") = upper(?)", userName));

            Group group;
            if (groups.length == 0) {
                group = ao.create(Group.class);
            } else {
                group = groups[0];
            }
            group.setGroupName(userName);
            group.save();

            //teamToGroup for one group
            TeamToGroup[] teamToGroups = ao.find(TeamToGroup.class, Query.select().where("\"GROUP_ID\" = ?", group.getID()));

            //update relation
            TeamToGroup teamToGroup;
            if ((teamToGroups.length == 0) || (teamToGroups[0].getRole() != role) || (teamToGroups[0].getTeam().getTeamName().equals(team.getTeamName()))) {
                teamToGroup = ao.create(TeamToGroup.class);
            } else {
                teamToGroup = teamToGroups[0];
            }

            teamToGroup.setGroup(group);
            teamToGroup.setTeam(team);
            teamToGroup.setRole(role);
            teamToGroup.save();

            //actually added TeamToGroup relation
            addedRelations.add(teamToGroup);
        }

        //retrieve all existing relations of a team
        TeamToGroup[] allTeamToGroups = ao.find(TeamToGroup.class, Query.select().where("\"TEAM_ID\" = ? AND \"ROLE\" = ?", team.getID(), role));
        //update all existing categoryToTeam relations of a team
        for (TeamToGroup oldTeamRelation : allTeamToGroups) {
            if (!addedRelations.contains(oldTeamRelation)) {
                ao.delete(oldTeamRelation);
            }
        }
    }

    @Override
    public void clearTimesheetAdminGroups() {
        for (TSAdminGroup timesheetAdminGroup : ao.find(TSAdminGroup.class)) {
            ao.delete(timesheetAdminGroup);
        }
    }

    @Override
    public void clearTimesheetAdmins() {
        for (TimesheetAdmin timesheetAdmin : ao.find(TimesheetAdmin.class)) {
            ao.delete(timesheetAdmin);
        }
    }

    @Override
    public Response editTeamName(String oldTeamName, String newTeamName) {
        if (oldTeamName == null || newTeamName == null) {
            return Response.status(Response.Status.FORBIDDEN).entity("Old team name or new team name does not exist.").build();
        }

        Team[] tempTeamArray = ao.find(Team.class, Query.select().where("upper(\"TEAM_NAME\") = upper(?)", oldTeamName));
        if (tempTeamArray.length == 0) {
            return Response.status(Response.Status.FORBIDDEN).entity("Old team name could not be found.").build();
        }
        Team team = tempTeamArray[0];

        tempTeamArray = ao.find(Team.class, Query.select().where("upper(\"TEAM_NAME\") = upper(?)", newTeamName));
        if (tempTeamArray.length > 0) {
            return Response.status(Response.Status.FORBIDDEN).entity("New team name already exist. Please take another name.").build();
        }

        team.setTeamName(newTeamName);
        team.save();
        return Response.ok().build();
    }

    @Override
    public void editTeam(String teamName, List<String> coordinatorGroups, List<String> developerGroups,
            List<String> teamCategoryNames) {

        teamName = teamName.trim();

        Team[] teamArray = ao.find(Team.class, Query.select().where("upper(\"TEAM_NAME\") = upper(?)", teamName));

        if (teamArray.length == 0) {
            return;
        }
        if (teamArray[0].getGroups() == null) {
            addTeam(teamName, coordinatorGroups, developerGroups, teamCategoryNames);
            return;
        }

        Team team = teamArray[0];

        updateTeamMember(team, TeamToGroup.Role.COORDINATOR, coordinatorGroups);
        updateTeamMember(team, TeamToGroup.Role.DEVELOPER, developerGroups);

        updateTeamCategory(team, teamCategoryNames);

        team.save();
    }

    @Override
    public Config editCategoryName(String oldCategoryName, String newCategoryName) {
        if (oldCategoryName == null || newCategoryName == null) {
            return null;
        }

        if (cs.getCategoryByName(newCategoryName) != null) {
            return null;
        }

        Category category = cs.getCategoryByName(oldCategoryName);
        if (category == null) {
            return null;
        }
        category.setName(newCategoryName);
        category.save();

        return getConfiguration();
    }

    @Override
    public List<String> getGroupsForRole(String teamName, TeamToGroup.Role role) {
        // TODO: move to teamService
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
    public TSAdminGroup addTimesheetAdminGroup(String groupName) {
        if (groupName == null || groupName.trim().length() == 0) {
            return null;
        }
        groupName = groupName.trim();

        TSAdminGroup[] timesheetAdminGroupArray = ao.find(TSAdminGroup.class, Query.select()
                .where("upper(\"GROUP_NAME\") = upper(?)", groupName));
        if (timesheetAdminGroupArray.length == 0) {
            return createTimesheetAdminGroup(groupName);
        } else {
            return createTimesheetAdminGroup(groupName);
        }
    }

    private TSAdminGroup createTimesheetAdminGroup(String timesheetAdminGroupName) {
        TSAdminGroup timesheetAdminGroup = ao.create(TSAdminGroup.class);
        timesheetAdminGroup.setGroupName(timesheetAdminGroupName);
        timesheetAdminGroup.setConfiguration(getConfiguration());
        timesheetAdminGroup.save();

        return timesheetAdminGroup;
    }

    @Override
    public int[] getCategoryIDsForTeam(String teamName) {
        Team[] team = ao.find(Team.class, "TEAM_NAME = ?", teamName);

        if (team.length != 1) {
            return null;
        }

        Category[] categories = team[0].getCategories();
        int[] categoryIDs = new int[categories.length];
        for (int i = 0; i < categories.length; i++) {
            categoryIDs[i] = categories[i].getID();
        }

        return categoryIDs;
    }

    @Override
    public List<String> getCategoryNamesForTeam(String teamName) {
        Team[] team = ao.find(Team.class, "TEAM_NAME = ?", teamName);
        if (team.length == 0) {
            return null;
        }
        List<String> categoryList = new ArrayList<>();

        for (Category category : team[0].getCategories()) {
            categoryList.add(category.getName());
        }

        return categoryList;
    }

    @Override
    public TimesheetAdmin addTimesheetAdmin(ApplicationUser user) {
        String userKey = user.getKey();
        if (userKey == null || userKey.trim().length() == 0) {
            return null;
        }
        userKey = userKey.trim();

        TimesheetAdmin[] timesheetAdminArray = ao.find(TimesheetAdmin.class, Query.select()
                .where("upper(\"USER_KEY\") = upper(?)", userKey));
        if (timesheetAdminArray.length == 0) {
            return createTimesheetAdmin(user);
        } else {
            return timesheetAdminArray[0];
        }
    }

    private TimesheetAdmin createTimesheetAdmin(ApplicationUser user) {
        String userKey = user.getKey();

        TimesheetAdmin timesheetAdmin = ao.create(TimesheetAdmin.class);
        timesheetAdmin.setUserKey(userKey);
        timesheetAdmin.setUserName(user.getUsername());
        timesheetAdmin.setEmailAddress(user.getEmailAddress());
        timesheetAdmin.setFullName(user.getDisplayName());
        timesheetAdmin.setConfiguration(getConfiguration());
        timesheetAdmin.save();

        return timesheetAdmin;
    }

    @Override
    public boolean isTimesheetAdminGroup(String groupName) {
        if (groupName != null) {
            groupName = groupName.trim();
        }

        return (ao.find(TSAdminGroup.class).length == 0 && ao.find(TimesheetAdmin.class).length == 0) ||
                ao.find(TSAdminGroup.class, Query.select()
                        .where("upper(\"GROUP_NAME\") = upper(?)", groupName)).length != 0;
    }

    @Override
    public boolean isTimesheetAdmin(String userKey) {
        if (userKey != null) {
            userKey = userKey.trim();
        }

        return ao.find(TimesheetAdmin.class, Query.select().where("upper(\"USER_KEY\") = upper(?)", userKey)).length != 0;
    }

    @Override
    public Config removeTimesheetAdminGroup(String groupName) {
        if (groupName != null) {
            groupName = groupName.trim();
        }

        TSAdminGroup[] timesheetAdminGroupArray = ao.find(TSAdminGroup.class, Query.select()
                .where("upper(\"GROUP_NAME\") = upper(?)", groupName));
        if (timesheetAdminGroupArray.length == 0) {
            return null;
        }
        ao.delete(timesheetAdminGroupArray[0]);

        return getConfiguration();
    }

    @Override
    public Config removeTimesheetAdmin(String TimesheetAdminKey) {
        if (TimesheetAdminKey != null) {
            TimesheetAdminKey = TimesheetAdminKey.trim();
        }

        TimesheetAdmin[] timesheetAdminArray = ao.find(TimesheetAdmin.class, Query.select()
                .where("upper(\"USER_KEY\") = upper(?)", TimesheetAdminKey));
        if (timesheetAdminArray.length == 0) {
            return null;
        }
        ao.delete(timesheetAdminArray[0]);

        return getConfiguration();
    }

    @Override
    public Config editPairProgrammingGroup(String pairProgrammingGroup) {
        Config config = getConfiguration();
        config.setPairProgrammingGroup(pairProgrammingGroup);

        config.save();

        return config;
    }
}
