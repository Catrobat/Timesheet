package org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.google.gson.Gson;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetAndEntries;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class ImportTimesheetAsJsonServlet extends HighPrivilegeServlet {

    private final ActiveObjects activeObjects;
    private final TimesheetService timesheetService;
    private final TimesheetEntryService timesheetEntryService;
    private final CategoryService categoryService;
    private final TeamService teamService;

    public ImportTimesheetAsJsonServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
            PermissionService permissionService, ConfigService configService, ActiveObjects activeObjects,
            TimesheetService timesheetService, TimesheetEntryService timesheetEntryService,
            CategoryService categoryService, TeamService teamService) {
        super(loginUriProvider, webSudoManager, permissionService, configService);
        this.activeObjects = activeObjects;
        this.timesheetService = timesheetService;
        this.timesheetEntryService = timesheetEntryService;
        this.categoryService = categoryService;
        this.teamService = teamService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        // Dangerous servlet - should be forbidden in production use
        /*if (!timesheetService.all().isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Importing Timesheets is not possible if timesheets exist");
            return;
        }*/

        PrintWriter writer = response.getWriter();
        writer.print("<html>" +
                "<body>" +
                "<h1>Dangerzone!</h1>" +
                "Just upload files when you know what you're doing - this upload will manipulate the database!<br />" +
                "<form action=\"json\" method=\"post\"><br />" +
                "<textarea name=\"json\" rows=\"20\" cols=\"175\" wrap=\"off\">" +
                "</textarea><br />\n" +
                "<input type=\"checkbox\" name=\"drop\" value=\"drop\">Drop existing timesheets and entries<br /><br />\n" +
                "<input type=\"submit\" />" +
                "</form>" +
                "</body>" +
                "</html>");
        writer.flush();
        writer.close();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);

        // Dangerous servlet - should be forbidden in production use
        /*if (!timesheetService.all().isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Importing Timesheets is not possible if timesheets exist");
            return;
        }*/

        String jsonString = request.getParameter("json");

        if (request.getParameter("drop") != null && request.getParameter("drop").equals("drop")) {
            dropEntries();
        }

        Gson gson = new Gson();
        JsonTimesheetAndEntries[] timesheetAndEntriesList = gson.fromJson(jsonString, JsonTimesheetAndEntries[].class);

        if (timesheetAndEntriesList == null) {
            response.getWriter().print("No Json given");
            return;
        }

        String errorString = "";

        for (JsonTimesheetAndEntries timesheetAndEntries : timesheetAndEntriesList) {
            JsonTimesheet jsonTimesheet = timesheetAndEntries.getJsonTimesheet();
            List<JsonTimesheetEntry> timesheetEntryList = timesheetAndEntries.getJsonTimesheetEntryList();

            Timesheet sheet = timesheetService.add(jsonTimesheet.getUserKey(), jsonTimesheet.getTargetHourPractice(), jsonTimesheet.getTargetHourTheory(),
                    jsonTimesheet.getTargetHours(), jsonTimesheet.getTargetHoursCompleted(), jsonTimesheet.getTargetHoursRemoved(),
                    jsonTimesheet.getLectures(), jsonTimesheet.getReason(),
                    jsonTimesheet.isActive(), jsonTimesheet.isOffline(), jsonTimesheet.isMTSheet(), jsonTimesheet.isEnabled());

            System.out.println("sheetid: " + sheet.getID());
            for (JsonTimesheetEntry entry : timesheetEntryList) {
                Category category = categoryService.getCategoryByID(entry.getCategoryID());
                Team team;
                System.out.println(entry.getEntryID());
                try {
                    // TODO: check service exception
                    team = teamService.getTeamByID(entry.getTeamID());
                } catch (ServiceException e) {
                    errorString += "Team with ID " + entry.getTeamID() + " not found. Entry #" + entry.getEntryID() + " not ignored.";
                    continue;
                }
                // FIXME: verify that team and entry is not null
                timesheetEntryService.add(sheet, entry.getBeginDate(), entry.getEndDate(), category, entry.getDescription(),
                        entry.getPauseMinutes(), team, entry.IsGoogleDocImport(), entry.getInactiveEndDate(), entry.getDeactivateEndDate(),
                        entry.getTicketID(), entry.getPairProgrammingUserName());
            }
        }

        response.getWriter().print("Successfully executed following string:<br />" +
                "<textarea rows=\"20\" cols=\"200\" wrap=\"off\" disabled>" + jsonString + "</textarea>" +
                "<br /><br />" +
                "Following errors occurred:<br />" + errorString);
    }

    private void dropEntries() {
        activeObjects.deleteWithSQL(TimesheetEntry.class, "1=?", "1");
        activeObjects.deleteWithSQL(Timesheet.class, "1=?", "1");
    }
}
