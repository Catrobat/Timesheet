package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
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

    UserKey test_key = new UserKey("test_key");
    private UserProfile userProfileMock;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        componentAccessorMock = Mockito.mock(ComponentAccessor.class, RETURNS_DEEP_STUBS);
        loginUriProviderMock = Mockito.mock(LoginUriProvider.class, RETURNS_DEEP_STUBS);
        templateRendererMock = Mockito.mock(TemplateRenderer.class, RETURNS_DEEP_STUBS);
        userManagerMock = Mockito.mock(UserManager.class, RETURNS_DEEP_STUBS);
        webSudoManagerMock = Mockito.mock(WebSudoManager.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = Mockito.mock(PermissionService.class, RETURNS_DEEP_STUBS);
        userProfileMock = Mockito.mock(UserProfile.class, RETURNS_DEEP_STUBS);
        request = Mockito.mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        response = Mockito.mock(HttpServletResponse.class, RETURNS_DEEP_STUBS);

        categoryService = new CategoryServiceImpl(ao);
        configService = new ConfigServiceImpl(ao, categoryService);
        teamService = new TeamServiceImpl(ao, configService);
        permissionService = new PermissionServiceImpl(userManagerMock, teamService, configService);

        adminServlet = new AdminServlet(loginUriProviderMock, templateRendererMock, webSudoManagerMock, permissionServiceMock);

        Mockito.when(userProfileMock.getUsername()).thenReturn("test");
        Mockito.when(userProfileMock.getUserKey()).thenReturn(test_key);

        Mockito.when(permissionService.checkIfUserExists(request)).thenReturn(userProfileMock);

        Mockito.when(userManagerMock.getRemoteUser(request)).thenReturn(userProfileMock);
        Mockito.when(userManagerMock.getUserProfile(test_key)).thenReturn(userProfileMock);

        Mockito.when(permissionServiceMock.checkIfUserIsGroupMember(request, "jira-administrators")).thenReturn(false);
        Mockito.when(permissionServiceMock.checkIfUserIsGroupMember(request, "Timesheet")).thenReturn(true);
    }

    @Test
    public void testDoGet() throws Exception {
        adminServlet.doGet(request, response);
    }

}
