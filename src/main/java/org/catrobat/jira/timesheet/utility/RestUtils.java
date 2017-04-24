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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class RestUtils {

    public static List<Team> asSortedList(Collection<Team> c) {
        List<Team> list = new ArrayList<>(c);
        list.sort((Comparator.comparing(Team::getTeamName)));
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
        } else if (sheet.getState() == Timesheet.State.DISABLED) {
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
        Instant instant = entry.getInactiveEndDate().toInstant();
        ZonedDateTime dataTime = instant.atZone(ZoneId.systemDefault());
        ZonedDateTime twoMonthsAhead = ZonedDateTime.now().plusMonths(2);

        RestUtils.checkJsonTimesheetEntry(entry);
        if (dataTime.isAfter(twoMonthsAhead)) {
            throw new ParseException("The 'Inactive End Date' is more than 2 months ahead. This is too far away.");
        } else if ((entry.getInactiveEndDate().compareTo(entry.getBeginDate()) > 0) &&
                !(categoryService.getCategoryByID(entry.getCategoryID()).getName().equals("Inactive") ||
                        categoryService.getCategoryByID(entry.getCategoryID()).getName().equals("Inactive & Offline") )) {
            throw new ParseException("You also have to select the 'Inactive' Category for a valid 'Inactive-Entry'.");
        }
    }

    /* Notice: Just for information
     * -----------------------------------------------------------------------------------------------------------------

    public static void printUserInformation(String username, ApplicationUser user) {
        ApplicationUser userByName = ComponentAccessor.getUserManager().getUserByName(username);
        Assert.isTrue(userByName.equals(user),"Users not equal!");

        System.out.println();
        System.out.println("user.getEmailAddress()       = " + user.getEmailAddress());
        System.out.println("userByName.getEmailAddress() = " + userByName.getEmailAddress());

        System.out.println("userByName.getName()         = " + userByName.getName());
        System.out.println("userByName.getUsername()     = " + userByName.getUsername());
        System.out.println("user.getUsername()           = " + user.getUsername());
        System.out.println("username()           = " + username);

        System.out.println("userByName.getDisplayName()  = " + userByName.getDisplayName());
        System.out.println("user.getDisplayName()        = " + user.getDisplayName());

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("userByName.getKey()          = " + userByName.getKey());
        System.out.println("########################################################################");
    }
    */
}
