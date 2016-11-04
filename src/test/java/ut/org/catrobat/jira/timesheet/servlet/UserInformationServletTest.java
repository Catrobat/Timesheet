package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.servlet.UserInformationServlet;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class UserInformationServletTest {

    String test_key = "test_key";
    private UserInformationServlet userInformationServlet;
    private LoginUriProvider loginUriProvider;
    private TemplateRenderer templateRenderer;
    private PermissionService permissionService;
    private WebSudoManager webSudoManager;
    private ConfigService configService;
    private ComponentAccessor componentAccessor;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private ApplicationUser user;

    @Before
    public void setUp() throws Exception {
        new MockComponentWorker().init();

        loginUriProvider = mock(LoginUriProvider.class);
        templateRenderer = mock(TemplateRenderer.class);
        webSudoManager = mock(WebSudoManager.class);
        permissionService = mock(PermissionService.class);
        componentAccessor = mock(ComponentAccessor.class);
        user = mock(ApplicationUser.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        userInformationServlet = new UserInformationServlet(loginUriProvider, templateRenderer, webSudoManager, permissionService, configService);

        when(user.getUsername()).thenReturn("test");
        when(user.getKey()).thenReturn(test_key);
        when(permissionService.checkIfUserExists(request)).thenReturn(user);
        when(permissionService.checkIfUserIsGroupMember("jira-administrators")).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);
    }

    @Test
    public void testDoGet() throws Exception {
        userInformationServlet.doGet(request, response);
    }
}
