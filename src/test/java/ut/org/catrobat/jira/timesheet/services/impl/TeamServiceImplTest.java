package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsSqlException;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.util.UserManager;

import net.java.ao.DBParam;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.services.impl.CategoryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TeamServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetEntryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetServiceImpl;
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

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(TeamServiceImplTest.MyDatabaseUpdater.class)
@PrepareForTest(ComponentAccessor.class)
public class TeamServiceImplTest {

    private static Team catroid, html5, drone, emptyTeam;
    private static Group developer1, coordinator1;
    @Rule
    public org.mockito.junit.MockitoRule mockitoRule = MockitoJUnit.rule();
    private EntityManager entityManager;
    private TeamService service;
    private ActiveObjects ao;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        
        UserManager userManagerJiraMock = mock(UserManager.class, RETURNS_DEEP_STUBS);
        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        
        TimesheetService timesheetService = new TimesheetServiceImpl(ao);
        CategoryService categoryService = new CategoryServiceImpl(ao);
        TimesheetEntryService entryService = new TimesheetEntryServiceImpl(ao, timesheetService);
        service = new TeamServiceImpl(ao, categoryService, entryService);
    }

    @Test
    public void testAddAndGetTeamByID() throws Exception {
        Team addedTeam = service.add("Test");
        Team receivedTeam = service.getTeamByID(addedTeam.getID());
        Assert.assertEquals(addedTeam, receivedTeam);
    }

    @Test
    public void testAddAndGetTeamByName() throws Exception {
        Team addedTeam = service.add("Test");
        Team receivedTeam = service.getTeamByName("Test");
        Assert.assertEquals(addedTeam, receivedTeam);
    }

    @Test
    public void testGetTeamByIDIsNull() throws IllegalAccessException, ServiceException {
        Team receivedTeam = service.getTeamByID(0);
        Assert.assertEquals(null, receivedTeam);
    }

    @Test
    public void testGetTeamByNameIsNull() throws IllegalAccessException, ServiceException {
        Team receivedTeam = service.getTeamByName("Test");
        Assert.assertEquals(null, receivedTeam);
    }

    @Test(expected = ActiveObjectsSqlException.class)
    public void testDuplicateNameThrowsException() throws ActiveObjectsSqlException {
        ao.create(Team.class,
            new DBParam("TEAM_NAME", "Drone")
        );
    }

    @Test
    public void testRemoveTeam() throws IllegalAccessException, ServiceException {
        service.removeTeam(emptyTeam.getTeamName());
        assertEquals(0, ao.find(Team.class, "TEAM_NAME = ?", emptyTeam.getTeamName()).length);
    }

    @Test(expected = ServiceException.class)
    public void testRemoveTeamIsNull() throws IllegalAccessException, ServiceException {
        String notExistent = "notExistent";
        service.removeTeam(notExistent);

        assertEquals(0, ao.find(Team.class, "TEAM_NAME = ?", notExistent).length);
    }

    @Test
    public void testAll() {
        List<Team> teamList = service.all();

        assertEquals(5, teamList.size());
    }

    
    @Test
    public void testGetTeamsOfUser() throws IllegalAccessException {

    	String userKey = "USER_KEY_1";
    	when(ComponentAccessor.getUserManager().getUserByName(anyString()).getKey()).thenReturn(userKey);
    	
        Team teamA = Mockito.mock(Team.class);
        Team teamB = Mockito.mock(Team.class);
        Team teamC = Mockito.mock(Team.class);

        Mockito.doReturn("teamA").when(teamA).getTeamName();
        Mockito.doReturn("teamB").when(teamB).getTeamName();
        Mockito.doReturn("teamC").when(teamC).getTeamName();

        Set<Team> teamsOfUser = service.getTeamsOfUser(developer1.getGroupName());
        assertEquals(2, teamsOfUser.size());
        assertTrue(teamsOfUser.contains(catroid));
        assertTrue(teamsOfUser.contains(drone));
    }

//    @Test
//    public void testGetCoordinatorTeamsOfUser() throws IllegalAccessException {
//        Team teamA = Mockito.mock(Team.class);
//        Team teamB = Mockito.mock(Team.class);
//        Team teamC = Mockito.mock(Team.class);
//
//        Mockito.doReturn("teamA").when(teamA).getTeamName();
//        Mockito.doReturn("teamB").when(teamB).getTeamName();
//        Mockito.doReturn("teamC").when(teamC).getTeamName();
//
//        Set<Team> teamsOfCoords = service.getTeamsOfCoordinator(coordinator1.getGroupName());
//        assertEquals(2, teamsOfCoords.size());
//        assertTrue(teamsOfCoords.contains(html5));
//        assertTrue(teamsOfCoords.contains(drone));
//    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Team.class);
            em.migrate(TimesheetEntry.class);
            em.migrate(TeamToGroup.class);
            em.migrate(CategoryToTeam.class);

            catroid = em.create(Team.class,
                new DBParam("TEAM_NAME", "Catroid")
            );

            html5 = em.create(Team.class,
                new DBParam("TEAM_NAME", "HTML5")
            );

            drone = em.create(Team.class,
                new DBParam("TEAM_NAME", "Drone")
            );

            emptyTeam = em.create(Team.class,
                new DBParam("TEAM_NAME", "Empty")
            );

            developer1 = em.create(Group.class);
            developer1.setGroupName("Developer1");
            developer1.save();

            coordinator1 = em.create(Group.class);
            coordinator1.setGroupName("Coordinator1");
            coordinator1.save();

            TeamToGroup dev1ToDrone = em.create(TeamToGroup.class,
                new DBParam("GROUP_ID", developer1),
                new DBParam("TEAM_ID", drone),
                new DBParam("ROLE", TeamToGroup.Role.DEVELOPER)
            );

            TeamToGroup dev1ToCatroid = em.create(TeamToGroup.class,
                new DBParam("GROUP_ID", developer1),
                new DBParam("TEAM_ID", catroid),
                new DBParam("ROLE", TeamToGroup.Role.DEVELOPER)
            );

            TeamToGroup coord1ToDrone = em.create(TeamToGroup.class,
                new DBParam("GROUP_ID", coordinator1),
                new DBParam("TEAM_ID", drone),
                new DBParam("ROLE", TeamToGroup.Role.COORDINATOR)
            );

            TeamToGroup coord1ToHTML5 = em.create(TeamToGroup.class,
                new DBParam("GROUP_ID", coordinator1),
                new DBParam("TEAM_ID", html5),
                new DBParam("ROLE", TeamToGroup.Role.COORDINATOR)
            );

            TimesheetEntry timesheetEntry = em.create(TimesheetEntry.class);
        }
    }
}
