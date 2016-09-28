package org.catrobat.jira.timesheet.rest;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;

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

    public TreeSet<ApplicationUser> getSortedUsers(Set<ApplicationUser> allUsers) {

        Comparator<ApplicationUser> userComparator = new Comparator<ApplicationUser>() {

            @Override
            public int compare(ApplicationUser o1, ApplicationUser o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };

        TreeSet<ApplicationUser> users = new TreeSet<ApplicationUser>(userComparator);

        for (ApplicationUser user : allUsers) {
            users.add(user);
        }
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
}
