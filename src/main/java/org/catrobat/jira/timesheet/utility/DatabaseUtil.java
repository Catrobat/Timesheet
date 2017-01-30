package org.catrobat.jira.timesheet.utility;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for DatabaseUtil issues
 */
public class DatabaseUtil {

    private final ActiveObjects ao;

    public DatabaseUtil(ActiveObjects ao) {
        this.ao = ao;
    }

    public void clearAllTimesheetTables() {
        ao.deleteWithSQL(TimesheetEntry.class, "ID > ?", 0);
        ao.deleteWithSQL(Timesheet.class, "ID > ?", 0);
        ao.deleteWithSQL(TimesheetAdmin.class, "ID > ?", 0);
        ao.deleteWithSQL(TSAdminGroup.class, "ID > ?", 0);

        ao.deleteWithSQL(CategoryToTeam.class, "ID > ?", 0);
        ao.deleteWithSQL(TeamToGroup.class, "ID > ?", 0);
        ao.deleteWithSQL(Category.class, "ID > ?", 0);
        ao.deleteWithSQL(Team.class, "ID > ?", 0);
        ao.deleteWithSQL(Config.class, "ID > ?", 0);
        ao.deleteWithSQL(Group.class, "ID > ?", 0);
        ao.deleteWithSQL(Scheduling.class, "ID > ?", 0);

        System.out.println("All timesheet tables has been deleted!");
    }

    public List getActiveObjectsAsJson(String tableName, String query) {
        if (query.equals("*")) {
            query = "ID >= 0";
        } else {
            query = "ID = " + query;
        }
        List jsonList = new ArrayList();
        switch (tableName) {
            case "Timesheet":
                Timesheet[] timesheets = ao.find(Timesheet.class, query);
                for (Timesheet timesheet : timesheets) {
                    jsonList.add(new JsonTimesheet(timesheet));
                } break;
            case "TimesheetEntry":
                TimesheetEntry[] timesheetEntries = ao.find(TimesheetEntry.class, query);
                for (TimesheetEntry timesheetEntry : timesheetEntries) {
                    jsonList.add(new JsonTimesheetEntry(timesheetEntry));
                } break;
        }
        return jsonList;
    }
}
