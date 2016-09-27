package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.servlet.ImportTimesheetCsvServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
public class ImportAllTimesheetsServletTest {

    String test_key = "test_key";
    private ImportTimesheetCsvServlet importTimesheetCsvServlet;
    private EntityManager entityManager;
    private ActiveObjects ao;
    private LoginUriProvider loginUriProvider;
    private TemplateRenderer templateRenderer;
    private PermissionService permissionService;
    private WebSudoManager webSudoManager;
    private ConfigService configService;
    private ComponentAccessor componentAccessor;
    private TimesheetService timesheetService;
    private Config config;
    private CategoryService categoryService;
    private TeamService teamService;
    private TimesheetEntryService timesheetEntryService;
    private PrintWriter printWriter;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private ApplicationUser user;
    private ServletOutputStream outputStream;
    private CategoryService cs;
    private UserManager userManager;

    @Before
    public void setUp() throws Exception {
        ;

        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        configService = new ConfigServiceImpl(ao, cs, userManager);

        loginUriProvider = mock(LoginUriProvider.class);
        templateRenderer = mock(TemplateRenderer.class);
        webSudoManager = mock(WebSudoManager.class);
        permissionService = mock(PermissionService.class);
        componentAccessor = mock(ComponentAccessor.class);
        timesheetService = mock(TimesheetService.class);
        user = mock(ApplicationUser.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        outputStream = mock(ServletOutputStream.class);
        config = mock(Config.class);
        categoryService = mock(CategoryService.class);
        teamService = mock(TeamService.class);
        timesheetEntryService = mock(TimesheetEntryService.class);
        printWriter = mock(PrintWriter.class);

        importTimesheetCsvServlet = new ImportTimesheetCsvServlet(loginUriProvider, webSudoManager,
                configService, timesheetService, timesheetEntryService, ao, permissionService,
                categoryService, teamService);

        when(user.getUsername()).thenReturn("test");
        when(user.getKey()).thenReturn(test_key);

        when(permissionService.checkIfUserExists(request)).thenReturn(user);

        when(permissionService.checkIfUserIsGroupMember(request, "jira-administrators")).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember(request, "Timesheet")).thenReturn(true);

        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    public void testDoGet() throws Exception {
        importTimesheetCsvServlet.doGet(request, response);
    }

    @Test
    public void testDoPosNoDrop() throws Exception {
        String csvString = ">This\n" +
                "is\n" +
                "a\n" +
                "Test";

        Mockito.when(request.getParameter("csv")).thenReturn(csvString);
        Mockito.when(request.getParameter("drop")).thenReturn(null);

        importTimesheetCsvServlet.doPost(request, response);
    }
}
