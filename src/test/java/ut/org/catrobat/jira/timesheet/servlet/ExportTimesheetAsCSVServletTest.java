package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.servlet.ExportTimesheetAsCSVServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class ExportTimesheetAsCSVServletTest {

    private ExportTimesheetAsCSVServlet exportTimesheetAsCSVServlet;
    private TimesheetService timesheetService;
    private Timesheet timesheet;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private ApplicationUser user;

    @Before
    public void setUp() throws Exception {

        PowerMockito.mockStatic(ComponentAccessor.class);

        PermissionService permissionService = mock(PermissionService.class);
        timesheetService = mock(TimesheetService.class);
        user = mock(ApplicationUser.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        timesheet = mock(Timesheet.class);
        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        JiraAuthenticationContext jiraAuthenticationContext = mock(JiraAuthenticationContext.class);
        ConfigService configService = mock(ConfigService.class);
        Config config = mock(Config.class);

        exportTimesheetAsCSVServlet = new ExportTimesheetAsCSVServlet(timesheetService, permissionService);

        when(user.getUsername()).thenReturn("test");
        String test_key = "test_key";
        when(user.getKey()).thenReturn(test_key);

        when(permissionService.checkIfUserExists()).thenReturn(user);

        when(permissionService.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);

        when(timesheet.getHoursCompleted()).thenReturn(50);
        when(timesheet.getTargetHours()).thenReturn(300);
        when(timesheet.getHoursCompleted()).thenReturn(150);
        when(timesheet.getLatestEntryBeginDate()).thenReturn(new Date());
        when(timesheet.getLectures()).thenReturn("Mobile Computing");
        when(timesheet.getUserKey()).thenReturn(test_key);
        when(timesheet.getHoursDeducted()).thenReturn(0);
        when(timesheet.getReason()).thenReturn("Agathe Bauer");
        when(timesheetService.getTimesheetByUser(user.getKey())).thenReturn(timesheet);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(configService.getConfiguration()).thenReturn(config);
        when(config.getTimesheetAdminUsers()).thenReturn(new TimesheetAdmin[0]);

        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(user);

    }

    @Test
    public void testDoGet() throws Exception {
        Timesheet sheet0 = timesheetService.getTimesheetByUser(user.getKey());
        assertNotNull(sheet0);
        TimesheetEntry[] timesheetEntries = {};
        when(timesheet.getEntries()).thenReturn(timesheetEntries);

        exportTimesheetAsCSVServlet.doGet(request, response);
    }
}
