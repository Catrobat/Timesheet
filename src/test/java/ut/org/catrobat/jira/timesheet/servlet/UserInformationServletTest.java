package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.servlet.UserInformationServlet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class UserInformationServletTest {

    private UserInformationServlet userInformationServlet;
    private HttpServletResponse response;
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {

        LoginUriProvider loginUriProvider = mock(LoginUriProvider.class);
        TemplateRenderer templateRenderer = mock(TemplateRenderer.class);
        WebSudoManager webSudoManager = mock(WebSudoManager.class);
        PermissionService permissionService = mock(PermissionService.class);
        PowerMockito.mockStatic(ComponentAccessor.class);
        ApplicationUser user = mock(ApplicationUser.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        ConfigService configService = Mockito.mock(ConfigService.class);
        Config config = Mockito.mock(Config.class);
        JiraAuthenticationContext jiraAuthenticationContext = Mockito.mock(JiraAuthenticationContext.class);

        userInformationServlet = new UserInformationServlet(loginUriProvider, templateRenderer, webSudoManager, permissionService, configService);

        when(user.getUsername()).thenReturn("test");
        String test_key = "test_key";
        when(user.getKey()).thenReturn(test_key);
        when(permissionService.checkIfUserExists()).thenReturn(user);
        when(permissionService.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        when(permissionService.checkIfUserIsGroupMember("Timesheet")).thenReturn(true);

        when(configService.getConfiguration()).thenReturn(config);
        when(config.getTimesheetAdminUsers()).thenReturn(new TimesheetAdmin[0]);

        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
    }

    @Test
    public void testDoGet() throws Exception {
        userInformationServlet.doGet(request, response);
    }
}
