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

    private TimesheetServlet timesheetServlet;
    private HttpServletResponse response;
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        new MockComponentWorker().init();

        LoginUriProvider loginUriProvider = mock(LoginUriProvider.class);
        TemplateRenderer templateRenderer = mock(TemplateRenderer.class);
        TimesheetService sheetService = mock(TimesheetService.class);
        PermissionService permissionService = mock(PermissionService.class);

        Timesheet timeSheet = mock(Timesheet.class);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        timesheetServlet = new TimesheetServlet(loginUriProvider, templateRenderer, sheetService, permissionService);

        ApplicationUser admin = mock(ApplicationUser.class);
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

    @Test
    public void testDoGetNullPointerException() throws Exception {
        timesheetServlet.doGet(request, response);
    }
}
