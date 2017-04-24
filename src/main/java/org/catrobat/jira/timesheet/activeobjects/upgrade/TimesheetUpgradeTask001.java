package org.catrobat.jira.timesheet.activeobjects.upgrade;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.CategoryToTeam;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.TeamToGroup;

public class TimesheetUpgradeTask001 implements ActiveObjectsUpgradeTask {

    @Override
    public ModelVersion getModelVersion() {
        return ModelVersion.valueOf("1");
    }

    @Override
    public void upgrade(ModelVersion currentVersion, ActiveObjects ao) {
        CategoryToTeam[] categoryToTeamList = ao.find(CategoryToTeam.class);
        for (CategoryToTeam categoryToTeam : categoryToTeamList) {
            if (categoryToTeam.getCategory() == null || categoryToTeam.getTeam() == null) {
                ao.delete(categoryToTeam);
            }
        }
        TeamToGroup[] teamToGroupList = ao.find(TeamToGroup.class);
        for (TeamToGroup teamToGroup : teamToGroupList) {
            if (teamToGroup.getTeam() == null || teamToGroup.getGroup() == null || teamToGroup.getRole() == null) {
                ao.delete(teamToGroup);
            }
        }
        Category[] categoryList = ao.find(Category.class);
        for (Category category : categoryList) {
            if (category.getName() == null) {
                ao.delete(category);
                continue;
            }
            Category[] duplicates = ao.find(Category.class,"NAME = ?", category.getName());
            for (int i = 1; i < duplicates.length; i++) {
                ao.delete(duplicates[i]);
            }
        }
        Team[] teamList = ao.find(Team.class);
        for (Team team : teamList) {
            if (team.getTeamName() == null) {
                ao.delete(team);
                continue;
            }
            Team[] duplicates = ao.find(Team.class,"TEAM_NAME = ?", team.getTeamName());
            for (int i = 1; i < duplicates.length; i++) {
                ao.delete(duplicates[i]);
            }
        }

        ao.migrate(CategoryToTeam.class);
        ao.migrate(TeamToGroup.class);
        ao.migrate(Category.class);
        ao.migrate(Team.class);
    }
}
