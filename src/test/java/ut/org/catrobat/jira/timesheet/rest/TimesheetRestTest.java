package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserKeyService;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.mail.queue.MailQueue;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.helper.TimesheetPermissionCondition;
import org.catrobat.jira.timesheet.rest.TimesheetRest;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@PowerMockIgnore("jdk.internal.reflect.*")
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest({ComponentAccessor.class, TimesheetRest.class, TimesheetService.class,
        TimesheetEntryService.class})
public class TimesheetRestTest {

    private UserManager userManagerJiraMock;
    private GroupManager groupManagerJiraMock;

    private TimesheetEntryService timesheetEntryService;

    private TimesheetService timesheetServiceMock;
    private TeamService teamServiceMock;
    private CategoryService categoryServiceMock;
    private TimesheetEntryService timesheetEntryServiceMock;
    private PermissionService permissionServiceMock;
    private ConfigService configServiceMock;

    private Timesheet timesheetMock;
    private Category categoryMock;
    private Team teamMock;

    private TimesheetRest timesheetRest;
    private TimesheetRest timesheetRestMock;

    private javax.ws.rs.core.Response response;
    private HttpServletRequest requestMock;

    private MailQueue mailQueueMock;

    private EntityManager entityManager;
    private ApplicationUser userMock;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        TestActiveObjects ao = new TestActiveObjects(entityManager);

        userManagerJiraMock = mock(UserManager.class, RETURNS_DEEP_STUBS);
        groupManagerJiraMock = mock(GroupManager.class, RETURNS_DEEP_STUBS);
        UserKeyService userKeyServiceMock = mock(UserKeyService.class, RETURNS_DEEP_STUBS);
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
        userMock = mock(ApplicationUser.class, RETURNS_DEEP_STUBS);
        JiraAuthenticationContext jiraAuthMock = mock(JiraAuthenticationContext.class, RETURNS_DEEP_STUBS);
        TimesheetPermissionCondition permissionConditionMock = mock(TimesheetPermissionCondition.class, RETURNS_DEEP_STUBS);

        CategoryService categoryService = new CategoryServiceImpl(ao);
        TimesheetService timesheetService = new TimesheetServiceImpl(ao);
        TeamService teamService = new TeamServiceImpl(ao, categoryService, timesheetEntryService);
        ConfigService configService = new ConfigServiceImpl(ao, categoryService, teamService);
        timesheetEntryService = new TimesheetEntryServiceImpl(ao, timesheetService);

        timesheetRest = new TimesheetRest(timesheetEntryService, timesheetService, categoryService, teamService,
                permissionServiceMock, configService, permissionConditionMock);

        timesheetRestMock = new TimesheetRest(timesheetEntryServiceMock, timesheetServiceMock, categoryServiceMock,
                teamServiceMock, permissionServiceMock, configServiceMock, permissionConditionMock);

        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserKeyService()).thenReturn(userKeyServiceMock);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthMock);
        PowerMockito.when(permissionServiceMock.checkUserPermission()).thenReturn(null);
    }

    private Timesheet createTimesheetMock() {
        Timesheet sheet = mock(Timesheet.class);
        when(sheet.getID()).thenReturn(1);
        when(sheet.getLectures()).thenReturn("Mobile Computing");
        Date latestEntryDate = new Date();
        when(sheet.getLatestEntryBeginDate()).thenReturn(latestEntryDate);
        when(sheet.getTargetHours()).thenReturn(150);
        when(sheet.getHoursCompleted()).thenReturn(50);
        when(sheet.getHoursDeducted()).thenReturn(0);
        when(sheet.getState()).thenReturn(Timesheet.State.ACTIVE);
        return sheet;
    }

    private TimesheetEntry createTimesheetEntryMock() {
        Team team = mock(Team.class);
        when(team.getID()).thenReturn(1);
        Category category = mock(Category.class);
        when(category.getID()).thenReturn(1);

        TimesheetEntry entry = mock(TimesheetEntry.class);
        when(entry.getID()).thenReturn(1);
        when(entry.getBeginDate()).thenReturn(new Date());
        when(entry.getEndDate()).thenReturn(new Date());
        when(entry.getInactiveEndDate()).thenReturn(new Date());
        when(entry.getPauseMinutes()).thenReturn(0);
        when(entry.getDescription()).thenReturn("Description");
        when(entry.getTeam()).thenReturn(team);
        when(entry.getCategory()).thenReturn(category);
        when(entry.getJiraTicketID()).thenReturn("CAT-1530");
        when(entry.getPairProgrammingUserName()).thenReturn("Partner");
        when(entry.getIsGoogleDocImport()).thenReturn(false);
        return entry;
    }

    @Test
    public void testGetTeamsForTimesheetOk() throws Exception {
        Timesheet timesheet = createTimesheetMock();

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

        Set<Team> teams = new HashSet<>();
        teams.add(team1);
        teams.add(team2);

        ApplicationUser user1 = mock(ApplicationUser.class);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);

        when(timesheetServiceMock.getTimesheetByID(timesheetID)).thenReturn(timesheet);
        when(permissionServiceMock.userCanViewTimesheet(user1, timesheet)).thenReturn(true);
        when(permissionServiceMock.checkIfUserExists()).thenReturn(user1);

        response = timesheetRestMock.getTeamsForTimesheetID(requestMock, 1);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        assertNotNull(responseTeamList);
    }

    @Test
    public void testGetTeamsForTimesheetReturnNoTeams() throws Exception {
        int timesheetID = 1;
        String username = "testUser";
        String userKey = "USER_KEY_1";

        List<JsonTeam> expectedTeams = new LinkedList<>();
        Set<Team> teams = new HashSet<>();

        when(teamServiceMock.getTeamsOfUser(username)).thenReturn(teams);

        ApplicationUser user1 = mock(ApplicationUser.class);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);
        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);
        when(timesheetServiceMock.getTimesheetByID(timesheetID)).thenReturn(timesheetMock);
        when(teamServiceMock.getTeamsOfUser(userMock.getName())).thenReturn(teams);

        response = timesheetRestMock.getTeamsForTimesheetID(requestMock, timesheetID);

        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        assertEquals(responseTeamList, expectedTeams);
    }

    @Test
    public void testGetTeamsForTimesheetUserDoesNotExist() throws Exception {
        //preparations
        int timesheetID = 1;
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);

        //execution & verifying
        response = timesheetRestMock.getTeamsForTimesheetID(requestMock, timesheetID);
        assertEquals(response.getEntity(), "You are not allowed to see the timesheet.");
    }

    @Test
    public void testGetTeamsForUserOk() throws Exception {
        Timesheet timesheet = createTimesheetMock();
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
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        when(timesheetServiceMock.getTimesheetByID(1)).thenReturn(timesheet);
        when(permissionServiceMock.userCanViewTimesheet(user1, timesheet)).thenReturn(true);
        when(permissionServiceMock.checkIfUserExists()).thenReturn(user1);

        response = timesheetRestMock.getTeamsForTimesheetID(requestMock, 1);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        assertNotNull(responseTeamList);
    }

    @Test
    public void testGetTeamsForUserReturnNoTeams() throws Exception {
        Timesheet timesheet = createTimesheetMock();
        String username = "testUser";
        String userKey = "USER_KEY_1";

        List<JsonTeam> expectedTeams = new LinkedList<>();
        Set<Team> teams = new HashSet<>();

        when(teamServiceMock.getTeamsOfUser(username)).thenReturn(teams);

        ApplicationUser user1 = mock(ApplicationUser.class);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);

        PowerMockito.when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(user1);
        when(timesheetServiceMock.getTimesheetByID(1)).thenReturn(timesheet);
        when(permissionServiceMock.userCanViewTimesheet(user1, timesheet)).thenReturn(true);
        when(teamServiceMock.getTeamsOfUser(user1.getKey())).thenReturn(teams);

        response = timesheetRestMock.getTeamsForTimesheetID(requestMock, 1);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        assertEquals(responseTeamList, expectedTeams);
    }

    @Test
    public void testGetTeamsUserDoesNotExist() throws Exception {
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);
        PowerMockito.when(permissionServiceMock.checkIfUserExists()).thenThrow(new PermissionException("User does not exist."));
        //execution & verifying
        response = timesheetRest.getTeamsForTimesheetID(requestMock, 0);

        assertEquals(response.getEntity(), "User does not exist.");
    }

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

        Set<Team> teams = new HashSet<>();
        teams.add(team1);
        teams.add(team2);

        ApplicationUser user1 = mock(ApplicationUser.class);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);

        when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        List<String> developerList = new LinkedList<>();
        developerList.add(username);
        developerList.add("asdf");
        developerList.add("jkl");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);
        when(teamServiceMock.getGroupsForRole(team1.getTeamName(), TeamToGroup.Role.DEVELOPER)).thenReturn(developerList);

        Date today = new Date();

        TimesheetEntry timesheetEntry = timesheetEntryServiceMock.add(timesheetMock, today, today, categoryMock,
                "Test Entry", 0, team1, false, today, "CAT-1530", "Partner", true);
        TimesheetEntry[] timesheetEntries = {timesheetEntry};

        when(timesheetServiceMock.getTimesheetByUser(userKey)).thenReturn(timesheetMock);
        when(timesheetMock.getEntries()).thenReturn(timesheetEntries);
        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);
        when(timesheetServiceMock.getTimesheetByID(timesheetID)).thenReturn(timesheetMock);

        //execution & verifying
        response = timesheetRestMock.getTimesheetEntriesOfAllTeamMembers(requestMock, timesheetID);

        List<JsonTimesheetEntry> responseTimesheetEntries = (List<JsonTimesheetEntry>) response.getEntity();
        assertNotNull(responseTimesheetEntries);
    }

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

        Set<Team> teams = new HashSet<>();
        teams.add(team1);
        teams.add(team2);

        ApplicationUser user1 = mock(ApplicationUser.class);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(username);
        when(timesheetServiceMock.getTimesheetByID(timesheetID).getUserKey()).thenReturn(userKey);

        when(ComponentAccessor.getUserManager().getUserByName(username).getKey()).thenReturn(userKey);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);

        List<String> developerList = new LinkedList<>();
        developerList.add("asdf");
        developerList.add("jkl");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);
        when(teamServiceMock.getGroupsForRole(team1.getTeamName(), TeamToGroup.Role.DEVELOPER)).thenReturn(developerList);
        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);
        when(timesheetServiceMock.getTimesheetByID(timesheetID)).thenReturn(timesheetMock);

        //execution & verifying
        response = timesheetRestMock.getTimesheetEntriesOfAllTeamMembers(requestMock, timesheetID);

        List<JsonTimesheetEntry> expectedList = new LinkedList<>();
        List<JsonTimesheetEntry> responseTimesheetEntries = (List<JsonTimesheetEntry>) response.getEntity();
        assertEquals(responseTimesheetEntries, expectedList);
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

        List<String> developerList = new LinkedList<>();
        developerList.add(userName);
        Date today = new Date();

        TimesheetEntry timesheetEntry = timesheetEntryServiceMock.add(timesheetMock, today, today, categoryMock,
                "Test Entry", 0, team1, false, today, "CAT-1530", "Partner", true);
        TimesheetEntry[] timesheetEntries = {timesheetEntry};

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);
        when(teamServiceMock.getGroupsForRole(teamName, TeamToGroup.Role.DEVELOPER)).thenReturn(developerList);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);
        when(ComponentAccessor.getUserKeyService().getKeyForUsername(userName)).thenReturn(userKey);

        when(timesheetServiceMock.getTimesheetByUser(userKey)).thenReturn(timesheetMock);
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

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRestMock.getTimesheetIDOFUser(requestMock, userName);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testGetOwnerOfTimesheetOk() throws Exception {
        //preparations
        int timesheetID = 1;
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
        when(timesheetServiceMock.getTimesheetByUser(userKey)).thenReturn(timesheetMock);

        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);

        //execution & verifying
        response = timesheetRestMock.getTimesheetForUsername(requestMock, userName);
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
        when(timesheetMock.getHoursCompleted()).thenReturn(50);

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

        List<Timesheet> timesheets = new LinkedList<>();
        timesheets.add(timesheetMock);

        ApplicationUser user1 = mock(ApplicationUser.class);
        when(user1.getName()).thenReturn(userName);

        //user1 should be the testUser
        when(user1.getName()).thenReturn(userName);

        when(ComponentAccessor.getUserManager().getUserByName(userName).getKey()).thenReturn(userKey);

        when(timesheetServiceMock.all()).thenReturn(timesheets);
        when(timesheetMock.getUserKey()).thenReturn(userKey);


        response = timesheetRestMock.getTimesheets(requestMock);
        assertNotNull(response.getEntity());
    }

    //TODO: mock the missing stuff - one of the biggest tests
    @Test
    public void testPostTimesheetEntryOk() throws Exception {
        int timesheetID = 1;
        TimesheetEntry timesheetEntryMock = createTimesheetEntryMock();
        JsonTimesheetEntry jsonTimesheetEntry = new JsonTimesheetEntry(timesheetEntryMock);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.postTimesheetEntry(requestMock, jsonTimesheetEntry, timesheetID);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testPostTimesheetEntriesOk() throws Exception {
        int timesheetID = 1;


        TimesheetEntry timesheetEntryMock = createTimesheetEntryMock();
        JsonTimesheetEntry jsonTimesheetEntry = new JsonTimesheetEntry(timesheetEntryMock);

        JsonTimesheetEntry[] jsonTimesheetEntries = {jsonTimesheetEntry};

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.postTimesheetEntries(requestMock, jsonTimesheetEntries, timesheetID);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testPostTimesheetHoursOk() throws Exception {
        int timesheetID = 1;
        String userKey = "USER_KEY";

        Timesheet sheet = createTimesheetMock();
        JsonTimesheet jsonTimesheet = new JsonTimesheet(sheet);

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        when(timesheetMock.getUserKey()).thenReturn(userKey);

        when(permissionServiceMock.userCanViewTimesheet(userMock, timesheetMock)).thenReturn(true);
        when(permissionServiceMock.checkIfUserIsGroupMember(PermissionService.JIRA_ADMINISTRATORS)).thenReturn(true);

        response = timesheetRest.postTimesheetHours(requestMock, jsonTimesheet, timesheetID);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testPostTimesheetEnableStatesOk() throws Exception {
        Timesheet sheet = createTimesheetMock();
        JsonTimesheet jsonTimesheet = new JsonTimesheet(sheet);

        JsonTimesheet[] jsonTimesheets = {jsonTimesheet};

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.postTimesheetEnableStates(requestMock, jsonTimesheets);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testPutTimesheetEntryAdminChangedEntry() throws Exception {
        int timesheetID = 1;
        int entryID = 1;
        String userKey = "USER_KEY";

        TimesheetEntry timesheetEntryMock = createTimesheetEntryMock();
        JsonTimesheetEntry jsonTimesheetEntry = new JsonTimesheetEntry(timesheetEntryMock);

        Date today = new Date();
        TimesheetEntry timesheetEntry = timesheetEntryServiceMock.add(timesheetMock, today, today, categoryMock,
                "Test Entry", 0, teamMock, false, today, "CAT-1530", "Partner", false);
        TimesheetEntry[] timesheetEntries = {timesheetEntry};


        Category[] categories = {categoryMock};
        Set<Team> teams = new LinkedHashSet<>();
        teams.add(teamMock);
        Collection<String> userGroups = new LinkedList<>();
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
        when(teamServiceMock.getTeamsOfUser(anyString())).thenReturn(teams);
        when(teamServiceMock.getTeamByID(anyInt())).thenReturn(teamMock);
        when(categoryServiceMock.getCategoryByID(anyInt())).thenReturn(categoryMock);
        when(timesheetMock.getUserKey()).thenReturn(userKey);
        when(userMock.getEmailAddress()).thenReturn("test@test.at");
        when(ComponentAccessor.getMailQueue()).thenReturn(mailQueueMock);
        when(timesheetMock.getEntries()).thenReturn(timesheetEntries);
        when(ComponentAccessor.getUserManager().getUserByKey(any()).getEmailAddress()).thenReturn("user@test.at");
        when(categoryMock.getName()).thenReturn("category 1");

        response = timesheetRestMock.putTimesheetEntry(requestMock, jsonTimesheetEntry, timesheetID);
        assertNotNull(response.getEntity());
    }

    @Test
    public void testDeleteTimesheetEntryNotFound() throws Exception {
        int entryID = 1;

        when(permissionServiceMock.checkIfUserExists()).thenReturn(userMock);

        response = timesheetRest.deleteTimesheetEntry(requestMock, entryID);
        assertNotNull(response.getEntity());
    }
}
