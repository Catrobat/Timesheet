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

package org.catrobat.jira.timesheet.activeobjects.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.ApplicationUser;
import net.java.ao.Query;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.CategoryService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigServiceImpl implements ConfigService {

    private final ActiveObjects ao;
    private final CategoryService cs;

    public ConfigServiceImpl(ActiveObjects ao, CategoryService cs) {
        this.ao = ao;
        this.cs = cs;
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
    public Config editSupervisedUsers(String supervisedUsers) {
        Config config = getConfiguration();
        config.setSupervisedUsers(supervisedUsers);

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

        Set<CategoryToTeam> addedRelations = new HashSet<CategoryToTeam>();

        for (String categoryName : categoryList) {
            Category category = cs.getCategoryByName(categoryName);
            category.setName(categoryName);
            category.save();

            //categoryToTeam for one category
            CategoryToTeam[] categoryToTeamArray = ao.find(CategoryToTeam.class, Query.select().where("\"CATEGORY_ID\" = ?", category.getID()));

            //update relation
            CategoryToTeam categoryToTeam;
            if ((categoryToTeamArray.length == 0) || (categoryToTeamArray[0].getTeam().getTeamName() != team.getTeamName())) {
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
                oldTeamRelation.setCategory(null);
                oldTeamRelation.setTeam(null);
                oldTeamRelation.save();
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
        Set<TeamToGroup> addedRelations = new HashSet<TeamToGroup>();

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
            if ((teamToGroups.length == 0) || (teamToGroups[0].getRole() != role) || (teamToGroups[0].getTeam().getTeamName()
                    != team.getTeamName())) {
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
                oldTeamRelation.setGroup(null);
                oldTeamRelation.setRole(null);
                oldTeamRelation.setTeam(null);
                oldTeamRelation.save();
            }
        }
    }

    @Override
    public void clearApprovedGroups() {
        for (ApprovedGroup approvedGroup : ao.find(ApprovedGroup.class)) {
            ao.delete(approvedGroup);
        }
    }

    @Override
    public void clearApprovedUsers() {
        for (ApprovedUser approvedUser : ao.find(ApprovedUser.class)) {
            ao.delete(approvedUser);
        }
    }

    @Override
    public Config editTeamName(String oldTeamName, String newTeamName) {
        if (oldTeamName == null || newTeamName == null) {
            return null;
        }

        Team[] tempTeamArray = ao.find(Team.class, Query.select().where("upper(\"TEAM_NAME\") = upper(?)", oldTeamName));
        if (tempTeamArray.length == 0) {
            return null;
        }
        Team team = tempTeamArray[0];

        tempTeamArray = ao.find(Team.class, Query.select().where("upper(\"TEAM_NAME\") = upper(?)", newTeamName));
        if (tempTeamArray.length != 0) {
            return null;
        }

        team.setTeamName(newTeamName);
        team.save();

        return getConfiguration();
    }

    @Override
    public Team editTeam(String teamName, List<String> coordinatorGroups, List<String> developerGroups,
            List<String> teamCategoryNames) {

        teamName = teamName.trim();

        Team[] teamArray = ao.find(Team.class, Query.select().where("upper(\"TEAM_NAME\") = upper(?)", teamName));
        if (teamArray[0].getGroups() == null) {
            Team team = addTeam(teamName, coordinatorGroups, developerGroups, teamCategoryNames);
            return team;
        }
        Team team = teamArray[0];

        updateTeamMember(team, TeamToGroup.Role.COORDINATOR, coordinatorGroups);
        updateTeamMember(team, TeamToGroup.Role.DEVELOPER, developerGroups);

        updateTeamCategory(team, teamCategoryNames);

        team.save();

        return team;
    }

    @Override
    public Config removeTeam(String teamName) {
        Team[] teamArray = ao.find(Team.class, Query.select().where("upper(\"TEAM_NAME\") = upper(?)", teamName));
        if (teamArray.length == 0) {
            return null;
        }
        Team team = teamArray[0];
        Group[] groupArray = team.getGroups();
        TeamToGroup[] teamToGroupArray = ao.find(TeamToGroup.class, Query.select().where("\"TEAM_ID\" = ?", team.getID()));
        for (TeamToGroup teamToGroup : teamToGroupArray) {
            ao.delete(teamToGroup);
        }

        for (Group group : groupArray) {
            if (group.getTeams().length == 0) {
                ao.delete(group);
            }
        }

        CategoryToTeam[] categoryToTeamArray = ao.find(CategoryToTeam.class, Query.select().where("\"TEAM_ID\" = ?", team.getID()));
        for (CategoryToTeam categoryToTeam : categoryToTeamArray)
            if (categoryToTeam.getTeam() != null) {
                ao.delete(categoryToTeam);
            }

        ao.delete(team);

        return getConfiguration();
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
        List<String> groupList = new ArrayList<String>();
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
    public ApprovedGroup addApprovedGroup(String approvedGroupName) {
        if (approvedGroupName == null || approvedGroupName.trim().length() == 0) {
            return null;
        }
        approvedGroupName = approvedGroupName.trim();

        ApprovedGroup[] approvedGroupArray = ao.find(ApprovedGroup.class, Query.select()
                .where("upper(\"GROUP_NAME\") = upper(?)", approvedGroupName));
        if (approvedGroupArray.length == 0) {
            return createApprovedGroup(approvedGroupName);
        } else {
            return createApprovedGroup(approvedGroupName);
        }
    }

    private ApprovedGroup createApprovedGroup(String approvedGroupName) {
        ApprovedGroup approvedGroup = ao.create(ApprovedGroup.class);
        approvedGroup.setGroupName(approvedGroupName);
        approvedGroup.setConfiguration(getConfiguration());
        approvedGroup.save();

        return approvedGroup;
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
        List<String> categoryList = new ArrayList<String>();

        for (Category category : team[0].getCategories()) {
            categoryList.add(category.getName());
        }

        return categoryList;
    }

    @Override
    public ApprovedUser addApprovedUser(ApplicationUser user) {
        String userKey = user.getKey();
        if (userKey == null || userKey.trim().length() == 0) {
            return null;
        }
        userKey = userKey.trim();

        ApprovedUser[] approvedUserArray = ao.find(ApprovedUser.class, Query.select()
                .where("upper(\"USER_KEY\") = upper(?)", userKey));
        if (approvedUserArray.length == 0) {
            return createApprovedUser(user);
        } else {
            return approvedUserArray[0];
        }
    }

    private ApprovedUser createApprovedUser(ApplicationUser user) {
        String userKey = user.getKey();

        ApprovedUser approvedUser = ao.create(ApprovedUser.class);
        approvedUser.setUserKey(userKey);
        approvedUser.setUserName(user.getUsername());
        approvedUser.setEmailAddress(user.getEmailAddress());
        approvedUser.setFullName(user.getDisplayName());
        approvedUser.setConfiguration(getConfiguration());
        approvedUser.save();

        return approvedUser;
    }

    @Override
    public boolean isGroupApproved(String groupName) {
        if (groupName != null) {
            groupName = groupName.trim();
        }

        return (ao.find(ApprovedGroup.class).length == 0 && ao.find(ApprovedUser.class).length == 0) ||
                ao.find(ApprovedGroup.class, Query.select()
                        .where("upper(\"GROUP_NAME\") = upper(?)", groupName)).length != 0;
    }

    @Override
    public boolean isUserApproved(String userKey) {
        if (userKey != null) {
            userKey = userKey.trim();
        }

        return ao.find(ApprovedUser.class, Query.select().where("upper(\"USER_KEY\") = upper(?)", userKey)).length != 0;
    }

    @Override
    public Config removeApprovedGroup(String approvedGroupName) {
        if (approvedGroupName != null) {
            approvedGroupName = approvedGroupName.trim();
        }

        ApprovedGroup[] approvedGroupArray = ao.find(ApprovedGroup.class, Query.select()
                .where("upper(\"GROUP_NAME\") = upper(?)", approvedGroupName));
        if (approvedGroupArray.length == 0) {
            return null;
        }
        ao.delete(approvedGroupArray[0]);

        return getConfiguration();
    }

    @Override
    public Config removeApprovedUser(String approvedUserKey) {
        if (approvedUserKey != null) {
            approvedUserKey = approvedUserKey.trim();
        }

        ApprovedUser[] approvedUserArray = ao.find(ApprovedUser.class, Query.select()
                .where("upper(\"USER_KEY\") = upper(?)", approvedUserKey));
        if (approvedUserArray.length == 0) {
            return null;
        }
        ao.delete(approvedUserArray[0]);

        return getConfiguration();
    }
}
