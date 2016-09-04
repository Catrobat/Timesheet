package org.catrobat.jira.timesheet.rest.json;

import com.atlassian.crowd.embedded.api.User;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class RestUtils {

    private static class InstanceHolder {
        private static final RestUtils instance = new RestUtils();
    }

    public static RestUtils getInstance() {
        return InstanceHolder.instance;
    }

    public TreeSet<User> getSortedUsers(Set<User> allUsers) {

        Comparator<User> userComparator = new Comparator<User>() {

            @Override
            public int compare(User o1, User o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };

        TreeSet<User> users = new TreeSet<User>(userComparator);

        for (User user : allUsers) {
            users.add(user);
        }
        return users;
    }

    public void printAllUsers(TreeSet<User> allUsers){
        for (User user : allUsers) {
            System.out.println(user.getName());
        }

    }
}
