package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserKeyService;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.mail.queue.MailQueue;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.rest.TimesheetRest;
import org.catrobat.jira.timesheet.rest.json.JsonCategory;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.CategoryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TeamServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetEntryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
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
@PrepareForTest({ComponentAccessor.class, TimesheetRest.class, TimesheetService.class,
        TimesheetEntryService.class})
public class TimesheetRestTest {

    private UserManager userManagerJiraMock;
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
    private Timesheet timesheetMock;
    private Category categoryMock;
    private Team teamMock;
    private TimesheetEntry timesheetEntryMock;

    private TimesheetRest timesheetRest;
    private TimesheetRest timesheetRestMock;
    private TimesheetRest spyTimesheetRest;

    private javax.ws.rs.core.Response response;
    private HttpServletRequest requestMock;

    private MailQueue mailQueueMock;

    private TestActiveObjects ao;
    private EntityManager entityManager;
    private ApplicationUser userMock;
    private JiraAuthenticationContext jiraAuthMock;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        userManagerJiraMock = mock(UserManager.class, RETURNS_DEEP_STUBS);
        groupManagerJiraMock = mock(GroupManager.class, RETURNS_DEEP_STUBS);
        userKeyServiceMock = mock(UserKeyService.class, RETURNS_DEEP_STUBS);
        userUtilMock = mock(UserUtil.class, RETURNS_DEEP_STUBS);
        configServiceMock = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        categoryServiceMock = mock(CategoryService.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        timesheetEntryServiceMock = mock(TimesheetEntryService.class, RETURNS_DEEP_STUBS);
        timesheetServiceMock = mock(TimesheetService.class, RETURNS_DEEP_STUBS);
        teamServiceMock = mock(TeamService.class, RETURNS_DEEP_STUBS);
        requestMock = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        mailQueueMock = mock(MailQueue.class, RETURNS_DEEP_STUBS);
        timesheetMock = mock(Timesheet.class, RETURNS_DEEP_STUBS);
        categoryMock = mock(Category.class, RETURNS_DEEP_STUBS);
        teamMock = mock(Team.class, RETURNS_DEEP_STUBS);
        timesheetEntryMock = mock(TimesheetEntry.class, RETURNS_DEEP_STUBS);
        userMock = mock(ApplicationUser.class, RETURNS_DEEP_STUBS);
        jiraAuthMock = mock(JiraAuthenticationContext.class, RETURNS_DEEP_STUBS);

        categoryService = new CategoryServiceImpl(ao);
        configService = new ConfigServiceImpl(ao, categoryService);
        teamService = new TeamServiceImpl(ao, configService);
        timesheetEntryService = new TimesheetEntryServiceImpl(ao);
        timesheetService = new TimesheetServiceImpl(ao);
        timesheetRest = new TimesheetRest(timesheetEntryService, timesheetService, categoryService, teamService, permissionServiceMock, configService);

        timesheetRestMock = new TimesheetRest(timesheetEntryServiceMock, timesheetServiceMock, categoryServiceMock, teamServiceMock, permissionServiceMock, configServiceMock);

        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);
        PowerMockito.when(ComponentAccessor.getUserKeyService()).thenReturn(userKeyServiceMock);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthMock);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(userMock);
        PowerMockito.when(ComponentAccessor.getGroupManager()).thenReturn(groupManagerJiraMock);
        PowerMockito.when(permissionServiceMock.checkUserPermission()).thenReturn(null);
    }

    @Test
    public void testGetTeamsForTimesheetOk() throws Exception {
        int timesheetID = 1;
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

        Set<ApplicationUser> usersSet = new HashSet<>(Arrays.asList(user1, user2));
        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);

        response = timesheetRestMock.getTeamsForUser(requestMock);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        assertNotNull(responseTeamList);
    }

    @Test
    public void testGetTeamsForTimesheetReturnNoTeams() throws Exception {
        int timesheetID = 1;
        String username = "testUser";
        String userKey = "USER_KEY_1";

        List<JsonTeam> expectedTeams = new LinkedList<JsonTeam>();
        Set<Team> teams = new HashSet<Team>();

        when(teamServiceMock.getTeamsOfUser(username)).thenReturn(teams);

        ApplicationUser user1 = mock(ApplicationUser.class);
        ApplicationUser user2 = mock(ApplicationUser.class);

        Set<ApplicationUser> usersSet = new HashSet<>(Arrays.asList(user1, user2));

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        PowerMockito.when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

//        response = timesheetRestMock.getTeamsForUser(requestMock);

//        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
//        assertEquals(responseTeamList, expectedTeams);
    }

    @Test
    public void testGetTeamsForTimesheetUserDoesNotExist() throws Exception {
        //preparations
        int timesheetID = 1;
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);

        //execution & verifying
//        response = timesheetRestMock.getTeamsForUser(requestMock);
//        assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testGetTeamsForUserOk() throws Exception {
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

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        response = timesheetRestMock.getTeamsForUser(requestMock);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        assertNotNull(responseTeamList);
    }

    @Test
    public void testGetTeamsForUserReturnNoTeams() throws Exception {
        String username = "testUser";
        String userKey = "USER_KEY_1";

        List<JsonTeam> expectedTeams = new LinkedList<JsonTeam>();
        Set<Team> teams = new HashSet<Team>();

        when(teamServiceMock.getTeamsOfUser(username)).thenReturn(teams);

        ApplicationUser user1 = mock(ApplicationUser.class);
        ApplicationUser user2 = mock(ApplicationUser.class);

        Set<ApplicationUser> usersSet = new HashSet<>(Arrays.asList(user1, user2));

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);

        PowerMockito.when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        response = timesheetRest.getTeamsForUser(requestMock);

        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        assertEquals(responseTeamList, expectedTeams);
    }

    @Test
    public void testGetTeamsUserDoesNotExist() throws Exception {
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);
        PowerMockito.when(permissionServiceMock.checkIfUserExists()).thenThrow(new PermissionException("User does not exist."));
        //execution & verifying
        response = timesheetRest.getTeamsForUser(requestMock);

        assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testGetCategoriesOk() throws Exception {
        response = timesheetRest.getCategories(requestMock);
        List<JsonCategory> responseTeamList = (List<JsonCategory>) response.getEntity();
        assertNotNull(responseTeamList);
    }

    @Test
    public void testGetCategoriesUserDoesNotExist() throws Exception {
        //preparations
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);
        Response userDoesNotExist = Response.status(Response.Status.UNAUTHORIZED).entity("User does not exist.").build();
        PowerMockito.when(permissionServiceMock.checkUserPermission()).thenReturn(userDoesNotExist);
        //execution & verifying
        response = timesheetRest.getCategories(requestMock);
        assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testGetTimesheetEntriesOfAllTeamMembersOk() throws Exception {
        //preparations
        int timesheetID = 1;
        String username = "testUser";
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

        Set<ApplicationUser> usersSet = new HashSet<>(Arrays.asList(user1, user2));

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);

        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        List<String> developerList = new LinkedList<String>();
        developerList.add(username);
        developerList.add("asdf");
        developerList.add("jklö");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);
        when(configServiceMock.getGroupsForRole(team1.getTeamName(), TeamToGroup.Role.DEVELOPER)).thenReturn(developerList);

        Date today = new Date();

        TimesheetEntry timesheetEntry = timesheetEntryServiceMock.add(timesheetMock, today, today, categoryMock,
                "Test Entry", 0, team1, false, today, today, "CAT-1530", "Partner");
        TimesheetEntry[] timesheetEntries = {timesheetEntry};

        when(timesheetServiceMock.getTimesheetByUser(userKey, false)).thenReturn(timesheetMock);
        when(timesheetEntryServiceMock.getEntriesBySheet(timesheetMock)).thenReturn(timesheetEntries);

        //execution & verifying
        response = timesheetRestMock.getTimesheetEntriesOfAllTeamMembers(requestMock, timesheetID);

//        List<JsonTimesheetEntry> responseTimesheetEntries = (List<JsonTimesheetEntry>) response.getEntity();
//        assertNotNull(responseTimesheetEntries);
    }

    @Test
    public void testGetTimesheetEntriesOfAllTeamMembersWrongUser() throws Exception {
        //preparations
        int timesheetID = 1;
        String username = "testUser";
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

        Set<ApplicationUser> usersSet = new HashSet<>(Arrays.asList(user1, user2));

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);

        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        List<String> developerList = new LinkedList<String>();
        developerList.add("asdf");
        developerList.add("jklö");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);
        when(configServiceMock.getGroupsForRole(team1.getTeamName(), TeamToGroup.Role.DEVELOPER)).thenReturn(developerList);

        //execution & verifying
        response = timesheetRestMock.getTimesheetEntriesOfAllTeamMembers(requestMock, timesheetID);

        List<JsonTimesheetEntry> expectedList = new LinkedList<JsonTimesheetEntry>();
//        List<JsonTimesheetEntry> responseTimesheetEntries = (List<JsonTimesheetEntry>) response.getEntity();
//        assertEquals(responseTimesheetEntries, expectedList);
    }

    @Test
    public void testGetTimesheetEntriesOfAllTeamMembersUserDoesNotExist() throws Exception {
        //preparations
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);
        PowerMockito.when(permissionServiceMock.checkIfUserExists()).thenThrow(new PermissionException("User does not exist."));
        //execution & verifying
        response = timesheetRest.getTimesheetEntriesOfAllTeamMembers(requestMock, 1);
        assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testGetAllTimesheetEntriesForTeamOk() throws Exception {
        String teamName = "Catroid";
        String userName = "testUser";
        String userKey = "USER_KEY_1";

        Category[] categories = {categoryMock};

        Team team1 = Mockito.mock(Team.class);
        when(team1.getID()).thenReturn(1);
        when(team1.getTeamName()).thenReturn("Catroid");
        when(team1.getCategories()).thenReturn(categories);

        ApplicationUser user1 = mock(ApplicationUser.class);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        List<String> developerList = new LinkedList<String>();
        developerList.add(userName);
        Date today = new Date();

        TimesheetEntry timesheetEntry = timesheetEntryServiceMock.add(timesheetMock, today, today, categoryMock,
                "Test Entry", 0, team1, false, today, today, "CAT-1530", "Partner");
        TimesheetEntry[] timesheetEntries = {timesheetEntry};

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);
        when(configServiceMock.getGroupsForRole(teamName, TeamToGroup.Role.DEVELOPER)).thenReturn(developerList);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);
        when(ComponentAccessor.getUserKeyService().getKeyForUsername(userName)).thenReturn(userKey);

        when(timesheetServiceMock.getTimesheetByUser(userKey, false)).thenReturn(timesheetMock);
        when(timesheetMock.getEntries()).thenReturn(timesheetEntries);

        when(timesheetEntry.getTeam().getTeamName()).thenReturn(teamName);

        //execution & verifying
        response = timesheetRestMock.getAllTimesheetEntriesForTeam(requestMock, teamName);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testGetAllTimesheetEntriesForTeamUserDoesNotExist() throws Exception {
        //preparations
        String teamName = "Catroid";
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);
        PowerMockito.when(permissionServiceMock.checkIfUserExists()).thenThrow(new PermissionException("User does not exist."));
        //execution & verifying
        response = timesheetRest.getAllTimesheetEntriesForTeam(requestMock, teamName);
        assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testGetTimesheetIDOfUserOk() throws Exception {
        String userName = "test";
        Boolean isMTSheet = false;
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRestMock.getTimesheetIDOFUser(requestMock, userName, isMTSheet);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testGetOwnerOfTimesheetOk() throws Exception {
        //preparations
        int timesheetID = 1;
        String userName = "test";
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(timesheetServiceMock.getTimesheetByID(timesheetID)).thenReturn(timesheetMock);

        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);

        //execution & verifying
        response = timesheetRestMock.getOwnerOfTimesheet(requestMock, timesheetID);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testGetOwnerOfTimesheetAccessDenied() throws Exception {
        //preparations
        int timesheetID = 1;
        String userName = "test";
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(timesheetServiceMock.getTimesheetByID(timesheetID)).thenReturn(timesheetMock);
        when(permissionServiceMock.userCanViewTimesheet(Matchers.any(), Matchers.any())).thenReturn(false);

        //execution & verifying
        response = timesheetRestMock.getOwnerOfTimesheet(requestMock, timesheetID);
        assertEquals(response.getEntity(), "Timesheet Access denied.");
    }

    @Test
    public void testGetOwnerOfTimesheetIsNull() throws Exception {
        //preparations
        int timesheetID = 1;
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(timesheetServiceMock.getTimesheetByID(timesheetID)).thenReturn(null);
        when(permissionServiceMock.userCanViewTimesheet(Matchers.any(), Matchers.any())).thenReturn(true);

        //execution & verifying
        response = timesheetRestMock.getOwnerOfTimesheet(requestMock, timesheetID);
        assertEquals(response.getEntity(), "User Timesheet has not been initialized.");
    }

    @Test
    public void testGetOwnerOfTimesheetUserDoesNotExist() throws Exception {
        //preparations
        int timesheetID = 1;
        when(userManagerJiraMock.getUserByKey(any())).thenReturn(null);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(userMock);
        Response userDoesNotExist = Response.status(Response.Status.UNAUTHORIZED).entity("User does not exist.").build();
        PowerMockito.when(permissionServiceMock.checkUserPermission()).thenReturn(userDoesNotExist);
        when(permissionServiceMock.userCanViewTimesheet(Matchers.any(), Matchers.any())).thenReturn(true);

        //execution & verifying
        response = timesheetRest.getOwnerOfTimesheet(requestMock, timesheetID);
        assertEquals(response.getEntity(), "User does not exist.");
    }

    //TODO: correct this test
    @Test
    public void testGetTimesheetForUsernameOk() throws Exception {
        //preparations
        String userName = "test";
        String userKey = "USER_KEY";
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(timesheetServiceMock.getTimesheetByUser(userKey, false)).thenReturn(timesheetMock);

        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);

        //execution & verifying
        response = timesheetRestMock.getTimesheetForUsername(requestMock, userName, false);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testGetTimesheetOk() throws Exception {
        int timesheetID = 1;
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.getTimesheet(requestMock, timesheetID);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testGetTimesheetBuildEmail() throws Exception {
        int timesheetID = 1;
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(timesheetServiceMock.getTimesheetByID(timesheetID)).thenReturn(timesheetMock);
        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);

        when(timesheetMock.getTargetHours()).thenReturn(100);
        when(timesheetMock.getTargetHoursCompleted()).thenReturn(50);

        when(userMock.getEmailAddress()).thenReturn("test@test.at");
        when(ComponentAccessor.getMailQueue()).thenReturn(mailQueueMock);

        response = timesheetRestMock.getTimesheet(requestMock, timesheetID);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testGetTimesheetEntriesOk() throws Exception {
        int timesheetID = 1;
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.getTimesheetEntries(requestMock, timesheetID);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testGetTimesheetsOk() throws Exception {
        String userKey = "USER_KEY";
        String userName = "test";
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        List<Timesheet> timesheets = new LinkedList<Timesheet>();
        timesheets.add(timesheetMock);

        ApplicationUser user1 = mock(ApplicationUser.class);
        when(user1.getName()).thenReturn(userName);
        Set<ApplicationUser> usersSet = new HashSet<>(Arrays.asList(user1));

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        when(ComponentAccessor.getUserManager().getUserByName(userName).getKey()).thenReturn(userKey);

        when(timesheetServiceMock.all()).thenReturn(timesheets);
        when(timesheetMock.getIsMasterThesisTimesheet()).thenReturn(false);
        when(timesheetMock.getUserKey()).thenReturn(userKey);


        response = timesheetRestMock.getTimesheets(requestMock);
        assertNotNull(response.getEntity());
    }

    //TODO: mock the missing stuff - one of the biggest tests

    @Test
    public void testPostTimesheetEntryOk() throws Exception {
        int timesheetID = 1;
        Boolean isMTSheet = false;
        JsonTimesheetEntry jsonTimesheetEntry = new JsonTimesheetEntry(1,
                new Date(), new Date(), new Date(), new Date()
                , 0, "Description", 1, 1,
                "CAT-1530", "Partner", false, false);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.postTimesheetEntry(requestMock, jsonTimesheetEntry, timesheetID, isMTSheet);
        assertNotNull(response.getEntity());
    }

    //TODO: mock the missing stuff - one of the biggest tests

    @Test
    public void testPostTimesheetEntriesOk() throws Exception {
        int timesheetID = 1;
        Boolean isMTSheet = false;
        JsonTimesheetEntry jsonTimesheetEntry = new JsonTimesheetEntry(1,
                new Date(), new Date(), new Date(), new Date()
                , 0, "Description", 1, 1,
                "CAT-1530", "Partner", false, false);

        JsonTimesheetEntry[] jsonTimesheetEntries = {jsonTimesheetEntry};

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.postTimesheetEntries(requestMock, jsonTimesheetEntries, timesheetID, isMTSheet);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testPostTimesheetHoursOk() throws Exception {
        int timesheetID = 1;
        String userKey = "USER_KEY";
        Boolean isMTSheet = false;
        JsonTimesheet jsonTimesheet = new JsonTimesheet(1, "Mobile Computing", "", 5.0, new Date(), 50, 50, 150, 50,
                0, true, false, false, false, true, false);

        Date begin = new Date();
        Date end = new Date(begin.getTime());
        String desc = "Debugged this thingy...";
        int pause = 0;
        boolean isGoogleDocImport = false;
        String jiraTicketID = "CAT-1530";
        String pairProgrammingUserName = "TestUser";
        Date today = new Date();

        //Act
        TimesheetEntry newEntry = timesheetEntryServiceMock.add(timesheetMock, begin, end, categoryMock, desc, pause, teamMock, isGoogleDocImport,
                today, today, jiraTicketID, pairProgrammingUserName);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        when(timesheetMock.getUserKey()).thenReturn(userKey);

        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);
        when(permissionServiceMock.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(true);


        response = timesheetRest.postTimesheetHours(requestMock, jsonTimesheet, timesheetID, isMTSheet);
        //assertNotNull(response.getEntity());
    }

    @Test
    public void testPostTimesheetEnableStatesOk() throws Exception {
        JsonTimesheet jsonTimesheet = new JsonTimesheet(1, "Mobile Computing", "", 5.0, new Date(), 50, 50, 150, 50,
                0, true, false, false, false, true, false);

        JsonTimesheet[] jsonTimesheets = {jsonTimesheet};

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.postTimesheetEnableStates(requestMock, jsonTimesheets);
        assertNotNull(response.getEntity());
    }

    //TODO: mock the missing stuff - one of the biggest tests
    @Test
    public void testPutTimesheetEntryAdminChangedEntry() throws Exception {
        int timesheetID = 1;
        int entryID = 1;
        Boolean isMTSheet = false;
        String userKey = "USER_KEY";
        JsonTimesheetEntry jsonTimesheetEntry = new JsonTimesheetEntry(1,
                new Date(), new Date(), new Date(), new Date()
                , 0, "Description", 1, 1,
                "CAT-1530", "Partner", false, false);

        Date today = new Date();
        TimesheetEntry timesheetEntry = timesheetEntryServiceMock.add(timesheetMock, today, today, categoryMock,
                "Test Entry", 0, teamMock, false, today, today, "CAT-1530", "Partner");
        TimesheetEntry[] timesheetEntries = {timesheetEntry};


        Category[] categories = {categoryMock};
        Set<Team> teams = new LinkedHashSet<Team>();
        teams.add(teamMock);
        Collection<String> userGroups = new LinkedList<String>();
        userGroups.add(PermissionService.JIRA_ADMINISTRATORS);

        when(teamMock.getCategories()).thenReturn(categories);
        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(permissionServiceMock.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(true);
        when(groupManagerJiraMock.getGroupNamesForUser(
                ComponentAccessor.getUserManager().getUserByKey(userKey))).thenReturn(userGroups);
        when(timesheetEntryServiceMock.getEntryByID(entryID)).thenReturn(timesheetEntryMock);
        when(timesheetEntryMock.getTimeSheet()).thenReturn(timesheetMock);
        when(timesheetEntryMock.getPairProgrammingUserName()).thenReturn("");
        when(timesheetEntryMock.getTeam()).thenReturn(teamMock);
        when(timesheetEntryMock.getCategory()).thenReturn(categoryMock);
        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);
        when(timesheetMock.getIsEnabled()).thenReturn(true);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);
        when(teamServiceMock.getTeamByID(anyInt())).thenReturn(teamMock);
        when(categoryServiceMock.getCategoryByID(anyInt())).thenReturn(categoryMock);
        when(timesheetMock.getUserKey()).thenReturn(userKey);
        when(userMock.getEmailAddress()).thenReturn("test@test.at");
        when(ComponentAccessor.getMailQueue()).thenReturn(mailQueueMock);
        when(timesheetMock.getEntries()).thenReturn(timesheetEntries);
        when(timesheetEntryServiceMock.getEntriesBySheet(timesheetMock)).thenReturn(timesheetEntries);
        when(ComponentAccessor.getUserManager().getUserByKey(any()).getEmailAddress()).thenReturn("user@test.at");
        when(categoryMock.getName()).thenReturn("category 1");

        response = timesheetRestMock.putTimesheetEntry(requestMock, jsonTimesheetEntry, timesheetID, isMTSheet);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testDeleteTimesheetEntryOk() throws Exception {
        int entryID = 1;
        Boolean isMTSheet = false;

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.deleteTimesheetEntry(requestMock, entryID, isMTSheet);
        assertNotNull(response.getEntity());
    }
}
