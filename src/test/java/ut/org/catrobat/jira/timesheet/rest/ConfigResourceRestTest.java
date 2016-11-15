package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.rest.ConfigResourceRest;
import org.catrobat.jira.timesheet.rest.json.JsonCategory;
import org.catrobat.jira.timesheet.rest.json.JsonConfig;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.*;
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
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest({ComponentAccessor.class, ConfigResourceRest.class, TimesheetService.class,
        TimesheetEntryService.class})
public class ConfigResourceRestTest {

    private UserManager userManagerJiraMock;

    private CategoryService categoryService;
    private ConfigService configService;
    private TeamService teamService;

    private TeamService teamServiceMock;
    private CategoryService categoryServiceMock;
    private PermissionService permissionServiceMock;
    private ConfigService configServiceMock;

    private UserUtil userUtilMock;
    private Category categoryMock;

    private ConfigResourceRest configResourceRest;
    private ConfigResourceRest configResourceRestMock;

    private javax.ws.rs.core.Response response;
    private HttpServletRequest request;

    private TestActiveObjects ao;
    private EntityManager entityManager;
    private ApplicationUser userMock;
    private JiraAuthenticationContext jiraAuthMock;
    private GroupManager groupManagerMock;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        userManagerJiraMock = mock(UserManager.class, RETURNS_DEEP_STUBS);
        userUtilMock = mock(UserUtil.class, RETURNS_DEEP_STUBS);
        configServiceMock = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        categoryServiceMock = mock(CategoryService.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        teamServiceMock = mock(TeamService.class, RETURNS_DEEP_STUBS);
        request = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        categoryMock = mock(Category.class, RETURNS_DEEP_STUBS);
        userMock = mock(ApplicationUser.class, RETURNS_DEEP_STUBS);
        jiraAuthMock = mock(JiraAuthenticationContext.class, RETURNS_DEEP_STUBS);
        groupManagerMock = mock(GroupManager.class, RETURNS_DEEP_STUBS);

        categoryService = new CategoryServiceImpl(ao);
        configService = new ConfigServiceImpl(ao, categoryService);
        teamService = new TeamServiceImpl(ao, configService);
        configResourceRest = new ConfigResourceRest(configService, teamService, categoryService, permissionServiceMock);

        configResourceRestMock = new ConfigResourceRest(configServiceMock, teamServiceMock, categoryServiceMock, permissionServiceMock);

        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthMock);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(userMock);
        PowerMockito.when(ComponentAccessor.getGroupManager()).thenReturn(groupManagerMock);

        //additional mocks
        when(permissionServiceMock.checkIfUserExists(request)).thenReturn(userMock);
        when(permissionServiceMock.checkPermission(request)).thenReturn(null);
    }

    @Test
    public void testGetTeamsOk() throws Exception {
        String userName = "test";
        String userKey = "USER_KEY_1";

        Category[] categories = {categoryMock};

        Team team1 = Mockito.mock(Team.class);
        when(team1.getID()).thenReturn(1);
        when(team1.getTeamName()).thenReturn("Catroid");
        when(team1.getCategories()).thenReturn(categories);

        Team team2 = Mockito.mock(Team.class);
        when(team2.getID()).thenReturn(2);
        when(team2.getTeamName()).thenReturn("IRC");
        when(team2.getCategories()).thenReturn(new Category[0]);

        Set<Team> teams = new HashSet<Team>();
        teams.add(team1);
        teams.add(team2);

        ApplicationUser user1 = mock(ApplicationUser.class);
        ApplicationUser user2 = mock(ApplicationUser.class);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);

        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        response = configResourceRest.getTeams(request);
        System.out.println(response.getEntity());
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        assertNotNull(responseTeamList);
    }

    @Test
    public void testGetCategoriesOk() throws Exception {
        response = configResourceRest.getCategories(request);
        List<JsonCategory> responseTeamList = (List<JsonCategory>) response.getEntity();
        assertNotNull(responseTeamList);
    }

    @Test
    public void testRemoveCategoryOk() throws Exception {
        String userName = "test";
        String userKey = "USER_KEY_1";

        Category[] categories = {categoryMock};

        Team team1 = Mockito.mock(Team.class);
        when(team1.getID()).thenReturn(1);
        when(team1.getTeamName()).thenReturn("Catroid");
        when(team1.getCategories()).thenReturn(categories);

        Team team2 = Mockito.mock(Team.class);
        when(team2.getID()).thenReturn(2);
        when(team2.getTeamName()).thenReturn("IRC");
        when(team2.getCategories()).thenReturn(new Category[0]);

        Set<Team> teams = new HashSet<>();
        teams.add(team1);
        teams.add(team2);

        ApplicationUser user1 = mock(ApplicationUser.class);
        ApplicationUser user2 = mock(ApplicationUser.class);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);


        when(permissionServiceMock.checkPermission(request)).thenReturn(response);
        when(categoryServiceMock.removeCategory(anyString())).thenReturn(true);

        response = configResourceRestMock.removeCategory(anyString(), request);
        assertNull(response.getEntity());
    }

    @Test
    public void testGetTeamListOk() throws Exception {
        String userName = "test";
        String userKey = "USER_KEY_1";

        Category[] categories = {categoryMock};

        Team team1 = Mockito.mock(Team.class);
        when(team1.getID()).thenReturn(1);
        when(team1.getTeamName()).thenReturn("Catroid");
        when(team1.getCategories()).thenReturn(categories);

        Team team2 = Mockito.mock(Team.class);
        when(team2.getID()).thenReturn(2);
        when(team2.getTeamName()).thenReturn("IRC");
        when(team2.getCategories()).thenReturn(new Category[0]);

        Team[] teams = {team1, team2};

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);


        when(permissionServiceMock.checkPermission(request)).thenReturn(response);
        when(configServiceMock.getConfiguration().getTeams()).thenReturn(teams);

        response = configResourceRestMock.getTeamList(request);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testGetConfigOk() throws Exception {
        Category[] categories = {categoryMock};

        Team team1 = Mockito.mock(Team.class);
        when(team1.getID()).thenReturn(1);
        when(team1.getTeamName()).thenReturn("Catroid");
        when(team1.getCategories()).thenReturn(categories);

        Team team2 = Mockito.mock(Team.class);
        when(team2.getID()).thenReturn(2);
        when(team2.getTeamName()).thenReturn("IRC");
        when(team2.getCategories()).thenReturn(new Category[0]);

        Team[] teams = {team1, team2};

        TimesheetAdmin timesheetAdmin1 = Mockito.mock(TimesheetAdmin.class);
        when(timesheetAdmin1.getUserKey()).thenReturn("USER_KEY1");
        when(timesheetAdmin1.getUserName()).thenReturn("User1");

        TimesheetAdmin timesheetAdmin2 = Mockito.mock(TimesheetAdmin.class);
        when(timesheetAdmin2.getUserKey()).thenReturn("USER_KEY2");
        when(timesheetAdmin2.getUserName()).thenReturn("User2");

        TimesheetAdmin[] timesheetAdmins = {timesheetAdmin1, timesheetAdmin2};

        TSAdminGroup timesheetAdminGroup = Mockito.mock(TSAdminGroup.class);
        when(timesheetAdminGroup.getGroupName()).thenReturn("TSAdminGroup");

        TSAdminGroup[] timesheetAdminGroups = {timesheetAdminGroup};

        when(permissionServiceMock.checkPermission(request)).thenReturn(response);
        when(configServiceMock.getConfiguration().getTeams()).thenReturn(teams);
        when(configServiceMock.getConfiguration().getTimesheetAdminGroups()).thenReturn(timesheetAdminGroups);
        when(configServiceMock.getConfiguration().getTimesheetAdminUsers()).thenReturn(timesheetAdmins);

        response = configResourceRestMock.getConfig(request);
        assertNotNull(response.getEntity());
    }


    @Test
    public void testSetConfigOk() throws Exception {
        Category[] categories = {categoryMock};

        Team team1 = Mockito.mock(Team.class);
        when(team1.getID()).thenReturn(1);
        when(team1.getTeamName()).thenReturn("Catroid");
        when(team1.getCategories()).thenReturn(categories);

        Team team2 = Mockito.mock(Team.class);
        when(team2.getID()).thenReturn(2);
        when(team2.getTeamName()).thenReturn("IRC");
        when(team2.getCategories()).thenReturn(new Category[0]);

        Team[] teams = {team1, team2};

        TimesheetAdmin timesheetAdmin1 = Mockito.mock(TimesheetAdmin.class);
        when(timesheetAdmin1.getUserKey()).thenReturn("USER_KEY1");
        when(timesheetAdmin1.getUserName()).thenReturn("User1");

        TimesheetAdmin timesheetAdmin2 = Mockito.mock(TimesheetAdmin.class);
        when(timesheetAdmin2.getUserKey()).thenReturn("USER_KEY2");
        when(timesheetAdmin2.getUserName()).thenReturn("User2");

        TimesheetAdmin[] timesheetAdmins = {timesheetAdmin1, timesheetAdmin2};

        TSAdminGroup timesheetAdminGroup = Mockito.mock(TSAdminGroup.class);
        when(timesheetAdminGroup.getGroupName()).thenReturn("TSAdminGroup");

        TSAdminGroup[] timesheetAdminGroups = {timesheetAdminGroup};

        ApplicationUser user1 = mock(ApplicationUser.class);
        ApplicationUser user2 = mock(ApplicationUser.class);

        Collection<ApplicationUser> usersInGroup = new ArrayList();
        usersInGroup.add(user1);
        usersInGroup.add(user2);

        when(permissionServiceMock.checkPermission(request)).thenReturn(response);
        when(configServiceMock.getConfiguration().getTeams()).thenReturn(teams);
        when(configServiceMock.getConfiguration().getTimesheetAdminGroups()).thenReturn(timesheetAdminGroups);
        when(configServiceMock.getConfiguration().getTimesheetAdminUsers()).thenReturn(timesheetAdmins);

        PowerMockito.when(ComponentAccessor.getGroupManager().getUsersInGroup(anyString())).thenReturn(usersInGroup);

        JsonConfig jsonConfig = new JsonConfig(configServiceMock);

        response = configResourceRestMock.setConfig(jsonConfig, request);
        assertNull(response.getEntity());
    }

    @Test
    public void testCannotRenameCategoryNameAlreadyExists() throws Exception {
        configResourceRest.addCategory("Test", request);
        configResourceRest.addCategory("Hallo", request);
        String[] renamePair = new String[] {"Test","Hallo"};
        response = configResourceRest.editCategoryName(renamePair, request);
        assertEquals(409, response.getStatus());
    }
}
