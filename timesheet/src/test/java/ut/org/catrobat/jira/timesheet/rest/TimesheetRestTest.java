package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.UserKeyService;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.sal.api.user.UserProfile;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.rest.TimesheetRest;
import org.catrobat.jira.timesheet.rest.json.JsonCategory;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
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
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest({ComponentAccessor.class, TimesheetRest.class, TimesheetService.class,
        TimesheetEntryService.class})
public class TimesheetRestTest {

    private ComponentAccessor componentAccessorMock;

    private com.atlassian.sal.api.user.UserManager userManagerLDAPMock;
    private com.atlassian.jira.user.util.UserManager userManagerJiraMock;
    private UserKeyService userKeyServiceMock;

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
    private UserProfile userProfile;
    private Timesheet timesheet;
    private Category category;
    private TimesheetEntry timesheetEntry;

    private TimesheetRest timesheetRest;
    private TimesheetRest timesheetRestMock;
    private TimesheetRest spyTimesheetRest;

    private Response response;
    private HttpServletRequest request;

    private SimpleDateFormat sdf;

    private TestActiveObjects ao;
    private EntityManager entityManager;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        componentAccessorMock = Mockito.mock(ComponentAccessor.class, RETURNS_DEEP_STUBS);
        userManagerLDAPMock = Mockito.mock(com.atlassian.sal.api.user.UserManager.class, RETURNS_DEEP_STUBS);
        userManagerJiraMock = Mockito.mock(com.atlassian.jira.user.util.UserManager.class, RETURNS_DEEP_STUBS);
        userKeyServiceMock = Mockito.mock(UserKeyService.class, RETURNS_DEEP_STUBS);
        userUtilMock = Mockito.mock(UserUtil.class, RETURNS_DEEP_STUBS);
        configServiceMock = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        categoryServiceMock = mock(CategoryService.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        timesheetEntryServiceMock = mock(TimesheetEntryService.class, RETURNS_DEEP_STUBS);
        timesheetServiceMock = mock(TimesheetService.class, RETURNS_DEEP_STUBS);
        teamServiceMock = mock(TeamService.class, RETURNS_DEEP_STUBS);
        request = Mockito.mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);

        categoryService = new CategoryServiceImpl(ao);
        configService = new ConfigServiceImpl(ao, categoryService);
        teamService = new TeamServiceImpl(ao, configService);
        permissionService = new PermissionServiceImpl(userManagerLDAPMock, teamService, configService);
        timesheetEntryService = new TimesheetEntryServiceImpl(ao);
        timesheetService = new TimesheetServiceImpl(ao);

        userProfile = Mockito.mock(UserProfile.class);
        timesheet = Mockito.mock(Timesheet.class);
        category = Mockito.mock(Category.class);
        timesheetEntry = Mockito.mock(TimesheetEntry.class);

        timesheetRest = new TimesheetRest(timesheetEntryService, timesheetService, categoryService, userManagerLDAPMock, teamService, permissionService, configService);

        timesheetRestMock = new TimesheetRest(timesheetEntryServiceMock, timesheetServiceMock, categoryServiceMock, userManagerLDAPMock, teamServiceMock, permissionServiceMock, configServiceMock);

        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);
        PowerMockito.when(ComponentAccessor.getUserKeyService()).thenReturn(userKeyServiceMock);
    }

    @Test
    public void testGetTeamsForTimesheetOk() throws Exception {
        int timesheetID = 1;
        String userName = "test";
        String userKey = "USER_KEY_1";

        Category[] categories = {category};

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

        User user1 = mock(User.class);
        User user2 = mock(User.class);

        Set<User> usersSet = new HashSet<User>(Arrays.asList(user1, user2));
        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);

        response = timesheetRestMock.getTeamsForTimesheet(request, timesheetID);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        Assert.assertNotNull(responseTeamList);
    }

    @Test
    public void testGetTeamsForTimesheetReturnNoTeams() throws Exception {
        int timesheetID = 1;
        String username = "testUser";
        String userKey = "USER_KEY_1";

        List<JsonTeam> expectedTeams = new LinkedList<JsonTeam>();
        Set<Team> teams = new HashSet<Team>();

        when(teamServiceMock.getTeamsOfUser(username)).thenReturn(teams);

        User user1 = mock(User.class);
        User user2 = mock(User.class);

        Set<User> usersSet = new HashSet<User>(Arrays.asList(user1, user2));

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);
        PowerMockito.when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        response = timesheetRest.getTeamsForTimesheet(request, timesheetID);

        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        Assert.assertEquals(responseTeamList, expectedTeams);
    }

    @Test
    public void testGetTeamsForTimesheetUserDoesNotExist() throws Exception {
        //preparations
        int timesheetID = 1;
        when(userManagerLDAPMock.getUserProfile(userProfile.getUsername())).thenReturn(null);

        //execution & verifying
        response = timesheetRest.getTeamsForTimesheet(request, timesheetID);
        Assert.assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testGetTeamsForUserOk() throws Exception {
        Category[] categories = {category};

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

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        response = timesheetRestMock.getTeamsForUser(request);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        Assert.assertNotNull(responseTeamList);
    }

    @Test
    public void testGetTeamsForUserReturnNoTeams() throws Exception {
        String username = "testUser";
        String userKey = "USER_KEY_1";

        List<JsonTeam> expectedTeams = new LinkedList<JsonTeam>();
        Set<Team> teams = new HashSet<Team>();

        when(teamServiceMock.getTeamsOfUser(username)).thenReturn(teams);

        User user1 = mock(User.class);
        User user2 = mock(User.class);

        Set<User> usersSet = new HashSet<User>(Arrays.asList(user1, user2));

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);

        PowerMockito.when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        response = timesheetRest.getTeamsForUser(request);

        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        Assert.assertEquals(responseTeamList, expectedTeams);
    }

    @Test
    public void testGetTeamsUserDoesNotExist() throws Exception {
        when(userManagerLDAPMock.getUserProfile(userProfile.getUsername())).thenReturn(null);
        //execution & verifying
        response = timesheetRest.getTeamsForUser(request);

        Assert.assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testGetCategoriesOk() throws Exception {
        response = timesheetRest.getCategories(request);
        List<JsonCategory> responseTeamList = (List<JsonCategory>) response.getEntity();
        Assert.assertNotNull(responseTeamList);
    }

    @Test
    public void testGetCategoriesUserDoesNotExist() throws Exception {
        //preparations
        when(userManagerLDAPMock.getUserProfile(userProfile.getUsername())).thenReturn(null);
        //execution & verifying
        response = timesheetRest.getCategories(request);
        Assert.assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testGetAllTeamsOk() throws Exception {

        Category[] categories = {category};
        int[] categoryIDs = {1};

        List<JsonTeam> expectedTeams = new LinkedList<JsonTeam>();
        expectedTeams.add(new JsonTeam(1, "Catroid", categoryIDs));
        expectedTeams.add(new JsonTeam(2, "IRC", new int[0]));

        Team team1 = Mockito.mock(Team.class);
        Mockito.when(team1.getID()).thenReturn(1);
        Mockito.when(team1.getTeamName()).thenReturn("Catroid");
        Mockito.when(team1.getCategories()).thenReturn(categories);

        Team team2 = Mockito.mock(Team.class);
        Mockito.when(team2.getID()).thenReturn(2);
        Mockito.when(team2.getTeamName()).thenReturn("IRC");
        Mockito.when(team2.getCategories()).thenReturn(new Category[0]);

        List<Team> teams = new LinkedList<Team>();
        teams.add(team1);
        teams.add(team2);

        Mockito.when(teamServiceMock.all()).thenReturn(teams);

        response = timesheetRest.getAllTeams(request);

        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        Assert.assertNotNull(responseTeamList);
    }

    @Test
    public void testGetAllTeamsReturnNoTeams() throws Exception {
        List<Team> teams = new LinkedList<Team>();

        Mockito.when(teamServiceMock.all()).thenReturn(teams);

        response = timesheetRest.getAllTeams(request);

        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        Assert.assertNotNull(responseTeamList);
    }

    @Test
    public void testGetAllTeamsUserDoesNotExist() throws Exception {
        //preparations
        when(userManagerLDAPMock.getUserProfile(userProfile.getUsername())).thenReturn(null);

        //execution & verifying
        response = timesheetRest.getAllTeams(request);
        Assert.assertEquals(response.getEntity(), "User does not exist.");
    }

    @Test
    public void testGetTimesheetEntriesOfAllTeamMembersOk() throws Exception {
        //preparations
        int timesheetID = 1;
        String username = "testUser";
        String userKey = "USER_KEY_1";

        Category[] categories = {category};

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

        User user1 = mock(User.class);
        User user2 = mock(User.class);

        Set<User> usersSet = new HashSet<User>(Arrays.asList(user1, user2));

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);

        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        List<String> developerList = new LinkedList<String>();
        developerList.add(username);
        developerList.add("asdf");
        developerList.add("jklö");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);
        when(configServiceMock.getGroupsForRole(team1.getTeamName(), TeamToGroup.Role.DEVELOPER)).thenReturn(developerList);

        TimesheetEntry timesheetEntry = timesheetEntryServiceMock.add(timesheet, new Date(), new Date(), category,
                "Test Entry", 0, team1, false, new Date(), "CAT-1530", "Partner");
        TimesheetEntry[] timesheetEntries = {timesheetEntry};

        when(timesheetServiceMock.getTimesheetByUser(userKey, false)).thenReturn(timesheet);
        when(timesheetEntryServiceMock.getEntriesBySheet(timesheet)).thenReturn(timesheetEntries);

        //execution & verifying
        response = timesheetRestMock.getTimesheetEntriesOfAllTeamMembers(request, timesheetID);

        List<JsonTimesheetEntry> responseTimesheetEntries = (List<JsonTimesheetEntry>) response.getEntity();
        Assert.assertNotNull(responseTimesheetEntries);
    }

    @Test
    public void testGetTimesheetEntriesOfAllTeamMembersWrongUser() throws Exception {
        //preparations
        int timesheetID = 1;
        String username = "testUser";
        String userKey = "USER_KEY_1";

        Category[] categories = {category};

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

        User user1 = mock(User.class);
        User user2 = mock(User.class);

        Set<User> usersSet = new HashSet<User>(Arrays.asList(user1, user2));

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);

        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(usersSet);
        when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        List<String> developerList = new LinkedList<String>();
        developerList.add("asdf");
        developerList.add("jklö");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);
        when(configServiceMock.getGroupsForRole(team1.getTeamName(), TeamToGroup.Role.DEVELOPER)).thenReturn(developerList);

        //execution & verifying
        response = timesheetRestMock.getTimesheetEntriesOfAllTeamMembers(request, timesheetID);

        List<JsonTimesheetEntry> expectedList = new LinkedList<JsonTimesheetEntry>();
        List<JsonTimesheetEntry> responseTimesheetEntries = (List<JsonTimesheetEntry>) response.getEntity();
        Assert.assertEquals(responseTimesheetEntries, expectedList);
    }

    @Test
    public void testGetTimesheetEntriesOfAllTeamMembersUserDoesNotExist() throws Exception {
        //preparations
        when(userManagerLDAPMock.getUserProfile(userProfile.getUsername())).thenReturn(null);
        //execution & verifying
        response = timesheetRest.getTimesheetEntriesOfAllTeamMembers(request, 1);
        Assert.assertEquals(response.getEntity(), "User does not exist.");
    }

    /*
    @Test
    public void testGetAllTimesheetEntriesForTeamOk() throws Exception {
        //preparations
        String teamName = "Catroid";
        String userName = "testUser";
        String userKey = "USER_KEY_1";

        Category[] categories = {category};

        Team team1 = Mockito.mock(Team.class);
        when(team1.getID()).thenReturn(1);
        when(team1.getTeamName()).thenReturn("Catroid");
        when(team1.getCategories()).thenReturn(categories);

        User user1 = mock(User.class);

        when(permissionServiceMock.checkIfUserExists(request)).thenReturn(userProfile);

        List<String> developerList = new LinkedList<String>();
        developerList.add(userName);
        developerList.add("asdf");
        developerList.add("jklö");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);
        when(configServiceMock.getGroupsForRole(team1.getTeamName(), TeamToGroup.Role.DEVELOPER)).thenReturn(developerList);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);
        when(ComponentAccessor.getUserKeyService().getKeyForUsername(userName)).thenReturn(userKey);

        TimesheetEntry timesheetEntry = timesheetEntryServiceMock.add(timesheet, new Date(), new Date(), category,
                "Test Entry", 0, team1, false, new Date(), "CAT-1530", "Partner");
        TimesheetEntry[] timesheetEntries = {timesheetEntry};

        when(timesheetServiceMock.getTimesheetByUser(userKey, false).getEntries()).thenReturn(timesheetEntries);

        //when(timesheetEntry.getTeam()).thenReturn(team1);
        //execution & verifying
        response = timesheetRestMock.getAllTimesheetEntriesForTeam(request, teamName);

        //List<JsonTimesheetEntry> responseTimesheetEntries = (List<JsonTimesheetEntry>) response.getEntity();
        //Assert.assertNotNull(responseTimesheetEntries);
    }*/

    //TODO: complete these tests

    @Test
    public void testGetAllTimesheetEntriesForTemOk() throws Exception {
        String teamName = "Catroid";
        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.getAllTimesheetEntriesForTeam(request, teamName);
        Assert.assertNotNull(response.getEntity());
    }

    @Test
    public void testGetTimesheetIDOfUserOk() throws Exception {
        String userName = "test";
        Boolean isMTSheet = false;
        when(permissionServiceMock.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRestMock.getTimesheetIDOFUser(request, userName, isMTSheet);
        Assert.assertNotNull(response.getEntity());
    }

    @Test
    public void testGetOwnerOfTimesheetOk() throws Exception {
        String userName = "test";
        int timesheetID = 1;
        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.getOwnerOfTimesheet(request, timesheetID);
        Assert.assertNotNull(response.getEntity());
    }

    @Test
    public void testGetTimesheetForUsernameOk() throws Exception {
        String userName = "test";
        Boolean isMTSheet = false;
        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.getTimesheetForUsername(request, userName, isMTSheet);
        Assert.assertNotNull(response.getEntity());
    }

    //TODO: approvedUserList mocken -> testing of private methods for emails

    @Test
    public void testGetTimesheetOk() throws Exception {
        int timesheetID = 1;
        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);
        when(permissionServiceMock.userCanViewTimesheet(userProfile, timesheet)).thenReturn(true);

        when(timesheet.getTargetHours()).thenReturn(100);
        when(timesheet.getTargetHoursCompleted()).thenReturn(50);

        response = timesheetRestMock.getTimesheet(request, timesheetID);
        Assert.assertNotNull(response.getEntity());
    }

    @Test
    public void testGetTimesheetEntriesOk() throws Exception {
        int timesheetID = 1;
        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.getTimesheetEntries(request, timesheetID);
        Assert.assertNotNull(response.getEntity());
    }

    //TODO: mock the rest

    @Test
    public void testGetTimesheetsOk() throws Exception {
        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.getTimesheets(request);
        Assert.assertNotNull(response.getEntity());
    }

    //TODO: mock the missing stuff - one of the biggest tests

    @Test
    public void testPostTimesheetEntryOk() throws Exception {
        int timesheetID = 1;
        Boolean isMTSheet = false;
        JsonTimesheetEntry jsonTimesheetEntry = new JsonTimesheetEntry(1,
                new Date(), new Date(), new Date(),
                0, "Description", 1, 1,
                "CAT-1530", "Partner", false);

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.postTimesheetEntry(request, jsonTimesheetEntry, timesheetID, isMTSheet);
        Assert.assertNotNull(response.getEntity());
    }

    //TODO: mock the missing stuff - one of the biggest tests

    @Test
    public void testPostTimesheetEntriesOk() throws Exception {
        int timesheetID = 1;
        Boolean isMTSheet = false;
        JsonTimesheetEntry jsonTimesheetEntry = new JsonTimesheetEntry(1,
                new Date(), new Date(), new Date(),
                0, "Description", 1, 1,
                "CAT-1530", "Partner", false);

        JsonTimesheetEntry[] jsonTimesheetEntries = {jsonTimesheetEntry};

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.postTimesheetEntries(request, jsonTimesheetEntries, timesheetID, isMTSheet);
        Assert.assertNotNull(response.getEntity());
    }

    @Test
    public void testPostTimesheetHoursOk() throws Exception {
        int timesheetID = 1;
        String userKey = "USER_KEY";
        Boolean isMTSheet = false;
        JsonTimesheet jsonTimesheet = new JsonTimesheet(1, "Mobile Computing", "", 5.0, "2016-04-09", 50, 50, 150, 50,
                0, true, true, false);

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        when(timesheet.getUserKey()).thenReturn(userKey);
        when(ComponentAccessor.getUserKeyService().getKeyForUsername(userProfile.getUsername())).thenReturn(userKey);

        when(permissionServiceMock.userCanViewTimesheet(userProfile, timesheet)).thenReturn(true);
        when(permissionServiceMock.checkIfUserIsGroupMember(request, "jira-administrators")).thenReturn(true);

        //when(ComponentAccessor.getUserKeyService().getKeyForUsername(userProfile.getUsername())).thenReturn()

        response = timesheetRest.postTimesheetHours(request, jsonTimesheet, timesheetID, isMTSheet);
        //Assert.assertNotNull(response.getEntity());
    }

    @Test
    public void testPostTimesheetEnableStatesOk() throws Exception {
        JsonTimesheet jsonTimesheet = new JsonTimesheet(1, "Mobile Computing", "", 5.0, "2016-04-09", 50, 50, 150, 50,
                0, true, true, false);

        JsonTimesheet[] jsonTimesheets = {jsonTimesheet};

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.postTimesheetEnableStates(request, jsonTimesheets);
        Assert.assertNotNull(response.getEntity());
    }

    //TODO: mock the missing stuff - one of the biggest tests

    @Test
    public void testPutTimesheetEntryOk() throws Exception {
        int timesheetID = 1;
        Boolean isMTSheet = false;
        JsonTimesheetEntry jsonTimesheetEntry = new JsonTimesheetEntry(1,
                new Date(), new Date(), new Date(),
                0, "Description", 1, 1,
                "CAT-1530", "Partner", false);

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.putTimesheetEntry(request, jsonTimesheetEntry, timesheetID, isMTSheet);
        Assert.assertNotNull(response.getEntity());
    }

    @Test
    public void testDeleteTimesheetEntryOk() throws Exception {
        int entryID = 1;
        Boolean isMTSheet = false;

        when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);

        response = timesheetRest.deleteTimesheetEntry(request, entryID, isMTSheet);
        Assert.assertNotNull(response.getEntity());
    }
}