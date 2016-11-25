package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserKeyService;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.servlet.ExportAllTimesheetsAsCSVServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Date;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class ExportAllTimesheetsAsCSVServletTest {

    String test_key = "test_key";
    private ExportAllTimesheetsAsCSVServlet exportAllTimesheetsAsCSVServlet;
    private LoginUriProvider loginUriProvider;
    private PermissionService permissionService;
    private WebSudoManager webSudoManager;
    private ConfigService configService;
    private TimesheetService timesheetService;
    private Timesheet timesheet;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private ApplicationUser user;
    private ServletOutputStream outputStream;
    private JiraAuthenticationContext jiraAuthenticationContext;
    private Config config;

    private UserKeyService userKeyService;

    private String userKey = "UserKey";

    @Before
    public void setUp() throws Exception {

        new MockComponentWorker().init();

        PowerMockito.mockStatic(ComponentAccessor.class);

        loginUriProvider = mock(LoginUriProvider.class);
        webSudoManager = Mockito.mock(WebSudoManager.class);
        permissionService = Mockito.mock(PermissionService.class);
        timesheetService = Mockito.mock(TimesheetService.class);
        user = Mockito.mock(ApplicationUser.class);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        timesheet = Mockito.mock(Timesheet.class);
        outputStream = Mockito.mock(ServletOutputStream.class);
        configService = Mockito.mock(ConfigService.class);
        config = Mockito.mock(Config.class);
        jiraAuthenticationContext = Mockito.mock(JiraAuthenticationContext.class);
        userKeyService = Mockito.mock(UserKeyService.class);

        exportAllTimesheetsAsCSVServlet = new ExportAllTimesheetsAsCSVServlet(loginUriProvider, webSudoManager, timesheetService,
                configService, permissionService);

        PowerMockito.mockStatic(ComponentAccessor.class);

        when(user.getUsername()).thenReturn("test");
        when(user.getKey()).thenReturn(test_key);
        when(permissionService.checkIfUserExists()).thenReturn(user);

        when(permissionService.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);

        when(timesheet.getTargetHoursPractice()).thenReturn(50);
        when(timesheet.getTargetHoursTheory()).thenReturn(100);
        when(timesheet.getTargetHours()).thenReturn(300);
        when(timesheet.getTargetHoursCompleted()).thenReturn(150);
        when(timesheet.getEcts()).thenReturn(10.0);
        when(timesheet.getLatestEntryDate()).thenReturn(new Date());
        when(timesheet.getLectures()).thenReturn("Mobile Computing");
        when(timesheet.getIsActive()).thenReturn(true);
        when(timesheet.getIsEnabled()).thenReturn(true);
        when(timesheet.getUserKey()).thenReturn("test_key");
        when(timesheet.getTargetHoursRemoved()).thenReturn(0);
        when(timesheet.getReason()).thenReturn("Agathe Bauer");
        when(timesheet.getIsEnabled()).thenReturn(true);
        when(timesheet.getIsMasterThesisTimesheet()).thenReturn(false);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(configService.getConfiguration()).thenReturn(config);
        when(config.getTimesheetAdminUsers()).thenReturn(new TimesheetAdmin[0]);

        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(user);
        PowerMockito.when(ComponentAccessor.getUserKeyService()).thenReturn(userKeyService);
        PowerMockito.when(userKeyService.getKeyForUsername(user.getUsername())).thenReturn(userKey);

    }

    @Test
    public void testDoGet() throws Exception {
        Mockito.when(timesheetService.getTimesheetByUser(ComponentAccessor.getUserKeyService().
                getKeyForUsername(user.getUsername()), false)).thenReturn(timesheet);

        exportAllTimesheetsAsCSVServlet.doGet(request, response);
    }
}
