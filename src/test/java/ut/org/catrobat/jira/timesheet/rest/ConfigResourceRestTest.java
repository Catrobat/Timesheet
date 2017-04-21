package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.TSAdminGroup;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.TimesheetAdmin;
import org.catrobat.jira.timesheet.rest.ConfigResourceRest;
import org.catrobat.jira.timesheet.rest.json.JsonConfig;
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
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest({ComponentAccessor.class, ConfigResourceRest.class, TimesheetService.class,
        TimesheetEntryService.class})
public class ConfigResourceRestTest {

    private TeamService teamServiceMock;
    private PermissionService permissionServiceMock;
    private ConfigService configServiceMock;

    private Category categoryMock;

    private ConfigResourceRest configResourceRest;
    private ConfigResourceRest configResourceRestMock;

    private javax.ws.rs.core.Response response;
    private HttpServletRequest request;

    private EntityManager entityManager;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        TestActiveObjects ao = new TestActiveObjects(entityManager);

        UserManager userManagerJiraMock = mock(UserManager.class, RETURNS_DEEP_STUBS);
        configServiceMock = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        CategoryService categoryServiceMock = mock(CategoryService.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        teamServiceMock = mock(TeamService.class, RETURNS_DEEP_STUBS);
        request = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        categoryMock = mock(Category.class, RETURNS_DEEP_STUBS);
        ApplicationUser userMock = mock(ApplicationUser.class, RETURNS_DEEP_STUBS);
        JiraAuthenticationContext jiraAuthMock = mock(JiraAuthenticationContext.class, RETURNS_DEEP_STUBS);
        GroupManager groupManagerMock = mock(GroupManager.class, RETURNS_DEEP_STUBS);

        CategoryService categoryService = new CategoryServiceImpl(ao);
        TimesheetService timesheetService = new TimesheetServiceImpl(ao);
        TimesheetEntryService entryService = new TimesheetEntryServiceImpl(ao, timesheetService);
        TeamService teamService = new TeamServiceImpl(ao, categoryService, entryService);
        ConfigService configService = new ConfigServiceImpl(ao, categoryService, teamService);
        configResourceRest = new ConfigResourceRest(configService, teamService, categoryService, permissionServiceMock, ao);

        configResourceRestMock = new ConfigResourceRest(configServiceMock, teamServiceMock, categoryServiceMock, permissionServiceMock, ao);

        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthMock);
        PowerMockito.when(ComponentAccessor.getGroupManager()).thenReturn(groupManagerMock);

        //additional mocks
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(permissionServiceMock.checkUserPermission()).thenReturn(null);
        when(permissionServiceMock.checkRootPermission()).thenReturn(null);
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

        Set<Team> teams = new HashSet<>();
        teams.add(team1);
        teams.add(team2);

        ApplicationUser user1 = mock(ApplicationUser.class);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);

        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        response = configResourceRest.getTeams(request);
        Object responseTeamList = response.getEntity();
        assertNotNull(responseTeamList);
    }

    @Test
    public void testGetCategoriesOk() throws Exception {
        response = configResourceRest.getCategories(request);
        Object responseTeamList = response.getEntity();
        assertNotNull(responseTeamList);
    }

    @Test
    public void testGetCategoriesUserDoesNotExist() throws Exception {
        //preparations
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);
        Response userDoesNotExist = Response.status(Response.Status.UNAUTHORIZED).entity("User does not exist.").build();
        PowerMockito.when(permissionServiceMock.checkUserPermission()).thenReturn(userDoesNotExist);
        //execution & verifying
        response = configResourceRest.getCategories(request);
        assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testAddTeamOk() {
        response = configResourceRest.addTeam("NewTeam", request);
        assertNull(response.getEntity());
    }

    @Test
    public void testRemoveTeamNotFound() {
        response = configResourceRest.removeTeam("ToDelete", request);
        assertEquals("Team not found.", response.getEntity());
    }

    @Test
    public void testRemoveTeamOk() {
        configResourceRest.addTeam("ToDelete", request);
        response = configResourceRest.removeTeam("ToDelete", request);
        assertNull(response.getEntity());
    }

    @Test
    public void testEditTeamNameNotFound() {
        String[] renameTeam = {"NotFound", "NewName"};
        response = configResourceRest.editTeamPermission(renameTeam, request);
        assertEquals(403, response.getStatus()); // TODO: change to entity
    }

    @Test
    public void testEditTeamNameOk() {
        configResourceRest.addTeam("ToRename", request);
        String[] renameTeam = {"ToRename", "NewName"};
        response = configResourceRest.editTeamPermission(renameTeam, request);
        assertNull(response.getEntity());
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

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);

        when(permissionServiceMock.checkUserPermission()).thenReturn(response);

        response = configResourceRestMock.removeCategory("Meeting", request);
        assertNull(response.getEntity());
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

        when(permissionServiceMock.checkUserPermission()).thenReturn(response);
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

        Collection<ApplicationUser> usersInGroup = new ArrayList<>();
        usersInGroup.add(user1);
        usersInGroup.add(user2);

        when(permissionServiceMock.checkUserPermission()).thenReturn(response);
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
        String[] renamePair = new String[]{"Test", "Hallo"};
        response = configResourceRest.editCategoryName(renamePair, request);
        assertEquals(409, response.getStatus());
    }
}
