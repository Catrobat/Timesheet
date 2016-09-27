package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*@RunWith(PowerMockRunner.class)
@PrepareForTest({ComponentAccessor.class})*/
public class ExportAllTimesheetsAsCSVServletTest {

    private ExportAllTimesheetsAsCSVServlet exportAllTimesheetsAsCSVServlet;

    private LoginUriProvider loginUriProvider;
    private TemplateRenderer templateRenderer;
    private PermissionService permissionService;
    private UserManager userManager;
    private WebSudoManager webSudoManager;
    private ConfigService configService;
    private ComponentAccessor componentAccessor;
    private TimesheetService timesheetService;
    private Timesheet timesheet;

    private HttpServletResponse response;
    private HttpServletRequest request;

    String test_key = "test_key";
    private ApplicationUser user;
    private ServletOutputStream outputStream;

    @Before
    public void setUp() throws Exception {

        new MockComponentWorker().init();

        loginUriProvider = Mockito.mock(LoginUriProvider.class);
        templateRenderer = Mockito.mock(TemplateRenderer.class);
        userManager = Mockito.mock(UserManager.class);
        webSudoManager = Mockito.mock(WebSudoManager.class);
        permissionService = Mockito.mock(PermissionService.class);
        //componentAccessor = Mockito.mock(ComponentAccessor.class);
        timesheetService = Mockito.mock(TimesheetService.class);
        user = Mockito.mock(ApplicationUser.class);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        timesheet = Mockito.mock(Timesheet.class);
        outputStream = Mockito.mock(ServletOutputStream.class);

        exportAllTimesheetsAsCSVServlet = new ExportAllTimesheetsAsCSVServlet(loginUriProvider, webSudoManager, timesheetService,
                configService, permissionService);

        PowerMockito.mockStatic(ComponentAccessor.class);
        Mockito.when(user.getUsername()).thenReturn("test");
        Mockito.when(user.getKey()).thenReturn(test_key);

        Mockito.when(permissionService.checkIfUserExists(request)).thenReturn(user);

        //Mockito.when(userManager.getRemoteUser(request)).thenReturn(user);
        //Mockito.when(userManager.getUserProfile(test_key)).thenReturn(user);

        Mockito.when(permissionService.checkIfUserIsGroupMember(request, "jira-administrators")).thenReturn(false);
        Mockito.when(permissionService.checkIfUserIsGroupMember(request, "Timesheet")).thenReturn(true);

        Mockito.when(timesheet.getTargetHoursPractice()).thenReturn(50);
        Mockito.when(timesheet.getTargetHoursTheory()).thenReturn(100);
        Mockito.when(timesheet.getTargetHours()).thenReturn(300);
        Mockito.when(timesheet.getTargetHoursCompleted()).thenReturn(150);
        Mockito.when(timesheet.getEcts()).thenReturn(10.0);
        Mockito.when(timesheet.getLatestEntryDate()).thenReturn(new DateTime().toString());
        Mockito.when(timesheet.getLectures()).thenReturn("Mobile Computing");
        Mockito.when(timesheet.getIsActive()).thenReturn(true);
        Mockito.when(timesheet.getIsEnabled()).thenReturn(true);
        Mockito.when(timesheet.getUserKey()).thenReturn("test_key");
        Mockito.when(timesheet.getTargetHoursRemoved()).thenReturn(0);
        Mockito.when(timesheet.getReason()).thenReturn("Agathe Bauer");
        Mockito.when(timesheet.getIsEnabled()).thenReturn(true);
        Mockito.when(timesheet.getIsMasterThesisTimesheet()).thenReturn(false);

        Mockito.when(response.getOutputStream()).thenReturn(outputStream);

    }

    @Test
    public void testDoGet() throws Exception {
        Mockito.when(timesheetService.getTimesheetByUser(componentAccessor.getUserKeyService().
                getKeyForUsername(user.getUsername()), false)).thenReturn(timesheet);

        exportAllTimesheetsAsCSVServlet.doGet(request, response);
    }
}
