package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.servlet.ExportMasterThesisTimesheetAsCSVServlet;
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class ExportMTTimesheetAsCSVServletTest {

    String test_key = "test_key";
    private ExportMasterThesisTimesheetAsCSVServlet exportMasterThesisTimesheetAsCSVServlet;
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
    private ApplicationUser user;
    private ServletOutputStream outputStream;
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Before
    public void setUp() throws Exception {

        PowerMockito.mockStatic(ComponentAccessor.class);

        loginUriProvider = mock(LoginUriProvider.class);
        templateRenderer = mock(TemplateRenderer.class);
        userManager = mock(UserManager.class);
        webSudoManager = mock(WebSudoManager.class);
        permissionService = mock(PermissionService.class);
        componentAccessor = mock(ComponentAccessor.class);
        timesheetService = mock(TimesheetService.class);
        user = mock(ApplicationUser.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        timesheet = mock(Timesheet.class);
        outputStream = mock(ServletOutputStream.class);
        jiraAuthenticationContext = mock(JiraAuthenticationContext.class);

        exportMasterThesisTimesheetAsCSVServlet = new ExportMasterThesisTimesheetAsCSVServlet(loginUriProvider, webSudoManager, timesheetService,
                configService, permissionService, userManager);

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
        when(timesheet.getUserKey()).thenReturn(test_key);
        when(timesheet.getTargetHoursRemoved()).thenReturn(0);
        when(timesheet.getReason()).thenReturn("Agathe Bauer");
        when(timesheet.getIsEnabled()).thenReturn(true);
        when(timesheet.getIsMasterThesisTimesheet()).thenReturn(true);
        when(timesheetService.getTimesheetByUser(user.getKey(), true)).thenReturn(timesheet);
        when(response.getOutputStream()).thenReturn(outputStream);

        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(user);
    }

    @Test
    public void testDoGet() throws Exception {
        Timesheet sheet0 = timesheetService.getTimesheetByUser(user.getKey(), true);
        assertNotNull(sheet0);
        TimesheetEntry[] timesheetEntries = {};
        Mockito.when(timesheet.getEntries()).thenReturn(timesheetEntries);


        exportMasterThesisTimesheetAsCSVServlet.doGet(request, response);
    }
}
