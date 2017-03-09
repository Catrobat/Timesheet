package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.pagebuilder.PageBuilder;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.TSAdminGroup;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.CategoryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TeamServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetEntryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetServiceImpl;
import org.catrobat.jira.timesheet.servlet.ImportConfigAsJsonServlet;
import org.catrobat.jira.timesheet.servlet.ImportTimesheetAsJsonServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
public class JsonImporterServletTest {

    private LoginUriProvider loginUriProvider;
    private WebSudoManager webSudoManager;

    private ConfigService configService;

    private PermissionService permissionService;
    private TimesheetService timesheetService;
    private TimesheetEntryService entryService;
    private CategoryService categoryService;
    private TeamService teamService;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private EntityManager entityManager;
    private ActiveObjects ao;
    private TemplateRenderer renderer;
    private PageBuilderService pageBuilderService;

    @Before
    public void setup() throws IOException, PermissionException {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        loginUriProvider = mock(LoginUriProvider.class);
        webSudoManager = mock(WebSudoManager.class);

        configService = mock(ConfigService.class);
        Config config = mock(Config.class);

        permissionService = mock(PermissionService.class);
        timesheetService = new TimesheetServiceImpl(ao);
        entryService = new TimesheetEntryServiceImpl(ao, timesheetService);
        categoryService = new CategoryServiceImpl(ao);
        teamService = new TeamServiceImpl(ao, entryService);

        ApplicationUser user = mock(ApplicationUser.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        renderer = mock(TemplateRenderer.class);
        pageBuilderService = mock(PageBuilderService.class);

        when(user.getUsername()).thenReturn("chris");
        when(user.getKey()).thenReturn("chris");
        when(permissionService.checkIfUserExists()).thenReturn(user);
        when(permissionService.getLoggedInUser()).thenReturn(user);

        when(configService.getConfiguration()).thenReturn(config);
        when(config.getTimesheetAdminUsers()).thenReturn(new TimesheetAdmin[0]);
        when(config.getTimesheetAdminGroups()).thenReturn(new TSAdminGroup[0]);
        when(config.getTeams()).thenReturn(new Team[0]);

        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
    }

    @Test
    public void testDoGetTimesheet() throws IOException, ServletException {
        ImportTimesheetAsJsonServlet importTimesheetAsJsonServlet = new ImportTimesheetAsJsonServlet(loginUriProvider,
                webSudoManager, permissionService, configService, ao, timesheetService, entryService, categoryService, teamService,
                renderer, pageBuilderService);

        importTimesheetAsJsonServlet.doGet(request, response);
    }

    @Test
    public void testDoGetConfig() throws IOException, ServletException {
        ImportConfigAsJsonServlet importConfigAsJsonServlet = new ImportConfigAsJsonServlet(loginUriProvider,
                webSudoManager, configService, teamService, ao, permissionService, renderer, categoryService);

        importConfigAsJsonServlet.doGet(request, response);
    }
}
