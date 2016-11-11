package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.ApprovedUser;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.servlet.ExportConfigAsCSVServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest(ComponentAccessor.class)
public class ExportConfigAsCSVServletTest {

    String test_key = "test_key";
    private ExportConfigAsCSVServlet exportConfigAsCSVServlet;
    private EntityManager entityManager;
    private ActiveObjects ao;
    private LoginUriProvider loginUriProvider;
    private PermissionService permissionService;
    private WebSudoManager webSudoManager;
    private ConfigService configService;
    private ApplicationUser applicationUser;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private ApplicationUser user;
    private ServletOutputStream outputStream;
    private CategoryService cs;
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Before
    public void setUp() throws Exception {

        PowerMockito.mockStatic(ComponentAccessor.class);

        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        configService = new ConfigServiceImpl(ao, cs);

        loginUriProvider = mock(LoginUriProvider.class);
        webSudoManager = mock(WebSudoManager.class);
        permissionService = mock(PermissionService.class);

        user = mock(ApplicationUser.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        outputStream = mock(ServletOutputStream.class);
        applicationUser = mock(ApplicationUser.class);
        jiraAuthenticationContext = mock(JiraAuthenticationContext.class);

        exportConfigAsCSVServlet = new ExportConfigAsCSVServlet(loginUriProvider, webSudoManager,
                configService, permissionService);

        when(user.getUsername()).thenReturn("test");
        when(user.getKey()).thenReturn(test_key);

        when(permissionService.checkIfUserExists(request)).thenReturn(user);

        when(permissionService.checkIfUserIsGroupMember("jira-administrators")).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);

        when(response.getOutputStream()).thenReturn(outputStream);
        when(applicationUser.getKey()).thenReturn(test_key);

        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(user);
    }

    @Test
    public void testDoGet() throws Exception {
        assertEquals(0, ao.find(ApprovedUser.class).length);
        assertNotNull(configService.addApprovedUser(applicationUser));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedUser.class).length);

        ApprovedUser[] approvedUsers = ao.find(ApprovedUser.class);
        assertTrue(approvedUsers.length > 0);

        assertNotNull(configService.editMail("mailFromName", "mailFrom", "[Subject] Time",
                "[Subject] Inactive", "[Subject] Offline", "[Subject] Active", "[Subject] Entry", "bla", "blabla",
                "blub", "blub blub", "noch mehr bla"));

        assertNotNull(configService.editSupervisedUsers("TestUser"));

        exportConfigAsCSVServlet.doGet(request, response);
    }
}
