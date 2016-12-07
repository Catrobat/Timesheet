package ut.org.catrobat.jira.timesheet.services.impl;


import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
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
import org.catrobat.jira.timesheet.services.*;
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
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(PermissionServiceImplTest.MyDatabaseUpdater.class)
@PrepareForTest(ComponentAccessor.class)
public class PermissionServiceImplTest {

    private static Team catroid, html5, drone;
    private static TimesheetAdmin timesheetAdmin;
    private static TSAdminGroup timesheetAdminGroup;
    @Rule
    public org.mockito.junit.MockitoRule mockitoRule = MockitoJUnit.rule();
    private PermissionServiceImpl permissionService, permissionServiceException;
    private TeamService teamService;
    private ApplicationUser coord;
    private ApplicationUser owner;
    private ApplicationUser eve;
    private ApplicationUser test;
    private ApplicationUser jiraAdmin;
    private ApplicationUser tsAdmin;
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
    private TestActiveObjects ao;
    private static Config config;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ComponentAccessor.class);

        ao = new TestActiveObjects(entityManager);

        teamService = mock(TeamService.class);
        configService = mock(ConfigService.class);
        groupManager = mock(GroupManager.class);
        jiraUserManager = mock(UserManager.class);
        jiraAuthenticationContext = mock(JiraAuthenticationContext.class);

        assertNotNull(entityManager);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        PowerMockito.when(ComponentAccessor.getApplicationProperties()).thenReturn(applicationProperties);
        when(applicationProperties.getString(APKeys.JIRA_BASEURL)).thenReturn("jira.catrob.at");

        permissionService = new PermissionServiceImpl(teamService, configService);

        //users / roles
        coord = mock(ApplicationUser.class);
        owner = mock(ApplicationUser.class);
        eve = mock(ApplicationUser.class);
        test = mock(ApplicationUser.class);
        jiraAdmin = mock(ApplicationUser.class);
        tsAdmin = mock(ApplicationUser.class);

        sheet = mock(Timesheet.class);
        sheetService = mock(TimesheetService.class);
        entryService = mock(TimesheetEntryService.class);
        timeSheetEntry = mock(TimesheetEntry.class);
        team = mock(Team.class);
        request = mock(HttpServletRequest.class);
        permissionServiceException = mock(PermissionServiceImpl.class);

        //real world objects
        ao.create(Config.class).save();
        config = ao.find(Config.class)[0];

        String coord_key = "coord_key";
        String owner_key = "owner_key";
        String eve_key = "eve_key";
        String test_key = "test_key";
        String jira_admin_key = "jira_admin_key";
        String ts_admin_key = "ts_admin_key";

        when(sheet.getUserKey()).thenReturn("owner_key");

        when(coord.getKey()).thenReturn(coord_key);
        when(owner.getKey()).thenReturn(owner_key);
        when(eve.getKey()).thenReturn(eve_key);
        when(test.getKey()).thenReturn(test_key);
        when(jiraAdmin.getKey()).thenReturn(jira_admin_key);
        when(tsAdmin.getKey()).thenReturn(ts_admin_key);

        when(coord.getUsername()).thenReturn("coord");
        when(owner.getUsername()).thenReturn("owner");
        when(eve.getUsername()).thenReturn("eve");
        when(test.getUsername()).thenReturn("test");
        when(jiraAdmin.getUsername()).thenReturn("admin");
        when(tsAdmin.getUsername()).thenReturn("ts_admin");

        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(jiraUserManager);
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext()).thenReturn(jiraAuthenticationContext);
        PowerMockito.when(ComponentAccessor.getGroupManager()).thenReturn(groupManager);

        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(owner, PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(jiraAdmin, PermissionService.JIRA_ADMINISTRATORS)).thenReturn(true);
        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(eve, PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(test, PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(coord, PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);
        PowerMockito.when(ComponentAccessor.getGroupManager().isUserInGroup(tsAdmin, PermissionService.JIRA_ADMINISTRATORS)).thenReturn(false);

        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(owner_key)).thenReturn(owner);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(jira_admin_key)).thenReturn(jiraAdmin);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(eve_key)).thenReturn(eve);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(test_key)).thenReturn(test);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(coord_key)).thenReturn(coord);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(ts_admin_key)).thenReturn(tsAdmin);

        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(owner.getUsername())).thenReturn(owner);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(jiraAdmin.getUsername())).thenReturn(jiraAdmin);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(eve.getUsername())).thenReturn(eve);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(test.getUsername())).thenReturn(test);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(coord.getUsername())).thenReturn(coord);
        PowerMockito.when(ComponentAccessor.getUserManager().getUserByKey(tsAdmin.getUsername())).thenReturn(tsAdmin);

        Set<Team> owner_teams = new HashSet<>();
        Set<Team> eve_teams = new HashSet<>();
        Set<Team> coord_cteams = new HashSet<>();
        Set<Team> no_teams = new HashSet<>();

        owner_teams.add(html5);
        owner_teams.add(drone);
        eve_teams.add(catroid);
        coord_cteams.add(html5);

        when(teamService.getTeamsOfUser("owner")).thenReturn(owner_teams);
        when(teamService.getTeamsOfUser("eve")).thenReturn(eve_teams);
        when(teamService.getTeamsOfCoordinator("coord")).thenReturn(coord_cteams);

        when(teamService.getTeamsOfUser("coord")).thenReturn(coord_cteams);
        when(teamService.getTeamsOfUser("admin")).thenReturn(no_teams);
        when(teamService.getTeamsOfCoordinator("owner")).thenReturn(owner_teams);
        when(teamService.getTeamsOfCoordinator("eve")).thenReturn(eve_teams);
        when(teamService.getTeamsOfCoordinator("admin")).thenReturn(no_teams);
        when(configService.getConfiguration()).thenReturn(config);
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
        config.setReadOnlyUsers("Markus,Adrian");
        assertFalse(permissionService.userCanViewTimesheet(jiraAdmin, sheet));
    }

    @Test
    public void testNullUserCantViewTimesheet() throws Exception {
        assertFalse(permissionService.userCanViewTimesheet(null, sheet));
    }

    @Test
    public void testIfUserExistsOk() throws Exception {
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(owner);
        when(permissionService.checkIfUserExists()).thenReturn(owner);
        ApplicationUser responseProfile = permissionService.checkIfUserExists();
        assertTrue(responseProfile.equals(owner));
    }

    @Test
    public void testIfUserExistsWrongUserProfile() throws Exception {
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(jiraAdmin);
        when(permissionService.checkIfUserExists()).thenReturn(eve);
        ApplicationUser responseProfile = permissionService.checkIfUserExists();
        Assert.assertFalse(responseProfile == jiraAdmin);
    }

    @Test(expected = PermissionException.class)
    public void testIfUserExistsExceptionHandling() throws PermissionException {
        PowerMockito.when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(null);
        Assert.assertEquals(permissionService.checkIfUserExists(), PermissionException.class);
    }

    @Test
    public void testIfUserCanEditTimesheetEntry() throws Exception {
        JsonTimesheetEntry entry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), timeSheetEntry.getBeginDate(), timeSheetEntry.getBeginDate()
                , timeSheetEntry.getPauseMinutes(), timeSheetEntry.getDescription(), 1, 1, "None", "", false);

        permissionServiceException.userCanEditTimesheetEntry(owner, sheet, entry);
        verify(permissionServiceException).userCanEditTimesheetEntry(owner, sheet, entry);
    }

    @Test(expected = PermissionException.class)
    public void userCanEditTimesheetEntryOldEntryException() throws Exception {
        when(sheet.getUserKey()).thenReturn("owner_key");

        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-1015 00:01"));
        when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("31-12-1015 23:59"));
        when(timeSheetEntry.getDescription()).thenReturn("My First Entry");
        when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        when(timeSheetEntry.getID()).thenReturn(1);
        when(teamService.getTeamByID(1)).thenReturn(team);
        when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        JsonTimesheetEntry entry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), timeSheetEntry.getBeginDate(), timeSheetEntry.getBeginDate()
                , timeSheetEntry.getPauseMinutes(), timeSheetEntry.getDescription(), 1, 1, "None", "", false);

        permissionService.userCanEditTimesheetEntry(owner, sheet, entry);
    }

    @Test(expected = PermissionException.class)
    public void userCanEditTimesheetEntryNotAdminException() throws Exception {
        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-1015 00:01"));
        when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("31-12-1015 23:59"));
        when(timeSheetEntry.getDescription()).thenReturn("My First Entry");
        when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        when(timeSheetEntry.getID()).thenReturn(1);
        when(teamService.getTeamByID(1)).thenReturn(team);
        when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        JsonTimesheetEntry entry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), timeSheetEntry.getBeginDate(), timeSheetEntry.getBeginDate()
                , timeSheetEntry.getPauseMinutes(), timeSheetEntry.getDescription(), 1, 1,
                "None", "", false);

        permissionService.userCanEditTimesheetEntry(owner, sheet, entry);
    }

    @Test
    public void testIfUserCanDeleteTimesheetEntry() throws Exception {
        TimesheetEntry newEntry = Mockito.mock(TimesheetEntry.class);
        when(newEntry.getID()).thenReturn(1);

        permissionServiceException.userCanDeleteTimesheetEntry(owner, newEntry);
        verify(permissionServiceException).userCanDeleteTimesheetEntry(owner, newEntry);
    }

    @Test(expected = PermissionException.class)
    public void testUserCanNotDeleteTimesheetException() throws Exception {
        when(sheet.getUserKey()).thenReturn("owner_key");

        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-1015 00:01"));
        when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("31-12-1015 23:59"));
        when(timeSheetEntry.getDescription()).thenReturn("My First Entry");
        when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        when(timeSheetEntry.getID()).thenReturn(1);
        when(timeSheetEntry.getTimeSheet()).thenReturn(sheet);
        when(teamService.getTeamByID(1)).thenReturn(team);
        when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        when(sheetService.getTimesheetByUser("admin_key", false)).thenReturn(sheet);
        when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        permissionService.userCanDeleteTimesheetEntry(eve, timeSheetEntry);
        verify(permissionServiceException).userCanDeleteTimesheetEntry(jiraAdmin, timeSheetEntry);
    }

    @Test(expected = PermissionException.class)
    public void userDoesNotExistException() throws Exception {

        when(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()).thenReturn(eve);
        when(permissionService.checkIfUserExists()).thenReturn(null);
        permissionService.checkIfUserExists();
    }

    @Test(expected = PermissionException.class)
    public void testNullUserCanNotEditTimesheetException() throws Exception {
        JsonTimesheetEntry entry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), timeSheetEntry.getBeginDate(), timeSheetEntry.getBeginDate()
                , timeSheetEntry.getPauseMinutes(), timeSheetEntry.getDescription(), 1, 1, "None", "", false);

        permissionService.userCanEditTimesheetEntry(eve, sheet, entry);
        verify(permissionServiceException).userCanEditTimesheetEntry(eve, sheet, entry);
    }

    @Test(expected = PermissionException.class)
    public void testNullUserCanNotDeleteTimesheetException() throws Exception {
        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-1015 00:01"));
        when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("31-12-1015 23:59"));
        when(timeSheetEntry.getDescription()).thenReturn("My First Entry");
        when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        when(timeSheetEntry.getID()).thenReturn(1);
        when(teamService.getTeamByID(1)).thenReturn(team);
        when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        permissionService.userCanDeleteTimesheetEntry(eve, timeSheetEntry);
        verify(permissionServiceException).userCanDeleteTimesheetEntry(eve, timeSheetEntry);
    }

    @Test(expected = PermissionException.class)
    public void testUserCanNotDeleteOldDateTimesheetException() throws Exception {
        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-2015 00:01"));
        when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("01-01-2015 23:59"));
        when(timeSheetEntry.getDescription()).thenReturn("My First Old Entry");
        when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        when(timeSheetEntry.getID()).thenReturn(1);
        when(teamService.getTeamByID(1)).thenReturn(team);
        when(sheetService.getTimesheetByID(1)).thenReturn(sheet);
        when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);

        permissionService.userCanDeleteTimesheetEntry(owner, timeSheetEntry);
        verify(permissionServiceException).userCanDeleteTimesheetEntry(owner, timeSheetEntry);
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
