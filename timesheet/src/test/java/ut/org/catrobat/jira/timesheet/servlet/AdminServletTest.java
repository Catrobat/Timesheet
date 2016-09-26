package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.impl.CategoryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.PermissionServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TeamServiceImpl;
import org.catrobat.jira.timesheet.servlet.AdminServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest(ComponentAccessor.class)
public class AdminServletTest {
    private AdminServlet adminServlet;

    private LoginUriProvider loginUriProviderMock;
    private TemplateRenderer templateRendererMock;
    private PermissionService permissionServiceMock;
    private UserManager userManagerMock;
    private WebSudoManager webSudoManagerMock;
    private ConfigService configServiceMock;
    private ComponentAccessor componentAccessorMock;

    private TeamService teamService;
    private PermissionService permissionService;
    private ConfigService configService;
    private CategoryService categoryService;
    private TestActiveObjects ao;
    private EntityManager entityManager;

    private HttpServletResponse response;
    private HttpServletRequest request;

    String test_key = "test_key";
    private ApplicationUser userMock;
    private UserManager userManager;
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        PowerMockito.mockStatic(ComponentAccessor.class);
        loginUriProviderMock = Mockito.mock(LoginUriProvider.class, RETURNS_DEEP_STUBS);
        templateRendererMock = Mockito.mock(TemplateRenderer.class, RETURNS_DEEP_STUBS);
        userManagerMock = Mockito.mock(UserManager.class, RETURNS_DEEP_STUBS);
        webSudoManagerMock = Mockito.mock(WebSudoManager.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = Mockito.mock(PermissionService.class, RETURNS_DEEP_STUBS);
        userMock = Mockito.mock(ApplicationUser.class, RETURNS_DEEP_STUBS);
        request = Mockito.mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        response = Mockito.mock(HttpServletResponse.class, RETURNS_DEEP_STUBS);
        jiraAuthenticationContext = Mockito.mock(JiraAuthenticationContext.class, RETURNS_DEEP_STUBS);

        categoryService = new CategoryServiceImpl(ao);
        configService = new ConfigServiceImpl(ao, categoryService, userManager);
        teamService = new TeamServiceImpl(ao, configService);
        permissionService = new PermissionServiceImpl(userManagerMock, teamService, configService);

        adminServlet = new AdminServlet(loginUriProviderMock, templateRendererMock, webSudoManagerMock, permissionServiceMock);

        Mockito.when(userMock.getUsername()).thenReturn("test");
        Mockito.when(userMock.getKey()).thenReturn(test_key);

        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(userMock);

        Mockito.when(permissionServiceMock.checkIfUserIsGroupMember(request, "jira-administrators")).thenReturn(false);
        Mockito.when(permissionServiceMock.checkIfUserIsGroupMember(request, "Timesheet")).thenReturn(true);
    }

    @Test
    public void testDoGet() throws Exception {
        adminServlet.doGet(request, response);
    }

}
