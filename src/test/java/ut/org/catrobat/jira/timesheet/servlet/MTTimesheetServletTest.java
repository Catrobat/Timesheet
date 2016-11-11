package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.servlet.MasterThesisTimesheetServlet;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class MTTimesheetServletTest {

    private MasterThesisTimesheetServlet masterThesisTimesheetServlet;

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

        masterThesisTimesheetServlet = new MasterThesisTimesheetServlet(loginUriProvider, templateRenderer, sheetService, permissionService);

        admin = mock(ApplicationUser.class);
        String admin_key = "admin_key";
        when(admin.getKey()).thenReturn(admin_key);
        when(admin.getUsername()).thenReturn("admin");
        when(permissionService.checkIfUserExists(request)).thenReturn(admin);
        when(sheetService.getTimesheetByUser("admin_key", false)).thenReturn(timeSheet);
        when(permissionService.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);
        when(timeSheet.getID()).thenReturn(1);
        when(timeSheet.getUserKey()).thenReturn(admin_key);
        when(timeSheet.getIsActive()).thenReturn(true);
        when(timeSheet.getIsEnabled()).thenReturn(false);
        when(timeSheet.getIsMasterThesisTimesheet()).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember("jira-administrators")).thenReturn(true);
    }

    @Test(expected = NullPointerException.class)
    public void testDoGetNullPointerException() throws Exception {
        when(permissionService.checkIfUserExists(request)).thenReturn(admin);
        when(sheetService.getTimesheetByUser("admin_key", false)).thenReturn(null);

        masterThesisTimesheetServlet.doGet(request, response);
    }
}
