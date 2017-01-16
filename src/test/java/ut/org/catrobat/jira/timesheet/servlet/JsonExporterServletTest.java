package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.gzipfilter.org.tuckey.web.filters.urlrewrite.Conf;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.services.impl.TimesheetEntryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetServiceImpl;
import org.catrobat.jira.timesheet.servlet.ExportAllTimesheetsAsJsonServlet;
import org.catrobat.jira.timesheet.servlet.ExportTimesheetAsJsonServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
public class JsonExporterServletTest {

    private LoginUriProvider loginUriProvider;
    private WebSudoManager webSudoManager;

    private ConfigService configService;
    private Config config;

    private PermissionService permissionService;
    private TimesheetService timesheetService;
    private TimesheetEntryService entryService;

    private ApplicationUser user;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private EntityManager entityManager;
    private ActiveObjects ao;

    @Before
    public void setup() throws IOException, PermissionException{
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        loginUriProvider = mock(LoginUriProvider.class);
        webSudoManager = mock(WebSudoManager.class);

        configService = mock(ConfigService.class);
        config = mock(Config.class);

        permissionService = mock(PermissionService.class);
        timesheetService = new TimesheetServiceImpl(ao);
        entryService = new TimesheetEntryServiceImpl(ao);

        user = mock(ApplicationUser.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        when(user.getUsername()).thenReturn("chris");
        when(user.getKey()).thenReturn("chris");
        when(permissionService.checkIfUserExists()).thenReturn(user);
        when(permissionService.getLoggedInUser()).thenReturn(user);

        when(configService.getConfiguration()).thenReturn(config);
        when(config.getTimesheetAdminUsers()).thenReturn(new TimesheetAdmin[0]);

        when(response.getOutputStream()).thenReturn(new TestServletOutputStream());
    }

    @Test
    public void testDoGetSingleTimesheet() throws IOException, ServletException {
        ExportTimesheetAsJsonServlet exportTimesheetAsJsonServlet = new ExportTimesheetAsJsonServlet(timesheetService, permissionService, entryService);

        exportTimesheetAsJsonServlet.doGet(request, response);
    }

    @Test
    public void testDoGetAllTimesheets() throws IOException, ServletException {
        ExportAllTimesheetsAsJsonServlet exportAllTimesheetsAsJsonServlet = new ExportAllTimesheetsAsJsonServlet(
                loginUriProvider, webSudoManager, permissionService, configService, timesheetService, entryService);

        exportAllTimesheetsAsJsonServlet.doGet(request, response);
    }

    class TestServletOutputStream extends ServletOutputStream {
        public ByteArrayOutputStream baos = new ByteArrayOutputStream();
        public void write(int i) throws IOException {
            baos.write(i);
        }
    }
}
