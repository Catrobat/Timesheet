package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.MockApplicationUser;
import com.atlassian.jira.user.util.UserUtil;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.rest.UserRest;
import org.catrobat.jira.timesheet.rest.RestUtils;
import org.catrobat.jira.timesheet.services.TeamService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class UserRestTest {


    private ComponentAccessor componentAccessor;

    private com.atlassian.sal.api.user.UserManager userManagerLDAP;
    private com.atlassian.jira.user.util.UserManager userManagerJira;
    private ConfigService configService;
    private TeamService teamService;
    private UserRest spyUserRest;
    private HttpServletRequest httpRequest;
    private UserRest userRest;
    private UserUtil userUtil;

    @Before
    public void setUp() throws Exception {
        new MockComponentWorker().init();

        componentAccessor = Mockito.mock(ComponentAccessor.class, RETURNS_DEEP_STUBS);
        userManagerLDAP = Mockito.mock(com.atlassian.sal.api.user.UserManager.class, RETURNS_DEEP_STUBS);
        userManagerJira = Mockito.mock(com.atlassian.jira.user.util.UserManager.class, RETURNS_DEEP_STUBS);
        userUtil = Mockito.mock(UserUtil.class, RETURNS_DEEP_STUBS);

        configService = Mockito.mock(ConfigService.class);
        teamService = Mockito.mock(TeamService.class);
        httpRequest = Mockito.mock(HttpServletRequest.class);

        userRest = new UserRest(userManagerLDAP, configService, teamService);
        spyUserRest = spy(userRest);

        final ApplicationUser fred = new MockApplicationUser("Fred");
        final JiraAuthenticationContext jiraAuthenticationContext = Mockito.mock(JiraAuthenticationContext.class);
        Mockito.when(jiraAuthenticationContext.getUser()).thenReturn(fred);
        Mockito.when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(fred.getDirectoryUser());
        new MockComponentWorker()
                .addMock(JiraAuthenticationContext.class, jiraAuthenticationContext)
                .addMock(UserUtil.class, userUtil)
                .addMock(com.atlassian.sal.api.user.UserManager.class, userManagerLDAP)
                .addMock(com.atlassian.jira.user.util.UserManager.class, userManagerJira)
                .init();

        PowerMockito.mockStatic(ComponentAccessor.class);

    }

    @Test
    public void testGetUsersUnauthorized() {
        String username = "MarkusHobisch";
        when(userManagerLDAP.getRemoteUser().getUsername()).thenReturn(username);

        doReturn(false).when(spyUserRest).isApproved(username);
        Response unauthorized = Response.status(Response.Status.UNAUTHORIZED).build();
        doReturn(unauthorized).when(spyUserRest).checkPermission(httpRequest);

        Response result = spyUserRest.getUsers(httpRequest);
        Assert.assertEquals(unauthorized, result);
    }

/*
    @Test
    public void testGetUsersOnlySystemAdminsInList(){
        String username = "MarkusHobisch";
        when(userManagerLDAP.getRemoteUser().getUsername()).thenReturn(username);
        doReturn(true).when(spyUserRest).isApproved(username);


        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJira);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtil);


        User user1 = mock(User.class);
        User user2 = mock(User.class);

        Set<User> usersSet = new HashSet<User>(Arrays.asList(user1, user2));

        // In this testcase all users are system admins
        User sysAdmin1 = user1;
        User sysAdmin2 = user2;

        Set<User> sysAdminsSet = new HashSet<User>(Arrays.asList(sysAdmin1, sysAdmin2));

        PowerMockito.when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        PowerMockito.when(userUtil.getJiraSystemAdministrators()).thenReturn(sysAdminsSet);

        spyUserRest.getUsers(httpRequest);

        verify(user1, never()).getDisplayName();
        verify(user2, never()).getDisplayName();
    }
    */

    @Test
    public void testGetUsersOnlyUsersInList(){
        String username = "MarkusHobisch";
        when(userManagerLDAP.getRemoteUser().getUsername()).thenReturn(username);
        doReturn(true).when(spyUserRest).isApproved(username);


        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJira);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtil);


        User user1 = mock(User.class);
        User user2 = mock(User.class);

        Set<User> usersSet = new HashSet<User>(Arrays.asList(user1, user2));

        // In this testcase all users are normal users
        User sysAdmin1 = mock(User.class);
        User sysAdmin2 = mock(User.class);

        Set<User> sysAdminsSet = new HashSet<User>(Arrays.asList(sysAdmin1, sysAdmin2));

        PowerMockito.when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        PowerMockito.when(userUtil.getJiraSystemAdministrators()).thenReturn(sysAdminsSet);

        PowerMockito.when(user1.getDisplayName()).thenReturn("User 1");
        PowerMockito.when(user2.getDisplayName()).thenReturn("User 2");
        PowerMockito.when(user1.getName()).thenReturn("User 1");
        PowerMockito.when(user2.getName()).thenReturn("User 2");

        spyUserRest.getUsers(httpRequest);

        verify(user1, times(1)).getDisplayName();
        verify(user2, times(1)).getDisplayName();
    }

    @Test
    public void testGetUsersUnusualCases(){
        String username = "MarkusHobisch";
        when(userManagerLDAP.getRemoteUser().getUsername()).thenReturn(username);
        doReturn(true).when(spyUserRest).isApproved(username);


        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJira);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtil);


        User user1 = mock(User.class);
        User user2 = mock(User.class);

        Set<User> usersSet = new HashSet<User>(Arrays.asList(user1, user2));

        // In this testcase all users are normal users
        User sysAdmin1 = mock(User.class);
        User sysAdmin2 = mock(User.class);

        Set<User> sysAdminsSet = new HashSet<User>(Arrays.asList(sysAdmin1, sysAdmin2));

        PowerMockito.when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        PowerMockito.when(userUtil.getJiraSystemAdministrators()).thenReturn(sysAdminsSet);

        PowerMockito.when(user1.getDisplayName()).thenReturn("User 1");
        PowerMockito.when(user2.getDisplayName()).thenReturn("User 2");
        PowerMockito.when(user1.getName()).thenReturn("User 1");
        PowerMockito.when(user2.getName()).thenReturn("User 2");

        Group group = mock(Group.class);
        TreeSet<Group> treeSet = new TreeSet(Arrays.asList(group));
        PowerMockito.when(group.getName()).thenReturn("DISABLED");
        PowerMockito.when(userUtil.getGroupsForUser(anyString())).thenReturn(treeSet);

        spyUserRest.getUsers(httpRequest);

        verify(user1, times(1)).getDisplayName();
        verify(user2, times(1)).getDisplayName();

        TreeSet<User> sortedUsers = RestUtils.getInstance().getSortedUsers(usersSet);
        Assert.assertEquals(2,sortedUsers.size());

    }
}