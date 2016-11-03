package org.catrobat.jira.timesheet.rest;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;

import java.util.*;

public class RestUtils {

    private static class InstanceHolder {
        private static final RestUtils instance = new RestUtils();
    }

    public static RestUtils getInstance() {
        return InstanceHolder.instance;
    }

    public TreeSet<ApplicationUser> getSortedUsers(Set<ApplicationUser> allUsers) {
        Comparator<ApplicationUser> userComparator = (o1, o2) -> o1.getName().compareTo(o2.getName());
        TreeSet<ApplicationUser> users = new TreeSet<>(userComparator);
        users.addAll(allUsers);
        return users;
    }

    public void printAllUsers(TreeSet<User> allUsers) {
        for (User user : allUsers) {
            System.out.println(user.getName());
        }
    }

    public void printUserInformation(String approvedUserName, ApplicationUser user) {
        ApplicationUser userByName = ComponentAccessor.getUserManager().getUserByName(approvedUserName);

        System.out.println();
        System.out.println("user.getEmailAddress()       = " + user.getEmailAddress());
        System.out.println("userByName.getEmailAddress() = " + userByName.getEmailAddress());

        System.out.println("userByName.getName()         = " + userByName.getName());
        System.out.println("userByName.getUsername()     = " + userByName.getUsername());
        System.out.println("user.getUsername()           = " + user.getUsername());
        System.out.println("approvedUserName()           = " + approvedUserName);

        System.out.println("userByName.getDisplayName()  = " + userByName.getDisplayName());
        System.out.println("user.getDisplayName()        = " + user.getDisplayName());

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("userByName.getKey()          = " + userByName.getKey());
        System.out.println("########################################################################");
    }

    public static List<Team> asSortedList(Collection<Team> c) {
        List<Team> list = new ArrayList<>(c);
        Collections.sort(list, ((o1, o2) -> o1.getTeamName().compareTo(o2.getTeamName())));
        return list;
    }

    public static List<JsonTeam> convertTeamsToJSON(List<Team> sortedTeamsOfUsersList) {
        List<JsonTeam> teams = new LinkedList<>();
        for (Team team : sortedTeamsOfUsersList) {
            Category[] categories = team.getCategories();
            List<Integer> categoryIDs = new ArrayList<>();
            for (int i = 0; i < categories.length; i++) {
                categoryIDs.add(categories[i].getID());
            }
            teams.add(new JsonTeam(team.getID(), team.getTeamName(), categoryIDs));
        }
        return teams;
    }
}
