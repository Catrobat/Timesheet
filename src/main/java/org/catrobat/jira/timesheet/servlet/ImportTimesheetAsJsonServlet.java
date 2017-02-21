package org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ImportTimesheetAsJsonServlet extends HighPrivilegeServlet {

    private final ActiveObjects activeObjects;
    private final TimesheetService timesheetService;
    private final TimesheetEntryService timesheetEntryService;
    private final CategoryService categoryService;
    private final TeamService teamService;
    private final TemplateRenderer renderer;

    public ImportTimesheetAsJsonServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
            PermissionService permissionService, ConfigService configService, ActiveObjects activeObjects,
            TimesheetService timesheetService, TimesheetEntryService timesheetEntryService,
            CategoryService categoryService, TeamService teamService, TemplateRenderer renderer) {
        super(loginUriProvider, webSudoManager, permissionService, configService);
        this.activeObjects = activeObjects;
        this.timesheetService = timesheetService;
        this.timesheetEntryService = timesheetEntryService;
        this.categoryService = categoryService;
        this.teamService = teamService;
        this.renderer = renderer;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        // Dangerous servlet - should be forbidden in production use
        /*if (!timesheetService.all().isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Importing Timesheets is not possible if timesheets exist");
            return;
        }*/

       /* PrintWriter writer = response.getWriter();
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
        writer.close();*/
        renderer.render("upload_timesheet.vm", response.getWriter());
    }


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);

        System.out.println("new Import");
        boolean isMultipartContent = ServletFileUpload.isMultipartContent(request);
        if (!isMultipartContent) {
            response.sendError(500, "An error occurred: no files were given!");
            return;
        }

        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        File temp = File.createTempFile("backup", ".json");

        try {
            List<FileItem> fields = upload.parseRequest(request);
            Iterator<FileItem> it = fields.iterator();
            if (!it.hasNext()) {
                return;
            }
            if (fields.size() != 1) {
                response.sendError(500, "An error occurred: Only one File is allowed");
                return;
            }
            FileItem fileItem = it.next();
            if (!(fileItem.getContentType().equals("application/json"))){
                response.sendError(500, "An error occurred: you may only upload Json files");
                return;
            }
            fileItem.write(temp);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Gson gson = new Gson();

        JsonReader jsonReader = new JsonReader(new FileReader(new File(temp.getAbsolutePath())));
        JsonTimesheetAndEntries[] timesheetAndEntriesList = gson.fromJson(jsonReader, JsonTimesheetAndEntries[].class);

        if (timesheetAndEntriesList == null) {
            response.getWriter().print("No Json given");
            return;
        }

        String errorString = "";

        for (JsonTimesheetAndEntries timesheetAndEntries : timesheetAndEntriesList) {
            JsonTimesheet jsonTimesheet = timesheetAndEntries.getJsonTimesheet();
            List<JsonTimesheetEntry> timesheetEntryList = timesheetAndEntries.getJsonTimesheetEntryList();

            ApplicationUser jsonUser = ComponentAccessor.getUserManager().getUserByKey(jsonTimesheet.getUserKey());
            if (jsonUser == null) {
                // TODO: do we even need to check whether the user exists?
                errorString += "User with Key: " + jsonTimesheet.getUserKey() + " does not exists. Timesheet ignored.\n";
                continue;
            }
            Timesheet sheet = timesheetService.add(jsonTimesheet.getUserKey(), jsonTimesheet.getDisplayName(),
                    jsonTimesheet.getTargetHourPractice(), jsonTimesheet.getTargetHourTheory(),
                    jsonTimesheet.getTargetHours(), jsonTimesheet.getTargetHoursCompleted(),
                    jsonTimesheet.getTargetHoursRemoved(), jsonTimesheet.getLectures(), jsonTimesheet.getReason(),
                    jsonTimesheet.isMTSheet(), jsonTimesheet.isEnabled(), jsonTimesheet.getState());

            for (JsonTimesheetEntry entry : timesheetEntryList) {
                Category category = categoryService.getCategoryByID(entry.getCategoryID());
                if (category == null) {
                    errorString += "Category with ID " + entry.getCategoryID() + " not found. Entry #" + entry.getEntryID() + " not ignored.\n";
                    continue;
                }
                Team team = teamService.getTeamByID(entry.getTeamID());
                if (team == null) {
                    errorString += "Team with ID " + entry.getTeamID() + " not found. Entry #" + entry.getEntryID() + " not ignored.\n";
                    continue;
                }
                try {
                    timesheetEntryService.add(sheet, entry.getBeginDate(), entry.getEndDate(), category, entry.getDescription(),
                            entry.getPauseMinutes(), team, entry.IsGoogleDocImport(), entry.getInactiveEndDate(),
                            entry.getTicketID(), entry.getPairProgrammingUserName());
                } catch (ServiceException e) {
                    errorString += e.getMessage() + " Entry ignored.\n";
                    continue;
                }
            }
        }

        response.getWriter().print("Successfully executed following string:<br />" +
                "<br /><br />" +
                "Following errors occurred:<br />" + errorString);
    }

    private void dropEntries() {
        activeObjects.deleteWithSQL(TimesheetEntry.class, "1=?", "1");
        activeObjects.deleteWithSQL(Timesheet.class, "1=?", "1");
    }
}
