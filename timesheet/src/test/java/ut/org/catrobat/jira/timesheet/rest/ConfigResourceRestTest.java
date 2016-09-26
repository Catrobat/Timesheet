package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserKeyService;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.mail.queue.MailQueue;
import com.atlassian.sal.api.user.UserManager;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.rest.ConfigResourceRest;
import org.catrobat.jira.timesheet.rest.TimesheetRest;
import org.catrobat.jira.timesheet.rest.json.JsonCategory;
import org.catrobat.jira.timesheet.rest.json.JsonConfig;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.*;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest({ComponentAccessor.class, ConfigResourceRest.class, TimesheetService.class,
        TimesheetEntryService.class})
public class ConfigResourceRestTest {

    private ComponentAccessor componentAccessorMock;

    private com.atlassian.sal.api.user.UserManager userManagerLDAPMock;
    private com.atlassian.jira.user.util.UserManager userManagerJiraMock;
    private UserKeyService userKeyServiceMock;
    private GroupManager groupManagerJiraMock;

    private CategoryService categoryService;
    private ConfigService configService;
    private TimesheetService timesheetService;
    private TimesheetEntryService timesheetEntryService;
    private TeamService teamService;
    private PermissionService permissionService;

    private TimesheetService timesheetServiceMock;
    private TeamService teamServiceMock;
    private CategoryService categoryServiceMock;
    private TimesheetEntryService timesheetEntryServiceMock;
    private PermissionService permissionServiceMock;
    private ConfigService configServiceMock;

    private UserUtil userUtilMock;
    private ApplicationUser userProfileMock;
    private Timesheet timesheetMock;
    private Category categoryMock;
    private Team teamMock;
    private TimesheetEntry timesheetEntryMock;

    private ConfigResourceRest configResourceRest;
    private ConfigResourceRest configResourceRestMock;
    private TimesheetRest spyTimesheetRest;

    private Response response;
    private HttpServletRequest request;

    private MailQueue mailQueueMock;

    private SimpleDateFormat sdf;

    private TestActiveObjects ao;
    private EntityManager entityManager;
    private UserManager userManager;
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        componentAccessorMock = Mockito.mock(ComponentAccessor.class, RETURNS_DEEP_STUBS);
        userManagerLDAPMock = Mockito.mock(com.atlassian.sal.api.user.UserManager.class, RETURNS_DEEP_STUBS);
        userManagerJiraMock = Mockito.mock(com.atlassian.jira.user.util.UserManager.class, RETURNS_DEEP_STUBS);
        groupManagerJiraMock = Mockito.mock(GroupManager.class, RETURNS_DEEP_STUBS);
        userKeyServiceMock = Mockito.mock(UserKeyService.class, RETURNS_DEEP_STUBS);
        userUtilMock = Mockito.mock(UserUtil.class, RETURNS_DEEP_STUBS);
        configServiceMock = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        categoryServiceMock = mock(CategoryService.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        timesheetEntryServiceMock = mock(TimesheetEntryService.class, RETURNS_DEEP_STUBS);
        timesheetServiceMock = mock(TimesheetService.class, RETURNS_DEEP_STUBS);
        teamServiceMock = mock(TeamService.class, RETURNS_DEEP_STUBS);
        request = Mockito.mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        mailQueueMock = Mockito.mock(MailQueue.class, RETURNS_DEEP_STUBS);
        jiraAuthenticationContext = Mockito.mock(JiraAuthenticationContext.class, RETURNS_DEEP_STUBS);

        categoryService = new CategoryServiceImpl(ao);
        configService = new ConfigServiceImpl(ao, categoryService, userManager);
        teamService = new TeamServiceImpl(ao, configService);
        permissionService = new PermissionServiceImpl(userManagerLDAPMock, teamService, configService);
        timesheetEntryService = new TimesheetEntryServiceImpl(ao);
        timesheetService = new TimesheetServiceImpl(ao);

        userProfileMock = Mockito.mock(ApplicationUser.class);
        timesheetMock = Mockito.mock(Timesheet.class);
        categoryMock = Mockito.mock(Category.class);
        teamMock = Mockito.mock(Team.class);
        timesheetEntryMock = Mockito.mock(TimesheetEntry.class);

        configResourceRest = new ConfigResourceRest(userManagerLDAPMock, configService, teamService, categoryService, permissionService);

        configResourceRestMock = new ConfigResourceRest(userManagerLDAPMock, configServiceMock, teamServiceMock, categoryServiceMock, permissionServiceMock);

        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);
        PowerMockito.when(ComponentAccessor.getUserKeyService()).thenReturn(userKeyServiceMock);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
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

        Set<ApplicationUser> usersSet = new HashSet<ApplicationUser>(Arrays.asList(user1, user2));
        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfileMock);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        response = configResourceRest.getTeams(request);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        Assert.assertNotNull(responseTeamList);
    }

    @Test
    public void testGetCategoriesOk() throws Exception {
        response = configResourceRest.getCategories(request);
        List<JsonCategory> responseTeamList = (List<JsonCategory>) response.getEntity();
        Assert.assertNotNull(responseTeamList);
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

        Set<ApplicationUser> usersSet = new HashSet<>(Arrays.asList(user1, user2));
        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);

        when(permissionServiceMock.checkIfUserExists(request)).thenReturn(userProfileMock);
        when(permissionServiceMock.checkPermission(request)).thenReturn(response);
        when(categoryServiceMock.removeCategory(anyString())).thenReturn(true);

        response = configResourceRestMock.removeCategory(anyString(), request);
        Assert.assertNull(response.getEntity());
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

        when(permissionServiceMock.checkIfUserExists(request)).thenReturn(userProfileMock);
        when(permissionServiceMock.checkPermission(request)).thenReturn(response);
        when(configServiceMock.getConfiguration().getTeams()).thenReturn(teams);

        response = configResourceRestMock.getTeamList(request);
        Assert.assertNotNull(response.getEntity());
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

        ApprovedUser approvedUser1 = Mockito.mock(ApprovedUser.class);
        when(approvedUser1.getUserKey()).thenReturn("USER_KEY1");
        when(approvedUser1.getUserName()).thenReturn("User1");

        ApprovedUser approvedUser2 = Mockito.mock(ApprovedUser.class);
        when(approvedUser2.getUserKey()).thenReturn("USER_KEY2");
        when(approvedUser2.getUserName()).thenReturn("User2");

        ApprovedUser[] approvedUsers = {approvedUser1, approvedUser2};

        ApprovedGroup approvedGroup = Mockito.mock(ApprovedGroup.class);
        when(approvedGroup.getGroupName()).thenReturn("ApprovedGroup");

        ApprovedGroup[] approvedGroups = {approvedGroup};


        when(permissionServiceMock.checkIfUserExists(request)).thenReturn(userProfileMock);
        when(permissionServiceMock.checkPermission(request)).thenReturn(response);
        when(configServiceMock.getConfiguration().getTeams()).thenReturn(teams);
        when(configServiceMock.getConfiguration().getApprovedGroups()).thenReturn(approvedGroups);
        when(configServiceMock.getConfiguration().getApprovedUsers()).thenReturn(approvedUsers);

        response = configResourceRestMock.getConfig(request);
        Assert.assertNotNull(response.getEntity());
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

        ApprovedUser approvedUser1 = Mockito.mock(ApprovedUser.class);
        when(approvedUser1.getUserKey()).thenReturn("USER_KEY1");
        when(approvedUser1.getUserName()).thenReturn("User1");

        ApprovedUser approvedUser2 = Mockito.mock(ApprovedUser.class);
        when(approvedUser2.getUserKey()).thenReturn("USER_KEY2");
        when(approvedUser2.getUserName()).thenReturn("User2");

        ApprovedUser[] approvedUsers = {approvedUser1, approvedUser2};

        ApprovedGroup approvedGroup = Mockito.mock(ApprovedGroup.class);
        when(approvedGroup.getGroupName()).thenReturn("ApprovedGroup");

        ApprovedGroup[] approvedGroups = {approvedGroup};


        when(permissionServiceMock.checkIfUserExists(request)).thenReturn(userProfileMock);
        when(permissionServiceMock.checkPermission(request)).thenReturn(response);
        when(configServiceMock.getConfiguration().getTeams()).thenReturn(teams);
        when(configServiceMock.getConfiguration().getApprovedGroups()).thenReturn(approvedGroups);
        when(configServiceMock.getConfiguration().getApprovedUsers()).thenReturn(approvedUsers);

        JsonConfig jsonConfig = new JsonConfig(configServiceMock);

        response = configResourceRestMock.setConfig(jsonConfig,  request);
        Assert.assertNull(response.getEntity());
    }
}
