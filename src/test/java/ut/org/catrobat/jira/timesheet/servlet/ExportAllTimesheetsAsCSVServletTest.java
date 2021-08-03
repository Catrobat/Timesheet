package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.mock.servlet.MockHttpServletRequest;
import com.atlassian.jira.mock.servlet.MockHttpServletResponse;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserKeyService;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.servlet.ExportAllTimesheetsAsCSVServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@PowerMockIgnore("jdk.internal.reflect.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class ExportAllTimesheetsAsCSVServletTest {

    private ExportAllTimesheetsAsCSVServlet exportAllTimesheetsAsCSVServlet;
    private TimesheetService timesheetService;
    private Timesheet timesheet;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private PermissionService permissionService;
    private ApplicationUser user;

    @Before
    public void setUp() throws Exception {

        new MockComponentWorker().init();

        PowerMockito.mockStatic(ComponentAccessor.class);

        LoginUriProvider loginUriProvider = mock(LoginUriProvider.class);
        WebSudoManager webSudoManager = Mockito.mock(WebSudoManager.class);
        permissionService = Mockito.mock(PermissionService.class);
        timesheetService = Mockito.mock(TimesheetService.class);
        timesheet = Mockito.mock(Timesheet.class);
        user = Mockito.mock(ApplicationUser.class);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse(mock(ServletOutputStream.class));

        ConfigService configService = Mockito.mock(ConfigService.class);
        Config config = Mockito.mock(Config.class);
        JiraAuthenticationContext jiraAuthenticationContext = Mockito.mock(JiraAuthenticationContext.class);
        UserKeyService userKeyService = Mockito.mock(UserKeyService.class);

        exportAllTimesheetsAsCSVServlet = new ExportAllTimesheetsAsCSVServlet(loginUriProvider, webSudoManager, timesheetService,
                configService, permissionService);

        PowerMockito.mockStatic(ComponentAccessor.class);

        when(user.getUsername()).thenReturn("test");
        String test_key = "test_key";
        when(user.getKey()).thenReturn(test_key);
        when(permissionService.checkIfUserExists()).thenReturn(user);

        when(permissionService.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);
        when(permissionService.isJiraAdministrator(user)).thenReturn(true);

        when(timesheet.getTargetHours()).thenReturn(300);
        when(timesheet.getHoursCompleted()).thenReturn(150);
        when(timesheet.getLatestEntryBeginDate()).thenReturn(new Date());
        when(timesheet.getLectures()).thenReturn("Mobile Computing");
        when(timesheet.getState()).thenReturn(Timesheet.State.ACTIVE);
        when(timesheet.getUserKey()).thenReturn("test_key");
        when(timesheet.getHoursDeducted()).thenReturn(0);
        when(timesheet.getReason()).thenReturn("Agathe Bauer");
        when(configService.getConfiguration()).thenReturn(config);
        when(config.getTimesheetAdminUsers()).thenReturn(new TimesheetAdmin[0]);

        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(user);
        PowerMockito.when(ComponentAccessor.getUserKeyService()).thenReturn(userKeyService);
        String userKey = "UserKey";
        PowerMockito.when(userKeyService.getKeyForUsername(user.getUsername())).thenReturn(userKey);
    }

    @Test
    public void testDoGet() throws Exception {
        Mockito.when(timesheetService.getTimesheetByUser(ComponentAccessor.getUserKeyService().
                getKeyForUsername(user.getUsername()))).thenReturn(timesheet);

        exportAllTimesheetsAsCSVServlet.doGet(request, response);

        assertNotEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals(ExportAllTimesheetsAsCSVServlet.CONTENT_TYPE, response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").startsWith("attachment; filename="));
    }

    @Test
    public void testDoGetUnauthorized() throws Exception {
        when(permissionService.isJiraAdministrator(user)).thenReturn(false);

        exportAllTimesheetsAsCSVServlet.doGet(request, response);

        verify(timesheetService, times(0)).all();
        assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
    }
}
