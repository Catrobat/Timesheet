package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.servlet.ExportAllTimesheetsAsCSVServlet;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class ExportAllTimesheetsAsCSVServletTest {

    String test_key = "test_key";
    private ExportAllTimesheetsAsCSVServlet exportAllTimesheetsAsCSVServlet;
    private LoginUriProvider loginUriProvider;
    private TemplateRenderer templateRenderer;
    private PermissionService permissionService;
    private WebSudoManager webSudoManager;
    private ConfigService configService;
    private ComponentAccessor componentAccessor;
    private TimesheetService timesheetService;
    private Timesheet timesheet;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private ApplicationUser user;
    private ServletOutputStream outputStream;

    @Before
    public void setUp() throws Exception {

        new MockComponentWorker().init();

        loginUriProvider = mock(LoginUriProvider.class);
        templateRenderer = Mockito.mock(TemplateRenderer.class);
        webSudoManager = Mockito.mock(WebSudoManager.class);
        permissionService = Mockito.mock(PermissionService.class);
        timesheetService = Mockito.mock(TimesheetService.class);
        user = Mockito.mock(ApplicationUser.class);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        timesheet = Mockito.mock(Timesheet.class);
        outputStream = Mockito.mock(ServletOutputStream.class);

        exportAllTimesheetsAsCSVServlet = new ExportAllTimesheetsAsCSVServlet(loginUriProvider, webSudoManager, timesheetService,
                configService, permissionService);

        PowerMockito.mockStatic(ComponentAccessor.class);

        when(user.getUsername()).thenReturn("test");
        when(user.getKey()).thenReturn(test_key);
        when(permissionService.checkIfUserExists(request)).thenReturn(user);

        when(permissionService.checkIfUserIsGroupMember(request, "jira-administrators")).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember(request, "Timesheet")).thenReturn(true);

        when(timesheet.getTargetHoursPractice()).thenReturn(50);
        when(timesheet.getTargetHoursTheory()).thenReturn(100);
        when(timesheet.getTargetHours()).thenReturn(300);
        when(timesheet.getTargetHoursCompleted()).thenReturn(150);
        when(timesheet.getEcts()).thenReturn(10.0);
        when(timesheet.getLatestEntryDate()).thenReturn(new DateTime().toString());
        when(timesheet.getLectures()).thenReturn("Mobile Computing");
        when(timesheet.getIsActive()).thenReturn(true);
        when(timesheet.getIsEnabled()).thenReturn(true);
        when(timesheet.getUserKey()).thenReturn("test_key");
        when(timesheet.getTargetHoursRemoved()).thenReturn(0);
        when(timesheet.getReason()).thenReturn("Agathe Bauer");
        when(timesheet.getIsEnabled()).thenReturn(true);
        when(timesheet.getIsMasterThesisTimesheet()).thenReturn(false);
        when(response.getOutputStream()).thenReturn(outputStream);

    }

    @Test
    public void testDoGet() throws Exception {
        Mockito.when(timesheetService.getTimesheetByUser(componentAccessor.getUserKeyService().
                getKeyForUsername(user.getUsername()), false)).thenReturn(timesheet);

        exportAllTimesheetsAsCSVServlet.doGet(request, response);
    }
}
