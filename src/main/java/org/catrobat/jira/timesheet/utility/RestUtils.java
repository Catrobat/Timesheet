package org.catrobat.jira.timesheet.utility;

import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.joda.time.DateTime;

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
            for (Category category : categories) {
                categoryIDs.add(category.getID());
            }
            teams.add(new JsonTeam(team.getID(), team.getTeamName(), categoryIDs));
        }
        return teams;
    }

    public static void checkTimesheetIsEnabled(Timesheet sheet) throws PermissionException {
        if (sheet == null) {
            throw new PermissionException("Timesheet not found.");
        } else if (!sheet.getIsEnabled()) {
            throw new PermissionException("Your timesheet has been disabled.");
        }
    }

    public static void checkTimesheetIsEditableByUser(ApplicationUser user, Timesheet sheet, PermissionService permissionService) throws PermissionException {
        RestUtils.checkTimesheetIsEnabled(sheet);
        if (!permissionService.userCanEditTimesheet(user, sheet)) {
            throw new PermissionException("You are not allowed to edit the timesheet.");
        }
    }

    public static void checkJsonTimesheetEntry(JsonTimesheetEntry entry) throws ParseException{
        if (entry == null) {
            throw new ParseException("The entry must not be null");
        } else if (entry.getDescription().isEmpty()) {
            throw new ParseException("The 'Task Description' field must not be empty.");
        } else if ((entry.getInactiveEndDate().compareTo(entry.getBeginDate()) < 0)) {
            throw new ParseException("The 'Inactive End Date' is before your 'Entry Date'. That is not possible. The begin date is " + entry.getBeginDate() +
                    " but your inactive end date is " + entry.getInactiveEndDate());
        }
    }

    public static void checkJsonTimesheetEntryAndCategory(JsonTimesheetEntry entry, CategoryService categoryService) throws ParseException{
        Date twoMonthsAhead = new DateTime().plusMonths(2).toDate();

        RestUtils.checkJsonTimesheetEntry(entry);
        if (entry.getInactiveEndDate().compareTo(twoMonthsAhead) > 0) {
            throw new ParseException("The 'Inactive End Date' is more than 2 months ahead. This is too far away.");
        } else if ((entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0) &&
                (!categoryService.getCategoryByID(entry.getCategoryID()).getName().equals("Inactive"))) {
            throw new ParseException("You also have to select the 'Inactive' Category for a valid 'Inactive-Entry'.");
        } else if (entry.getDeactivateEndDate().compareTo(entry.getBeginDate()) < 0) {
            throw new ParseException("The 'Inactive & Offline End Date' is before your 'Entry Date'. That is not possible. The begin date is " + entry.getBeginDate() +
                    " but your inactive end date is " + entry.getDeactivateEndDate());
        }
    }
}
