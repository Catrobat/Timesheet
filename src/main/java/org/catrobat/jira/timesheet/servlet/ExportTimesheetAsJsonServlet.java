package org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.gson.Gson;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetAndEntries;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExportTimesheetAsJsonServlet extends HttpServlet {

    private final LoginUriProvider loginUriProvider;
    private final TemplateRenderer templateRenderer;
    private final TimesheetService sheetService;
    private final PermissionService permissionService;
    private final TimesheetEntryService entryService;

    public ExportTimesheetAsJsonServlet(final LoginUriProvider loginUriProvider, final TemplateRenderer templateRenderer,
            final TimesheetService sheetService, final PermissionService permissionService, TimesheetEntryService timesheetEntryService) {
        this.loginUriProvider = loginUriProvider;
        this.templateRenderer = templateRenderer;
        this.sheetService = sheetService;
        this.permissionService = permissionService;
        this.entryService = timesheetEntryService;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Response unauthorized = permissionService.checkUserPermission();
        if (unauthorized != null) {
            response.sendError(HttpServletResponse.SC_CONFLICT, unauthorized.getEntity().toString());
        }
        super.service(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
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

        Timesheet timesheet = null;
        try {
            timesheet = sheetService.getTimesheetByUser(loggedInUser.getKey(), false);
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        if (!permissionService.userCanViewTimesheet(loggedInUser, timesheet)) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "You are not allowed to see the timesheet.");
        }

        TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);

        List<JsonTimesheetEntry> jsonEntries = new ArrayList<JsonTimesheetEntry>();

        for (TimesheetEntry entry : entries) {
            jsonEntries.add(new JsonTimesheetEntry(entry));
        }

        Gson gson = new Gson();
        String jsonString = gson.toJson(new JsonTimesheetAndEntries(new JsonTimesheet(timesheet), jsonEntries));

        PrintStream printStream = new PrintStream(response.getOutputStream(), false, "UTF-8");
        printStream.print(jsonString);
        printStream.flush();
        printStream.close();
    }
}
