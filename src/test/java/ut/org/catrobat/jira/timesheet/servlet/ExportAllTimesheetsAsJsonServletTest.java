package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.mock.servlet.MockHttpServletRequest;
import com.atlassian.jira.mock.servlet.MockHttpServletResponse;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.servlet.ExportAllTimesheetsAsJsonServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class ExportAllTimesheetsAsJsonServletTest {

    private LoginUriProvider loginUriProvider;
    private WebSudoManager webSudoManager;

    private ConfigService configService;

    private PermissionService permissionService;
    private TimesheetService timesheetService;
    private TimesheetEntryService entryService;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private CategoryService categoryService;
    private TeamService teamService;

    private ExportAllTimesheetsAsJsonServlet exportAllTimesheetsAsJsonServlet;
    private ApplicationUser user;

    @Before
    public void setup() throws PermissionException{
        loginUriProvider = mock(LoginUriProvider.class);
        webSudoManager = mock(WebSudoManager.class);

        configService = mock(ConfigService.class);
        Config config = mock(Config.class);

        permissionService = mock(PermissionService.class);
        timesheetService = mock(TimesheetService.class);
        entryService = mock(TimesheetEntryService.class);

        user = mock(ApplicationUser.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse(mock(ServletOutputStream.class));
        teamService = mock(TeamService.class);
        categoryService = mock(CategoryService.class);

        when(user.getUsername()).thenReturn("chris");
        when(user.getKey()).thenReturn("chris");
        when(permissionService.checkIfUserExists()).thenReturn(user);
        when(permissionService.getLoggedInUser()).thenReturn(user);
        when(permissionService.isJiraAdministrator(user)).thenReturn(true);

        when(configService.getConfiguration()).thenReturn(config);
        when(config.getTimesheetAdminUsers()).thenReturn(new TimesheetAdmin[0]);
        when(config.getTeams()).thenReturn(new Team[0]);

        exportAllTimesheetsAsJsonServlet = new ExportAllTimesheetsAsJsonServlet(
                loginUriProvider, webSudoManager, permissionService, configService, timesheetService, entryService,
                categoryService, teamService);
    }

    @Test
    public void testDoGet() throws IOException, ServletException {
        exportAllTimesheetsAsJsonServlet.doGet(request, response);

        verify(timesheetService, times(1)).all();

        assertNotEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals(ExportAllTimesheetsAsJsonServlet.CONTENT_TYPE, response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("attachment; filename="));
    }

    @Test
    public void testDoGetUnauthorized() throws Exception {
        when(permissionService.isJiraAdministrator(user)).thenReturn(false);

        exportAllTimesheetsAsJsonServlet.doGet(request, response);

        verify(timesheetService, times(0)).all();
        assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
    }
}
