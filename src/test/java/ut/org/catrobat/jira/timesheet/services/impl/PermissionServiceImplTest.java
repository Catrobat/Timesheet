package ut.org.catrobat.jira.timesheet.services.impl;


import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheetEntry;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.services.impl.PermissionServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(PermissionServiceImplTest.MyDatabaseUpdater.class)
@PrepareForTest(ComponentAccessor.class)
public class PermissionServiceImplTest {

    private static Team catroid, html5, drone;
    private static TimesheetAdmin timesheetAdmin;
    private static TSAdminGroup timesheetAdminGroup;
    private static Config config;
    @Rule
    public org.mockito.junit.MockitoRule mockitoRule = MockitoJUnit.rule();
    private PermissionServiceImpl permissionService, permissionServiceException;
    private TeamService teamService;
    private ApplicationUser coord, owner, eve, test, admin;
    private Timesheet sheet;
    private TimesheetEntry timeSheetEntry;
    private HttpServletRequest request;
    private Team team;
    private TimesheetService sheetService;
    private TimesheetEntryService entryService;
    private SimpleDateFormat sdf;
    private ConfigService configService;
    private GroupManager groupManager;
    private EntityManager entityManager;
    private UserManager jiraUserManager;
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ComponentAccessor.class);


        teamService = Mockito.mock(TeamService.class);
        config = Mockito.mock(Config.class);
        configService = Mockito.mock(ConfigService.class);
        groupManager = Mockito.mock(GroupManager.class);
        jiraUserManager = Mockito.mock(UserManager.class);
        jiraAuthenticationContext = Mockito.mock(JiraAuthenticationContext.class);

        assertNotNull(entityManager);

        permissionService = new PermissionServiceImpl(teamService, configService);

        //arrange
        coord = Mockito.mock(ApplicationUser.class);
        owner = Mockito.mock(ApplicationUser.class);
        eve = Mockito.mock(ApplicationUser.class);
        test = Mockito.mock(ApplicationUser.class);
        admin = Mockito.mock(ApplicationUser.class);
        sheet = Mockito.mock(Timesheet.class);
        sheetService = Mockito.mock(TimesheetService.class);
        entryService = Mockito.mock(TimesheetEntryService.class);
        timeSheetEntry = Mockito.mock(TimesheetEntry.class);
        team = Mockito.mock(Team.class);
        request = Mockito.mock(HttpServletRequest.class);
        permissionServiceException = Mockito.mock(PermissionServiceImpl.class);
        config = Mockito.mock(Config.class);

        String coord_key = "coord_key";
        String owner_key = "owner_key";
        String eve_key = "eve_key";
        String test_key = "test_key";
        String admin_key = "admin_key";

        Mockito.when(sheet.getUserKey()).thenReturn("owner_key");

        Mockito.when(coord.getKey()).thenReturn(coord_key);
        Mockito.when(owner.getKey()).thenReturn(owner_key);
        Mockito.when(eve.getKey()).thenReturn(eve_key);
        Mockito.when(test.getKey()).thenReturn(test_key);
        Mockito.when(admin.getKey()).thenReturn(admin_key);

        Mockito.when(coord.getUsername()).thenReturn("coord");
        Mockito.when(owner.getUsername()).thenReturn("owner");
        Mockito.when(eve.getUsername()).thenReturn("eve");
        Mockito.when(test.getUsername()).thenReturn("test");
        Mockito.when(admin.getUsername()).thenReturn("admin");

        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(jiraUserManager);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(ComponentAccessor.getGroupManager()).thenReturn(groupManager);

        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(owner, "jira-administrators")).thenReturn(false);
        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(admin, "jira-administrators")).thenReturn(true);
        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(eve, "jira-administrators")).thenReturn(false);
        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(test, "jira-administrators")).thenReturn(false);
        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(coord, "jira-administrators")).thenReturn(false);

        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(owner_key)).thenReturn(owner);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(admin_key)).thenReturn(admin);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(eve_key)).thenReturn(eve);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(test_key)).thenReturn(test);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(coord_key)).thenReturn(coord);

        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(owner.getUsername())).thenReturn(owner);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(admin.getUsername())).thenReturn(admin);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(eve.getUsername())).thenReturn(eve);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(test.getUsername())).thenReturn(test);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(coord.getUsername())).thenReturn(coord);

        Set<Team> owner_teams = new HashSet<Team>();
        Set<Team> eve_teams = new HashSet<Team>();
        Set<Team> coord_cteams = new HashSet<Team>();
        Set<Team> no_teams = new HashSet<Team>();

        owner_teams.add(html5);
        owner_teams.add(drone);
        eve_teams.add(catroid);
        coord_cteams.add(html5);

        Mockito.when(teamService.getTeamsOfUser("owner")).thenReturn(owner_teams);
        Mockito.when(teamService.getTeamsOfUser("eve")).thenReturn(eve_teams);
        Mockito.when(teamService.getCoordinatorTeamsOfUser("coord")).thenReturn(coord_cteams);

        Mockito.when(teamService.getTeamsOfUser("coord")).thenReturn(coord_cteams);
        Mockito.when(teamService.getTeamsOfUser("admin")).thenReturn(no_teams);
        Mockito.when(teamService.getCoordinatorTeamsOfUser("owner")).thenReturn(owner_teams);
        Mockito.when(teamService.getCoordinatorTeamsOfUser("eve")).thenReturn(eve_teams);
        Mockito.when(teamService.getCoordinatorTeamsOfUser("admin")).thenReturn(no_teams);
    }

    @Test
    public void testOwnerCanViewTimesheet() throws Exception {
        assertTrue(permissionService.userCanViewTimesheet(owner, sheet));
    }

    @Test
    public void testCoordinatorCanViewTimesheet() throws Exception {
        assertTrue(permissionService.userCanViewTimesheet(coord, sheet));
    }

    @Test
    public void testAdminCanViewTimesheet() throws Exception {
        assertTrue(permissionService.userCanViewTimesheet(admin, sheet));
    }

    /*
    @Test
    public void testEveCantViewTimesheet() throws Exception {
        configService.addTimesheetAdmin("test1", "test1_key");
        configService.addTimesheetAdminGroup("testGroup1");

        TimesheetAdmin[] approvedUsers = {timesheetAdmin};
        TSAdminGroup[] approvedGroups = {timesheetAdminGroup};

        userGroupNames.add("abc");
        userGroupNames.add("def");

        Mockito.when(configService.getConfiguration()).thenReturn(config);
        Mockito.when(config.getTimesheetAdminUsers()).thenReturn(approvedUsers);
        Mockito.when(config.getTimesheetAdminGroups()).thenReturn(approvedGroups);

        Mockito.when(componentAccessor.getGroupManager().getGroupNamesForUser(eve.getUsername())).thenReturn(userGroupNames);

        assertFalse(permissionService.userCanViewTimesheet(eve, sheet));
    }
    */

    @Test
    public void testNullUserCantViewTimesheet() throws Exception {
        assertFalse(permissionService.userCanViewTimesheet(null, sheet));
    }

    @Test
    public void testIfUserExistsOk() throws Exception {
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(owner);
        Mockito.when(permissionService.checkIfUserExists(request)).thenReturn(owner);
        ApplicationUser responseProfile = permissionService.checkIfUserExists(request);
        assertTrue(responseProfile.equals(owner));
    }

    @Test
    public void testIfUserExistsWrongUserProfile() throws Exception {
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(admin);
        Mockito.when(permissionService.checkIfUserExists(request)).thenReturn(eve);
        ApplicationUser responseProfile = permissionService.checkIfUserExists(request);
        Assert.assertFalse(responseProfile == admin);
    }

    @Test(expected = PermissionException.class)
    public void testIfUserExistsExceptionHandling() throws PermissionException {
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);
        Assert.assertEquals(permissionService.checkIfUserExists(request), PermissionException.class);
    }

    @Test
    public void testIfUserCanEditTimesheetEntry() throws Exception {
        JsonTimesheetEntry entry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), timeSheetEntry.getBeginDate(), timeSheetEntry.getBeginDate()
                , timeSheetEntry.getPauseMinutes(), timeSheetEntry.getDescription(), 1, 1, "None", "", false);

        permissionServiceException.userCanEditTimesheetEntry(owner, sheet, entry);
        Mockito.verify(permissionServiceException).userCanEditTimesheetEntry(owner, sheet, entry);
    }

    @Test(expected = PermissionException.class)
    public void userCanEditTimesheetEntryOldEntryException() throws Exception {
        Mockito.when(sheet.getUserKey()).thenReturn("owner_key");

        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        Mockito.when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-1015 00:01"));
        Mockito.when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("31-12-1015 23:59"));
        Mockito.when(timeSheetEntry.getDescription()).thenReturn("My First Entry");
        Mockito.when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        Mockito.when(timeSheetEntry.getID()).thenReturn(1);
        Mockito.when(teamService.getTeamByID(1)).thenReturn(team);
        Mockito.when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        Mockito.when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        JsonTimesheetEntry entry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), timeSheetEntry.getBeginDate(), timeSheetEntry.getBeginDate()
                , timeSheetEntry.getPauseMinutes(), timeSheetEntry.getDescription(), 1, 1, "None", "", false);

        permissionService.userCanEditTimesheetEntry(owner, sheet, entry);
    }

    @Test(expected = PermissionException.class)
    public void userCanEditTimesheetEntryNotAdminException() throws Exception {
        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        Mockito.when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-1015 00:01"));
        Mockito.when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("31-12-1015 23:59"));
        Mockito.when(timeSheetEntry.getDescription()).thenReturn("My First Entry");
        Mockito.when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        Mockito.when(timeSheetEntry.getID()).thenReturn(1);
        Mockito.when(teamService.getTeamByID(1)).thenReturn(team);
        Mockito.when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        Mockito.when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        JsonTimesheetEntry entry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), timeSheetEntry.getBeginDate(), timeSheetEntry.getBeginDate()
                , timeSheetEntry.getPauseMinutes(), timeSheetEntry.getDescription(), 1, 1,
                "None", "", false);

        permissionService.userCanEditTimesheetEntry(owner, sheet, entry);
    }

    @Test
    public void testIfUserCanDeleteTimesheetEntry() throws Exception {
        TimesheetEntry newEntry = Mockito.mock(TimesheetEntry.class);
        Mockito.when(newEntry.getID()).thenReturn(1);

        permissionServiceException.userCanDeleteTimesheetEntry(owner, newEntry);
        Mockito.verify(permissionServiceException).userCanDeleteTimesheetEntry(owner, newEntry);
    }

    @Test(expected = PermissionException.class)
    public void testUserCanNotDeleteTimesheetException() throws Exception {
        Mockito.when(sheet.getUserKey()).thenReturn("owner_key");

        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        Mockito.when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-1015 00:01"));
        Mockito.when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("31-12-1015 23:59"));
        Mockito.when(timeSheetEntry.getDescription()).thenReturn("My First Entry");
        Mockito.when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        Mockito.when(timeSheetEntry.getID()).thenReturn(1);
        Mockito.when(timeSheetEntry.getTimeSheet()).thenReturn(sheet);
        Mockito.when(teamService.getTeamByID(1)).thenReturn(team);
        Mockito.when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        Mockito.when(sheetService.getTimesheetByUser("admin_key", false)).thenReturn(sheet);
        Mockito.when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        permissionService.userCanDeleteTimesheetEntry(eve, timeSheetEntry);
        //Mockito.verify(permissionServiceException).userCanDeleteTimesheetEntry(admin, timeSheetEntry);
    }

    @Test(expected = PermissionException.class)
    public void userDoesNotExistException() throws Exception {

        Mockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(eve);
        Mockito.when(permissionService.checkIfUserExists(request)).thenReturn(null);
        permissionService.checkIfUserExists(request);
    }

    @Test(expected = PermissionException.class)
    public void testNullUserCanNotEditTimesheetException() throws Exception {
        JsonTimesheetEntry entry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), timeSheetEntry.getBeginDate(), timeSheetEntry.getBeginDate()
                , timeSheetEntry.getPauseMinutes(), timeSheetEntry.getDescription(), 1, 1, "None", "", false);

        permissionService.userCanEditTimesheetEntry(eve, sheet, entry);
        //Mockito.verify(permissionServiceException).userCanEditTimesheetEntry(eve, sheet, entry);
    }

    @Test(expected = PermissionException.class)
    public void testNullUserCanNotDeleteTimesheetException() throws Exception {
        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        Mockito.when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-1015 00:01"));
        Mockito.when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("31-12-1015 23:59"));
        Mockito.when(timeSheetEntry.getDescription()).thenReturn("My First Entry");
        Mockito.when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        Mockito.when(timeSheetEntry.getID()).thenReturn(1);
        Mockito.when(teamService.getTeamByID(1)).thenReturn(team);
        Mockito.when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        Mockito.when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        permissionService.userCanDeleteTimesheetEntry(eve, timeSheetEntry);
        //Mockito.verify(permissionServiceException).userCanDeleteTimesheetEntry(eve, timeSheetEntry);
    }

    @Test(expected = PermissionException.class)
    public void testUserCanNotDeleteOldDateTimesheetException() throws Exception {
        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        Mockito.when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-2015 00:01"));
        Mockito.when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("01-01-2015 23:59"));
        Mockito.when(timeSheetEntry.getDescription()).thenReturn("My First Old Entry");
        Mockito.when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        Mockito.when(timeSheetEntry.getID()).thenReturn(1);
        Mockito.when(teamService.getTeamByID(1)).thenReturn(team);
        Mockito.when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        Mockito.when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        permissionService.userCanDeleteTimesheetEntry(owner, timeSheetEntry);
        //Mockito.verify(permissionServiceException).userCanDeleteTimesheetEntry(owner, timeSheetEntry);
    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Team.class);
            catroid = em.create(Team.class);
            catroid.setTeamName("catroid");
            catroid.save();

            html5 = em.create(Team.class);
            html5.setTeamName("html5");
            html5.save();

            drone = em.create(Team.class);
            drone.setTeamName("drone");
            drone.save();

            em.migrate(TimesheetAdmin.class);
            timesheetAdmin = em.create(TimesheetAdmin.class);
            timesheetAdmin.setConfiguration(config);
            timesheetAdmin.setUserKey("APPROVED_KEY");
            timesheetAdmin.setUserName("TimesheetAdmin");

            em.migrate(TSAdminGroup.class);
            timesheetAdminGroup = em.create(TSAdminGroup.class);
            timesheetAdminGroup.setConfiguration(config);
            timesheetAdminGroup.setGroupName("TSAdminGroup");
        }
    }
}
