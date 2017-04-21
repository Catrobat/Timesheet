package org.catrobat.jira.timesheet.servlet;

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
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetAndEntries;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ImportTimesheetAsJsonServlet extends HighPrivilegeServlet {

    private final TimesheetService timesheetService;
    private final TimesheetEntryService timesheetEntryService;
    private final CategoryService categoryService;
    private final TeamService teamService;
    private final TemplateRenderer renderer;

    private enum Result{Success, Failure, Errors}
    private Result importResult;

    public ImportTimesheetAsJsonServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                        PermissionService permissionService, ConfigService configService,
                                        TimesheetService timesheetService, TimesheetEntryService timesheetEntryService,
                                        CategoryService categoryService, TeamService teamService, TemplateRenderer renderer) {
        super(loginUriProvider, webSudoManager, permissionService, configService);
        this.timesheetService = timesheetService;
        this.timesheetEntryService = timesheetEntryService;
        this.categoryService = categoryService;
        this.teamService = teamService;
        this.renderer = renderer;
        this.importResult = Result.Success;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);
        renderer.render("upload.vm", response.getWriter());
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);

        Map<String, List<Map<String,String>>> faulty_teams = new HashMap<>();

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
                    jsonTimesheet.isMTSheet(), jsonTimesheet.getState());

            for (JsonTimesheetEntry entry : timesheetEntryList) {
                try {
                    System.out.println("Entry cat: " + entry.getCategoryName());
                    System.out.println("Entry team: " + entry.getTeamName());
                    Category category = categoryService.getCategoryByName(entry.getCategoryName());
                    if (category == null) {
                        System.out.println("Category in import does not exist, need to set Default Category");
                        category = categoryService.getCategoryByName(SpecialCategories.DEFAULT);
                    }
                    Team team = teamService.getTeamByName(entry.getTeamName());
                    List<Team> teams = teamService.all();
                    for(Team t : teams) {
                        System.out.println(t.getTeamName());
                    }
                    if (team == null) {
                        importResult = Result.Errors;
                        System.out.println("Team given in import does not exist");

                        Map<String, String> map = new HashMap<>();
                        map.put("ID" , Integer.toString(entry.getEntryID()));
                        map.put("Entry", entry.getBeginDate() + "-" + entry.getEndDate());
                        map.put("Team", entry.getTeamName());
                        map.put("Purpose", entry.getDescription());

                        if(!faulty_teams.containsKey(entry.getTeamName())) {
                            faulty_teams.put(entry.getTeamName(), new ArrayList<>());
                        }
                        faulty_teams.get(entry.getTeamName()).add(map);

                        continue;
                    }
                    timesheetEntryService.add(sheet, entry.getBeginDate(), entry.getEndDate(), category, entry.getDescription(),
                            entry.getPauseMinutes(), team, entry.IsGoogleDocImport(), entry.getInactiveEndDate(),
                            entry.getTicketID(), entry.getPairProgrammingUserName());

                } catch (ServiceException e) {
                    errorString += e.getMessage() + " Entry ignored.\n";
                    continue;
                }
            }
        }
        Map<String, Object> params = new HashMap<>();

        params.put("status", importResult);
        params.put("error_teams", gson.toJson(faulty_teams));
        renderer.render("upload_result.vm", params, response.getWriter());
    }
}
