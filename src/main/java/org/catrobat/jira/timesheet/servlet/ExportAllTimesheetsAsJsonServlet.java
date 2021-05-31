package org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ExportAllTimesheetsAsJsonServlet extends HighPrivilegeServlet {

    public static final String CONTENT_TYPE = "text/csv; charset=utf-8";
    private final TimesheetService sheetService;
    private final TimesheetEntryService entryService;
    private final TeamService teamService;
    private final CategoryService categoryService;
    private static final Logger logger = LoggerFactory.getLogger(ExportAllTimesheetsAsJsonServlet.class);

    public ExportAllTimesheetsAsJsonServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                            PermissionService permissionService, ConfigService configService,
                                            TimesheetService timesheetService, TimesheetEntryService entryService,
                                            CategoryService categoryService, TeamService teamService) {
        super(loginUriProvider, webSudoManager, permissionService, configService);
        this.sheetService = timesheetService;
        this.entryService = entryService;
        this.categoryService = categoryService;
        this.teamService = teamService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        if (response.getStatus() == HttpServletResponse.SC_MOVED_TEMPORARILY) {
            return;
        }

        ApplicationUser loggedInUser;
        try {
            loggedInUser = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            throw new ServletException(e);
        }
        Date actualDate =  new Date();
        String filename = "attachment; filename=\"" +
                actualDate.toString().substring(0,10) +
                "-" +
                actualDate.toString().substring(25,28) +
                "-" +
                loggedInUser.getUsername() +
                "_Timesheet_Timesheet.json\"";

        response.setContentType(CONTENT_TYPE);
        response.setHeader("Content-Disposition", filename);

        List<JsonTimesheetAndEntries> timesheetAndEntriesList = new LinkedList<>();

        List<Timesheet> timesheetList = sheetService.all();
        for (Timesheet timesheet : timesheetList) {
            TimesheetEntry[] entries = timesheet.getEntries();

            List<JsonTimesheetEntry> jsonEntries = new ArrayList<>();

            for (TimesheetEntry entry : entries) {
                JsonTimesheetEntry new_entry = new JsonTimesheetEntry(entry);
                Category cat = categoryService.getCategoryByID(new_entry.getCategoryID());
                Team team =  teamService.getTeamByID(new_entry.getTeamID());
                if(cat == null || team == null) {
                    logger.error("category or team not found should not happen on live system");
                    continue;
                }
                String category_name = cat.getName();
                String team_name = team.getTeamName();

                new_entry.setCategoryName(category_name);
                new_entry.setTeamName(team_name);

                jsonEntries.add(new_entry);
            }


            JsonTimesheetAndEntries jsonTimesheetAndEntries = new JsonTimesheetAndEntries(new JsonTimesheet(timesheet), jsonEntries);
            timesheetAndEntriesList.add(jsonTimesheetAndEntries);
        }

        Gson gson = new Gson();
        String jsonString = gson.toJson(timesheetAndEntriesList);

        PrintStream printStream = new PrintStream(response.getOutputStream(), false, "UTF-8");
        printStream.print(jsonString);
        printStream.flush();
        printStream.close();
    }
}
