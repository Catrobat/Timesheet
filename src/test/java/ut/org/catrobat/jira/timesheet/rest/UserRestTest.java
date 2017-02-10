package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.group.search.GroupPickerSearchService;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.rest.UserRest;
import org.mockito.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest(ComponentAccessor.class)
public class UserRestTest {

    private UserManager userManagerJiraMock;
    private ConfigService configServiceMock;
    private UserRest spyUserRest;
    private HttpServletRequest httpRequestMock;
    private UserRest userRest;
    private UserUtil userUtilMock;
    private ApplicationUser userMock;
    private EntityManager entityManager;
    private TestActiveObjects ao;
    private JiraAuthenticationContext jiraAuthMock;
    private PermissionService permissionServiceMock;
    private TimesheetService timesheetServiceMock;
    private TimesheetEntryService timesheetEntryServiceMock;

    private UserSearchService userSearchService;
    private GroupPickerSearchService groupPickerSearchService;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        userManagerJiraMock = mock(UserManager.class, RETURNS_DEEP_STUBS);
        userUtilMock = mock(UserUtil.class, RETURNS_DEEP_STUBS);
        configServiceMock = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        httpRequestMock = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        userMock = mock(ApplicationUser.class, RETURNS_DEEP_STUBS);
        jiraAuthMock = mock(JiraAuthenticationContext.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        timesheetServiceMock = mock(TimesheetService.class, RETURNS_DEEP_STUBS);
        timesheetEntryServiceMock = mock(TimesheetEntryService.class, RETURNS_DEEP_STUBS);
        userSearchService = mock(UserSearchService.class, RETURNS_DEEP_STUBS);
        groupPickerSearchService = mock(GroupPickerSearchService.class, RETURNS_DEEP_STUBS);

        userRest = new UserRest(configServiceMock, permissionServiceMock, timesheetServiceMock,
                timesheetEntryServiceMock, userSearchService, groupPickerSearchService);
        spyUserRest = spy(userRest);

        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthMock);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(userMock);
        PowerMockito.when(permissionServiceMock.checkUserPermission()).thenReturn(null);
    }

    @Test
    public void testGetUsersUnauthorized() {
        doReturn(false).when(permissionServiceMock).isTimesheetAdmin(userMock);
        Response unauthorized = Response.status(Response.Status.UNAUTHORIZED).build();
        doReturn(unauthorized).when(permissionServiceMock).checkUserPermission();

        Response result = spyUserRest.getUsers(httpRequestMock);
        Assert.assertEquals(unauthorized, result);
    }

    @Test
    public void testGetUsersOnlyUsersInList() {
        doReturn(true).when(permissionServiceMock).isTimesheetAdmin(userMock);

        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);

        ApplicationUser user1 = mock(ApplicationUser.class);
        ApplicationUser user2 = mock(ApplicationUser.class);

        List<ApplicationUser> usersSet = new ArrayList<>(Arrays.asList(user1, user2));

        when(userSearchService.findUsersAllowEmptyQuery(Matchers.any(JiraServiceContext.class), Matchers.eq(""))).thenReturn(usersSet);

        PowerMockito.when(user1.getDisplayName()).thenReturn("User 1");
        PowerMockito.when(user2.getDisplayName()).thenReturn("User 2");
        PowerMockito.when(user1.getName()).thenReturn("User 1");
        PowerMockito.when(user2.getName()).thenReturn("User 2");

        spyUserRest.getUsers(httpRequestMock);

        verify(user1, times(1)).getDisplayName();
        verify(user2, times(1)).getDisplayName();
    }

    @Test
    public void testGetUsersUnusualCases() {
        doReturn(true).when(permissionServiceMock).isTimesheetAdmin(userMock);

        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);

        ApplicationUser user1 = mock(ApplicationUser.class);
        ApplicationUser user2 = mock(ApplicationUser.class);

        List<ApplicationUser> usersSet = new ArrayList<>(Arrays.asList(user1, user2));

        when(userSearchService.findUsersAllowEmptyQuery(Matchers.any(JiraServiceContext.class), Matchers.eq(""))).thenReturn(usersSet);

        PowerMockito.when(user1.getDisplayName()).thenReturn("User 1");
        PowerMockito.when(user2.getDisplayName()).thenReturn("User 2");
        PowerMockito.when(user1.getName()).thenReturn("User 1");
        PowerMockito.when(user2.getName()).thenReturn("User 2");

        Group group = mock(Group.class);
        TreeSet<Group> treeSet = new TreeSet(Arrays.asList(group));
        PowerMockito.when(group.getName()).thenReturn("DISABLED");
        PowerMockito.when(userUtilMock.getGroupsForUser(anyString())).thenReturn(treeSet);

        spyUserRest.getUsers(httpRequestMock);

        verify(user1, times(1)).getDisplayName();
        verify(user2, times(1)).getDisplayName();

        Assert.assertEquals(2, usersSet.size());
    }
}
