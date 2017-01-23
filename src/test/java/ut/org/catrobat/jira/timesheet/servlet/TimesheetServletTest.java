package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.servlet.TimesheetServlet;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class TimesheetServletTest {

    final String userKey = "USER_001";
    final int targetHoursPractice = 150;
    final int targetHoursTheory = 0;
    final int targeHours = 300;
    final int targetHoursCompleted = 150;
    final int targetHoursRemoved = 0;
    final int ects = 10;
    final String latestEntryDate = "Not Available";
    final String lectures = "Mobile Applications (705.881)";
    final String reason = "Agathe Bauer";
    private TimesheetServlet timesheetServlet;
    private LoginUriProvider loginUriProvider;
    private TemplateRenderer templateRenderer;
    private TimesheetService sheetService;
    private PermissionService permissionService;
    private Timesheet timeSheet;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private ApplicationUser admin;

    @Before
    public void setUp() throws Exception {
        new MockComponentWorker().init();

        loginUriProvider = mock(LoginUriProvider.class);
        templateRenderer = mock(TemplateRenderer.class);
        sheetService = mock(TimesheetService.class);
        permissionService = mock(PermissionService.class);

        timeSheet = mock(Timesheet.class);
        sheetService = mock(TimesheetService.class);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        timesheetServlet = new TimesheetServlet(loginUriProvider, templateRenderer, sheetService, permissionService);

        admin = mock(ApplicationUser.class);
        String admin_key = "admin_key";
        when(admin.getKey()).thenReturn(admin_key);
        when(admin.getUsername()).thenReturn("admin");
        when(permissionService.checkIfUserExists()).thenReturn(admin);
        when(sheetService.getTimesheetByUser("admin_key", false)).thenReturn(timeSheet);
        when(permissionService.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);
        when(timeSheet.getID()).thenReturn(1);
        when(timeSheet.getUserKey()).thenReturn(admin_key);
        when(timeSheet.getState()).thenReturn(Timesheet.State.ACTIVE);
        when(timeSheet.getIsEnabled()).thenReturn(false);
        when(timeSheet.getIsMasterThesisTimesheet()).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(true);
    }

    @Test(expected = NullPointerException.class)
    public void testDoGetNullPointerException() throws Exception {
        when(permissionService.checkIfUserExists()).thenReturn(admin);
        when(sheetService.getTimesheetByUser("admin_key", false)).thenReturn(null);

        timesheetServlet.doGet(request, response);
    }
}
