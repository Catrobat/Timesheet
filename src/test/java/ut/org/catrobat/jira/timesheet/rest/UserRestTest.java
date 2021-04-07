package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.group.search.GroupPickerSearchService;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.rest.json.JsonUserInformation;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.rest.UserRest;
import org.catrobat.jira.timesheet.services.impl.TimesheetServiceImpl;
import org.mockito.Matchers;
import org.junit.Assert;
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
import javax.ws.rs.core.Response;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest(ComponentAccessor.class)
public class UserRestTest {

    private TestActiveObjects ao;
    private UserManager userManagerJiraMock;
    private UserRest spyUserRest;
    private HttpServletRequest httpRequestMock;
    private UserUtil userUtilMock;
    private ApplicationUser userMock;
    private EntityManager entityManager;
    private UserManager userManager;
    private GroupManager groupManager;
    private TeamService teamServiceMock;
    private TimesheetService timesheetServiceMock;
    private PermissionService permissionServiceMock;
    private UserSearchService userSearchService;
    private GroupPickerSearchService groupPickerSearchService;
    private List<ApplicationUser> usersSet;
    private ApplicationUser user1;
    private ApplicationUser user2;
    private ApplicationUser chris;
    private ApplicationUser joh;
    private String timesheetId = "125";


    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        userManagerJiraMock = mock(UserManager.class, RETURNS_DEEP_STUBS);
        userUtilMock = mock(UserUtil.class, RETURNS_DEEP_STUBS);
        ConfigService configServiceMock = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        httpRequestMock = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        userMock = mock(ApplicationUser.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        TimesheetEntryService timesheetEntryServiceMock = mock(TimesheetEntryService.class, RETURNS_DEEP_STUBS);
        teamServiceMock = mock(TeamService.class, RETURNS_DEEP_STUBS);
        userSearchService = mock(UserSearchService.class, RETURNS_DEEP_STUBS);
        MonitoringService monitoringService = mock(MonitoringService.class, RETURNS_DEEP_STUBS);
        groupPickerSearchService = mock(GroupPickerSearchService.class, RETURNS_DEEP_STUBS);
        userManager = mock(UserManager.class);
        groupManager = mock(GroupManager.class);
        timesheetServiceMock = new TimesheetServiceImpl(ao);

        UserRest userRest = new UserRest(configServiceMock, permissionServiceMock, timesheetServiceMock,
                timesheetEntryServiceMock, teamServiceMock, monitoringService, userSearchService, groupPickerSearchService);
        spyUserRest = spy(userRest);


        user1 = mock(ApplicationUser.class);
        user2 = mock(ApplicationUser.class);

        Mockito.when(user1.getDisplayName()).thenReturn("User 1");
        Mockito.when(user2.getDisplayName()).thenReturn("User 2");
        Mockito.when(user1.getName()).thenReturn("User 1");
        Mockito.when(user2.getName()).thenReturn("User 2");

        usersSet = new ArrayList<>(Arrays.asList(user1, user2));
        when(userSearchService.findUsersAllowEmptyQuery(Matchers.any(JiraServiceContext.class), Matchers.eq(""))).thenReturn(usersSet);

        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManager);
        PowerMockito.when(ComponentAccessor.getGroupManager()).thenReturn(groupManager);
        chris = mock(ApplicationUser.class);
        joh = mock(ApplicationUser.class);
        when(userMock.getUsername()).thenReturn("userMock");
        when(userManager.getUserByKey("chris")).thenReturn(chris);
        when(userManager.getUserByKey("joh")).thenReturn(joh);
        when(userManager.getUserByName("chris")).thenReturn(chris);
        when(userManager.getUserByName("joh")).thenReturn(joh);
        when(chris.getUsername()).thenReturn("chris");
        when(chris.getName()).thenReturn("chris");
        when(joh.getUsername()).thenReturn("joh");
        when(joh.getName()).thenReturn("joh");
        when(chris.getEmailAddress()).thenReturn("chris@example.com");
        when(joh.getEmailAddress()).thenReturn("joh@example.com");
        Mockito.when(permissionServiceMock.checkUserPermission()).thenReturn(null);
    }


    @Test
    public void testUserInformationStates() {
        when(permissionServiceMock.checkRootPermission()).thenReturn(null);
        Response response_active = spyUserRest.getUserInformationWithState(httpRequestMock, "ACTIVE");
        List<JsonUserInformation> list_active = (List<JsonUserInformation>) response_active.getEntity();
        assertEquals(1, list_active.size());
        assertEquals("chris", list_active.get(0).getUserName());
        assertEquals(Timesheet.State.ACTIVE, list_active.get(0).getState());

        Response response_inactive = spyUserRest.getUserInformationWithState(httpRequestMock, "INACTIVE");
        List<JsonUserInformation> list_inactive = (List<JsonUserInformation>) response_inactive.getEntity();
        assertEquals(1, list_inactive.size());
        assertEquals("joh", list_inactive.get(0).getUserName());
        assertEquals(Timesheet.State.INACTIVE, list_inactive.get(0).getState());
    }

    @Test
    public void testUserInformationCount() {
        when(permissionServiceMock.checkRootPermission()).thenReturn(null);
        Response response = spyUserRest.getUserInformationStats(httpRequestMock);
        List<Integer> counts = (List<Integer>) response.getEntity();
        assertEquals(6, counts.size());
        assertEquals(1, (int) counts.get(0));
        assertEquals(1, (int) counts.get(1));
        assertEquals(0, (int) counts.get(2));
        assertEquals(0, (int) counts.get(3));
        assertEquals(0, (int) counts.get(4));
        assertEquals(0, (int) counts.get(5));
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

        Mockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        Mockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);

        spyUserRest.getUsers(httpRequestMock);

        verify(user1, times(1)).getDisplayName();
        verify(user2, times(1)).getDisplayName();
    }

    @Test
    public void testGetUsersUnusualCases() {
        doReturn(true).when(permissionServiceMock).isTimesheetAdmin(userMock);

        Mockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        Mockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);

        Group group = mock(Group.class);
        Mockito.when(group.getName()).thenReturn("DISABLED");

        spyUserRest.getUsers(httpRequestMock);

        verify(user1, times(1)).getDisplayName();
        verify(user2, times(1)).getDisplayName();

        Assert.assertEquals(2, usersSet.size());
    }

    @Test
    public void testGetGroupsOk() {
        when(permissionServiceMock.checkRootPermission()).thenReturn(null);
        List<Group> groupList = new ArrayList<>();
        groupList.add(mock(Group.class));
        groupList.add(mock(Group.class));
        when(groupPickerSearchService.findGroups("")).thenReturn(groupList);

        Response response = spyUserRest.getGroups(httpRequestMock);
        assertEquals(2, ((List<Group>)response.getEntity()).size());
    }

    @Test
    public void testUserInformationForDeletedUser() {
        when(userManager.getUserByKey("joh")).thenReturn(null);
        when(permissionServiceMock.checkRootPermission()).thenReturn(null);

        Response response = spyUserRest.getUserInformation(httpRequestMock);
        assertEquals(1, ((List<JsonUserInformation>) response.getEntity()).size());
    }

    @Test
    public void testUsersForCoordinatorWithDeletedUser() throws PermissionException {
        when(userManager.getUserByKey("joh")).thenReturn(null);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(permissionServiceMock.isUserTeamCoordinator(userMock)).thenReturn(true);
        when(permissionServiceMock.isUserCoordinatorOfTimesheet(eq(userMock), any())).thenReturn(true);

        Response response = spyUserRest.getUsersForCoordinator(httpRequestMock, timesheetId);
        assertEquals(0, ((List<JsonUserInformation>) response.getEntity()).size());
    }
    
    @Test
    public void testUserInformation() {
        when(permissionServiceMock.checkRootPermission()).thenReturn(null);
        Response response = spyUserRest.getUserInformation(httpRequestMock);
        assertEquals(2, ((List<JsonUserInformation>)response.getEntity()).size());
    }

    @Test
    public void testUsersForCoordinator() throws PermissionException {
        String teamGroupName = "TestGroup";

        Team teamMock = mock(Team.class);
        org.catrobat.jira.timesheet.activeobjects.Group groupMock = mock(org.catrobat.jira.timesheet.activeobjects.Group.class);
        when(teamMock.getTeamName()).thenReturn("TestTeam");
        org.catrobat.jira.timesheet.activeobjects.Group[] groupList = {groupMock};
        when(teamMock.getGroups()).thenReturn(groupList);
        when(groupMock.getGroupName()).thenReturn(teamGroupName);
        when(groupManager.isUserInGroup(chris, teamGroupName)).thenReturn(true);
        when(groupManager.isUserInGroup(joh, teamGroupName)).thenReturn(false);

        Timesheet timesheet = mock(Timesheet.class);
        Timesheet timesheet2 = mock(Timesheet.class);
        when(timesheet.getUserKey()).thenReturn("chris");
        when(timesheet2.getUserKey()).thenReturn("joh");
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(permissionServiceMock.isUserTeamCoordinator(userMock)).thenReturn(true);

        when(teamServiceMock.getTeamsOfCoordinator(userMock.getUsername())).thenReturn(new HashSet<Team>(Arrays.asList(teamMock)));
        when(teamServiceMock.all()).thenReturn(Arrays.asList(teamMock));

        Response response = spyUserRest.getUsersForCoordinator(httpRequestMock, timesheetId);
        assertEquals(1, ((List<JsonUserInformation>)response.getEntity()).size());
        assertEquals(chris.getUsername(), ((List<JsonUserInformation>)response.getEntity()).get(0).getUserName());
    }

    @Test
    public void testPairProgrammingUsers() {
        Response response = spyUserRest.getPairProgrammingUsers(httpRequestMock);
        assertEquals(2, ((List<String>)response.getEntity()).size());
    }
}
