package org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.google.gson.Gson;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetAndEntries;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;

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

    private final TimesheetService sheetService;
    private final TimesheetEntryService entryService;

    public ExportAllTimesheetsAsJsonServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                            PermissionService permissionService, ConfigService configService,
                                            TimesheetService timesheetService, TimesheetEntryService entryService) {
        super(loginUriProvider, webSudoManager, permissionService, configService);
        this.sheetService = timesheetService;
        this.entryService = entryService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

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

        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", filename);

        List<JsonTimesheetAndEntries> timesheetAndEntriesList = new LinkedList<>();

        List<Timesheet> timesheetList = sheetService.all();
        for (Timesheet timesheet : timesheetList) {
            TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);

            List<JsonTimesheetEntry> jsonEntries = new ArrayList<>();

            for (TimesheetEntry entry : entries) {
                jsonEntries.add(new JsonTimesheetEntry(entry));
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
