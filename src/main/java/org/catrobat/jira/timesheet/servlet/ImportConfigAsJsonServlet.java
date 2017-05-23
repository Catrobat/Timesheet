package org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.rest.json.JsonConfig;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class ImportConfigAsJsonServlet extends HighPrivilegeServlet {

    private final ConfigService configService;
    private final TeamService teamService;
    private final ActiveObjects activeObjects;
    private final TemplateRenderer renderer;
    private final CategoryService categoryService;
    private static final Logger logger = LoggerFactory.getLogger(ImportConfigAsJsonServlet.class);

    public ImportConfigAsJsonServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                     ConfigService configService, TeamService teamService,
                                     ActiveObjects activeObjects, PermissionService permissionService, TemplateRenderer renderer,
                                     CategoryService categoryService) {
        super(loginUriProvider, webSudoManager, permissionService, configService);
        this.configService = configService;
        this.teamService = teamService;
        this.activeObjects = activeObjects;
        this.renderer = renderer;
        this.categoryService = categoryService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        renderer.render("upload.vm", response.getWriter());
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);

        boolean isMultipartContent = ServletFileUpload.isMultipartContent(request);
        if (!isMultipartContent) {
            response.sendError(500, "An error occurred: no files were given!");
            return;
        }

        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        File temp = File.createTempFile("backup_config", ".json");

        try {
            List<FileItem> fields = upload.parseRequest(request);
            Iterator<FileItem> it = fields.iterator();
            if (!it.hasNext()) {
                return;
            }
            if (fields.size() != 1) {
                response.sendError(500, "An error occurred: You may only upload one file!");
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
        JsonConfig jsonConfig = gson.fromJson(jsonReader, JsonConfig.class);
        String errorString = "";

        jsonToConfig(jsonConfig, configService);

        response.getWriter().print("Successfully executed following string:<br />" +
                "<textarea rows=\"20\" cols=\"200\" wrap=\"off\" disabled>" + gson.toJson(jsonConfig) + "</textarea>" +
                "<br /><br />" +
                "Following errors occurred:<br />" + errorString);
    }

    private void jsonToConfig(JsonConfig jsonConfig, ConfigService configService) throws ServletException {
        // TODO: copy paste from Config REST, remove at one place
        dropEntries();

        configService.editMail(jsonConfig.getMailFromName(), jsonConfig.getMailFrom(),
                jsonConfig.getMailSubjectTime(), jsonConfig.getMailSubjectInactive(),
                jsonConfig.getMailSubjectOffline(), jsonConfig.getMailSubjectActive(), jsonConfig.getMailSubjectEntry(), jsonConfig.getMailBodyTime(),
                jsonConfig.getMailBodyInactive(), jsonConfig.getMailBodyOffline(), jsonConfig.getMailBodyActive(), jsonConfig.getMailBodyEntry());

        configService.editReadOnlyUsers(jsonConfig.getReadOnlyUsers());
        configService.editPairProgrammingGroup(jsonConfig.getPairProgrammingGroup());

        configService.editTimesheetAdmins(jsonConfig.getTimesheetAdmins());

        for(JsonTeam jsonTeam : jsonConfig.getTeams())
        {
            for(String cat : jsonTeam.getTeamCategoryNames())
            {
                if(!SpecialCategories.AllSpecialCategories.contains(cat) && categoryService.getCategoryByName(cat) == null)
                {
                    logger.info("category: {} does not exist, creating", cat);
                    try{
                        categoryService.add(cat);
                    }
                    catch (ServiceException e) {
                      e.printStackTrace();
                    }
                }
            }
            configService.addTeam(jsonTeam.getTeamName(), jsonTeam.getCoordinatorGroups(), jsonTeam.getDeveloperGroups(),
                    jsonTeam.getTeamCategoryNames());
        }

    }

    private void dropEntries() {
        activeObjects.deleteWithSQL(TimesheetEntry.class, "1=?", 1);
        activeObjects.deleteWithSQL(TimesheetAdmin.class, "1=?", 1);
        activeObjects.deleteWithSQL(CategoryToTeam.class, "1=?", 1);
        activeObjects.deleteWithSQL(Category.class, "1=?", 1);
        activeObjects.deleteWithSQL(TeamToGroup.class, "1=?", 1);
        activeObjects.deleteWithSQL(Team.class, "1=?", 1);
        activeObjects.deleteWithSQL(Group.class, "1=?", 1);
        activeObjects.deleteWithSQL(Config.class, "1=?", 1);
    }
}
