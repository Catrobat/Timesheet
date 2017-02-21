package org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.gson.Gson;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.rest.json.JsonConfig;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

public class ImportConfigAsJsonServlet extends HighPrivilegeServlet {

    private final ConfigService configService;
    private final TeamService teamService;
    private final ActiveObjects activeObjects;
    private final TemplateRenderer renderer;

    public ImportConfigAsJsonServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                     ConfigService configService, TeamService teamService,
                                     ActiveObjects activeObjects, PermissionService permissionService, TemplateRenderer renderer) {
        super(loginUriProvider, webSudoManager, permissionService, configService);
        this.configService = configService;
        this.teamService = teamService;
        this.activeObjects = activeObjects;
        this.renderer = renderer;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        // Dangerous servlet - should be forbidden in production use
        /*
        if (configService.getConfiguration().getTeams().length != 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "The Configuration - Import is not possible if teams exist");
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
                "<input type=\"checkbox\" name=\"drop\" value=\"drop\">Drop existing config settings and ALL timesheet entries<br /><br />\n" +
                "<input type=\"submit\" />" +
                "</form>" +
                "</body>" +
                "</html>");
        writer.flush();
        writer.close();
        //renderer.render("upload_timesheet.vm", response.getWriter());
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);

        // Dangerous servlet - should be forbidden in production use
        /*
        if (configService.getConfiguration().getTeams().length != 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "The Configuration - Import is not possible if teams exist");
            return;
        }
        */

        String jsonString = request.getParameter("json");

        if (request.getParameter("drop") != null && request.getParameter("drop").equals("drop")) {
            // FIXME: do we even need dropping here???
            dropEntries();
        }

        String errorString = "";

        Gson gson = new Gson();
        JsonConfig jsonConfig = gson.fromJson(jsonString, JsonConfig.class);

        jsonToConfig(jsonConfig, configService);

        response.getWriter().print("Successfully executed following string:<br />" +
                "<textarea rows=\"20\" cols=\"200\" wrap=\"off\" disabled>" + jsonString + "</textarea>" +
                "<br /><br />" +
                "Following errors occurred:<br />" + errorString);
    }

    private void jsonToConfig(JsonConfig jsonConfig, ConfigService configService) throws ServletException {
        // TODO: copy paste from Config REST, remove at one place

        configService.editMail(jsonConfig.getMailFromName(), jsonConfig.getMailFrom(),
                jsonConfig.getMailSubjectTime(), jsonConfig.getMailSubjectInactive(),
                jsonConfig.getMailSubjectOffline(), jsonConfig.getMailSubjectActive(), jsonConfig.getMailSubjectEntry(), jsonConfig.getMailBodyTime(),
                jsonConfig.getMailBodyInactive(), jsonConfig.getMailBodyOffline(), jsonConfig.getMailBodyActive(), jsonConfig.getMailBodyEntry());

        configService.editReadOnlyUsers(jsonConfig.getReadOnlyUsers());
        configService.editPairProgrammingGroup(jsonConfig.getPairProgrammingGroup());

        //clear fields
        configService.clearTimesheetAdminGroups();
        configService.clearTimesheetAdmins();

        // add TimesheetAdmin group
        if (jsonConfig.getTimesheetAdminGroups() != null) {
            for (String approvedGroupName : jsonConfig.getTimesheetAdminGroups()) {
                configService.addTimesheetAdminGroup(approvedGroupName);
                // add all users in group
                Collection<ApplicationUser> usersInGroup = ComponentAccessor.getGroupManager().getUsersInGroup(approvedGroupName);
                for (ApplicationUser user : usersInGroup) {
                    configService.addTimesheetAdmin(user);
                }
            }
        }

        // add TimesheetAdmins
        if (jsonConfig.getTimesheetAdmins() != null) {
            for (String username : jsonConfig.getTimesheetAdmins()) {
                ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(username);
                if (user != null) {
                    configService.addTimesheetAdmin(user);
                    //RestUtils.getInstance().printUserInformation(username, user);
                }
            }
        }

        if (jsonConfig.getTeams() != null) {
            for (JsonTeam jsonTeam : jsonConfig.getTeams()) {
                try {
                    if (teamService.getTeamByName(jsonTeam.getTeamName()) != null) {
                        configService.editTeam(jsonTeam.getTeamName(), jsonTeam.getCoordinatorGroups(),
                                jsonTeam.getDeveloperGroups(), jsonTeam.getTeamCategoryNames());
                    } else {
                        configService.addTeam(jsonTeam.getTeamName(), jsonTeam.getCoordinatorGroups(),
                                jsonTeam.getDeveloperGroups(), jsonTeam.getTeamCategoryNames());
                    }
                } catch (ServiceException e) {
                    throw new ServletException(e);
                }
            }
        }
    }

    private void dropEntries() {
        activeObjects.deleteWithSQL(TimesheetEntry.class, "1=?", "1");
        activeObjects.deleteWithSQL(TSAdminGroup.class, "1=?", "1");
        activeObjects.deleteWithSQL(TimesheetAdmin.class, "1=?", "1");
        activeObjects.deleteWithSQL(CategoryToTeam.class, "1=?", "1");
        activeObjects.deleteWithSQL(Category.class, "1=?", "1");
        activeObjects.deleteWithSQL(TeamToGroup.class, "1=?", "1");
        activeObjects.deleteWithSQL(Team.class, "1=?", "1");
        activeObjects.deleteWithSQL(Group.class, "1=?", "1");
        activeObjects.deleteWithSQL(Config.class, "1=?", "1");
    }
}
