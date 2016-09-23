package org.catrobat.jira.timesheet.rest;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.user.UserProfile;

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

    public void printUserInformation(String approvedUserName, UserProfile userProfile) {
        ApplicationUser userByName = ComponentAccessor.getUserManager().getUserByName(approvedUserName);
        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(userProfile.getUsername());

        System.out.println();
        System.out.println("userProfile.getEmail()      = " + userProfile.getEmail());
        System.out.println("userByKey.getEmailAddress() = " + userByName.getEmailAddress());

        System.out.println("userByName.getName()        = " + userByName.getName());
        System.out.println("userByKey.getUsername()     = " + userByName.getUsername());
        System.out.println("userProfile.getUsername()   = " + userProfile.getUsername());
        System.out.println("approvedUserName()          = " + approvedUserName);

        System.out.println("userByKey.getDisplayName()  = " + userByName.getDisplayName());
        System.out.println("userProfile.getFullName()   = " + userProfile.getFullName());

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("userKey (CA) userKeyService = " + userKey);
        System.out.println("userByName.getKey()         = " + userByName.getKey());
        System.out.println("########################################################################");
    }
}
