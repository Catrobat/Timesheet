package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.mock.servlet.MockHttpServletRequest;
import com.atlassian.jira.mock.servlet.MockHttpServletResponse;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.apache.poi.ss.usermodel.Workbook;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.services.XlsxExportService;
import org.catrobat.jira.timesheet.servlet.ExportAllTimesheetsAsXlsxServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class ExportAllTimesheetsAsXlsxServletTest {

    private ExportAllTimesheetsAsXlsxServlet exportAllTimesheetAsXLSXServlet;
    private XlsxExportService exportService;
    private TimesheetService timesheetService;
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

        timesheetService = mock(TimesheetService.class);
        exportService = mock(XlsxExportService.class);
        user = mock(ApplicationUser.class);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        JiraAuthenticationContext jiraAuthenticationContext = mock(JiraAuthenticationContext.class);
        ConfigService configService = mock(ConfigService.class);
        Config config = mock(Config.class);

        exportAllTimesheetAsXLSXServlet = new ExportAllTimesheetsAsXlsxServlet(loginUriProvider, webSudoManager,
                timesheetService, exportService, configService, permissionService);

        when(user.getUsername()).thenReturn("test");
        String test_key = "test_key";
        when(user.getKey()).thenReturn(test_key);

        when(configService.getConfiguration()).thenReturn(config);
        when(config.getTimesheetAdminUsers()).thenReturn(new TimesheetAdmin[0]);

        when(permissionService.checkIfUserExists()).thenReturn(user);
        when(permissionService.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);
        when(permissionService.isJiraAdministrator(user)).thenReturn(true);

        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(user);
    }

    @Test
    public void testDoGet() throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-dd-MM");
        String expectedFilename = "Timesheets-" + simpleDateFormat.format(new Date()) + ".xlsx";

        List<Timesheet> availableTimesheets = Arrays.asList(mock(Timesheet.class), mock(Timesheet.class));
        when(timesheetService.all()).thenReturn(availableTimesheets);

        Workbook workbook = mock(Workbook.class);
        when(exportService.exportTimesheets(eq(availableTimesheets))).thenReturn(workbook);

        exportAllTimesheetAsXLSXServlet.doGet(request, response);

        verify(exportService, times(1)).exportTimesheets(availableTimesheets);

        assertNotEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals(ExportAllTimesheetsAsXlsxServlet.CONTENT_TYPE, response.getContentType());
        assertEquals("attachment; filename=\"" + expectedFilename + "\"", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testDoGetUnauthorized() throws Exception {
        when(permissionService.isJiraAdministrator(user)).thenReturn(false);

        exportAllTimesheetAsXLSXServlet.doGet(request, response);

        verify(timesheetService, times(0)).all();
        verify(exportService, times(0)).exportTimesheets(any());
        assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
    }
}
