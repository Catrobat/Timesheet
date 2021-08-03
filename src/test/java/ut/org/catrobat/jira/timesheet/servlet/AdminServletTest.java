package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.*;
import org.catrobat.jira.timesheet.servlet.AdminServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@PowerMockIgnore("jdk.internal.reflect.*")
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest(ComponentAccessor.class)
public class AdminServletTest {
    private AdminServlet adminServlet;
    private EntityManager entityManager;
    private HttpServletResponse response;
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        TestActiveObjects ao = new TestActiveObjects(entityManager);

        PowerMockito.mockStatic(ComponentAccessor.class);

        LoginUriProvider loginUriProviderMock = mock(LoginUriProvider.class, RETURNS_DEEP_STUBS);
        TemplateRenderer templateRendererMock = mock(TemplateRenderer.class, RETURNS_DEEP_STUBS);
        WebSudoManager webSudoManagerMock = mock(WebSudoManager.class, RETURNS_DEEP_STUBS);
        PermissionService permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        ApplicationUser userMock = mock(ApplicationUser.class, RETURNS_DEEP_STUBS);
        request = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        response = mock(HttpServletResponse.class, RETURNS_DEEP_STUBS);
        JiraAuthenticationContext jiraAuthenticationContext = mock(JiraAuthenticationContext.class, RETURNS_DEEP_STUBS);

        CategoryService categoryService = new CategoryServiceImpl(ao);
        TimesheetService timesheetService = new TimesheetServiceImpl(ao);
        TimesheetEntryService entryService = new TimesheetEntryServiceImpl(ao, timesheetService);
        TeamService teamService = new TeamServiceImpl(ao, categoryService, entryService);
        ConfigService configService = new ConfigServiceImpl(ao, categoryService, teamService);

        adminServlet = new AdminServlet(loginUriProviderMock, templateRendererMock, webSudoManagerMock, permissionServiceMock, configService);

        when(userMock.getUsername()).thenReturn("test");
        String test_key = "test_key";
        when(userMock.getKey()).thenReturn(test_key);

        when(permissionServiceMock.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        when(permissionServiceMock.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);

        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(userMock);
    }

    @Test
    public void testDoGet() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        adminServlet.service(request, response);
        adminServlet.doGet(request, response);
    }

}
