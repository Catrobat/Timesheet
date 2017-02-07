package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.service.ServiceException;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Group;
import org.catrobat.jira.timesheet.activeobjects.TeamToGroup;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.impl.TeamServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetEntryServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.*;
import org.powermock.api.support.membermodification.MemberModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TeamServiceImplTest.MyDatabaseUpdater.class)

public class TeamServiceImplTest {

    private static Team catroid, html5, drone, emptyTeam;
    private static Group developer1, coordinator1;
    @Rule
    public org.mockito.junit.MockitoRule mockitoRule = MockitoJUnit.rule();
    private EntityManager entityManager;
    private TeamService service;
    private TimesheetEntryService entryService;
    private ActiveObjects ao;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        entryService = new TimesheetEntryServiceImpl(ao);
        service = new TeamServiceImpl(ao, entryService);
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
        ActiveObjects ao = Mockito.mock(ActiveObjects.class);

        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "ao").set(service, ao);

        Mockito.doReturn(null).when(ao).find(Team.class, "ID = ?", 0);

        Team receivedTeam = service.getTeamByID(0);
        Assert.assertEquals(null, receivedTeam);

        Mockito.verify(ao).find(Team.class, "ID = ?", 0);
    }

    @Test
    public void testGetTeamByNameIsNull() throws IllegalAccessException, ServiceException {
        ActiveObjects ao = Mockito.mock(ActiveObjects.class);

        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "ao").set(service, ao);

        Mockito.doReturn(null).when(ao).find(Team.class, "TEAM_NAME = ?", "Test");

        Team[] defaultTeam = {Mockito.mock(Team.class)};
        Mockito.when(ao.find(Team.class, "TEAM_NAME = ?", "Default")).thenReturn(defaultTeam);

        Team receivedTeam = service.getTeamByName("Test");
        Assert.assertEquals(null, receivedTeam);

        Mockito.verify(ao).find(Team.class, "TEAM_NAME = ?", "Test");
    }

    @Test(expected = ServiceException.class)
    public void testGetTeamByIDLengthCheck() throws IllegalAccessException, ServiceException {
        ActiveObjects ao = Mockito.mock(ActiveObjects.class);

        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "ao").set(service, ao);

        Team team1 = Mockito.mock(Team.class);
        Team team2 = Mockito.mock(Team.class);

        Team[] teams = {team1, team2};

        Mockito.doReturn(teams).when(ao).find(Team.class, "ID = ?", 0);

        service.getTeamByID(0);
    }

    @Test(expected = ServiceException.class)
    public void testGetTeamBNameLengthCheck() throws IllegalAccessException, ServiceException {
        ActiveObjects ao = Mockito.mock(ActiveObjects.class);

        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "ao").set(service, ao);

        Team team1 = Mockito.mock(Team.class);
        Team team2 = Mockito.mock(Team.class);

        Team[] teams = {team1, team2};

        Mockito.doReturn(teams).when(ao).find(Team.class, "TEAM_NAME = ?", "Test");

        Team[] defaultTeam = {Mockito.mock(Team.class)};
        Mockito.when(ao.find(Team.class, "TEAM_NAME = ?", "Default")).thenReturn(defaultTeam);

        service.getTeamByName("Test");
    }

    @Test
    public void testRemoveTeam() throws IllegalAccessException, ServiceException {
        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "ao").set(service, ao);

        boolean result = service.removeTeam(emptyTeam.getTeamName());
        assertEquals(true, result);
        assertEquals(0, ao.find(Team.class, "TEAM_NAME = ?", emptyTeam.getTeamName()).length);
    }

    @Test
    public void testRemoveTeamIsNull() throws IllegalAccessException, ServiceException {
        String notExistent = "notExistent";
        boolean result = service.removeTeam(notExistent);

        assertEquals(false, result);
        assertEquals(0, ao.find(Team.class, "TEAM_NAME = ?", notExistent).length);
    }

    @Test(expected = ServiceException.class)
    public void testRemoveTeamLengthCheck() throws IllegalAccessException, ServiceException {
        Team duplicate = ao.create(Team.class);
        duplicate.setTeamName("Drone");
        duplicate.save();

        service.removeTeam("Drone");
    }

    @Test
    public void testAll() {
        List<Team> teamList = service.all();

        assertEquals(5, teamList.size());
    }

    @Test
    public void testGetTeamsOfUser() throws IllegalAccessException {
        Team teamA = Mockito.mock(Team.class);
        Team teamB = Mockito.mock(Team.class);
        Team teamC = Mockito.mock(Team.class);

        Mockito.doReturn("teamA").when(teamA).getTeamName();
        Mockito.doReturn("teamB").when(teamB).getTeamName();
        Mockito.doReturn("teamC").when(teamC).getTeamName();

        List<String> developerListA = new ArrayList<String>();
        developerListA.add("MarkusHobisch");
        developerListA.add("AdrianSchnedlitz");
        List<String> developerListB = new ArrayList<String>();
        developerListB.add("PatrickRadkohl");
        developerListB.add("GeraldWagner");
        List<String> developerListC = new ArrayList<String>();
        developerListC.add("ThomasGrossman");
        developerListC.add("MarkusHobisch");

        Set<Team> teamsOfUser = service.getTeamsOfUser(developer1.getGroupName());
        assertEquals(2, teamsOfUser.size());
        assertTrue(teamsOfUser.contains(catroid));
        assertTrue(teamsOfUser.contains(drone));
    }

    @Test
    public void testGetCoordinatorTeamsOfUser() throws IllegalAccessException {
        Team teamA = Mockito.mock(Team.class);
        Team teamB = Mockito.mock(Team.class);
        Team teamC = Mockito.mock(Team.class);

        Mockito.doReturn("teamA").when(teamA).getTeamName();
        Mockito.doReturn("teamB").when(teamB).getTeamName();
        Mockito.doReturn("teamC").when(teamC).getTeamName();

        List<String> coordinatorListA = new ArrayList<>();
        coordinatorListA.add("MarkusHobisch");
        coordinatorListA.add("AdrianSchnedlitz");
        List<String> coordinatorListB = new ArrayList<>();
        coordinatorListB.add("PatrickRadkohl");
        coordinatorListB.add("GeraldWagner");
        List<String> coordinatorListC = new ArrayList<>();
        coordinatorListC.add("ThomasGrossman");
        coordinatorListC.add("MarkusHobisch");

        Set<Team> teamsOfCoords = service.getTeamsOfCoordinator(coordinator1.getGroupName());
        assertEquals(2, teamsOfCoords.size());
        assertTrue(teamsOfCoords.contains(html5));
        assertTrue(teamsOfCoords.contains(drone));
    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Team.class);
            em.migrate(TimesheetEntry.class);
            em.migrate(TeamToGroup.class);

            catroid = em.create(Team.class);
            catroid.setTeamName("Catroid");
            catroid.save();

            html5 = em.create(Team.class);
            html5.setTeamName("HTML5");
            html5.save();

            drone = em.create(Team.class);
            drone.setTeamName("Drone");
            drone.save();

            emptyTeam = em.create(Team.class);
            emptyTeam.setTeamName("Empty");
            emptyTeam.save();

            developer1 = em.create(Group.class);
            developer1.setGroupName("Developer1");
            developer1.save();

            coordinator1 = em.create(Group.class);
            coordinator1.setGroupName("Coordinator1");
            coordinator1.save();

            TeamToGroup dev1ToDrone = em.create(TeamToGroup.class);
            dev1ToDrone.setGroup(developer1);
            dev1ToDrone.setTeam(drone);
            dev1ToDrone.setRole(TeamToGroup.Role.DEVELOPER);
            dev1ToDrone.save();
            TeamToGroup dev1ToCatroid = em.create(TeamToGroup.class);
            dev1ToCatroid.setGroup(developer1);
            dev1ToCatroid.setTeam(catroid);
            dev1ToCatroid.setRole(TeamToGroup.Role.DEVELOPER);
            dev1ToCatroid.save();

            TeamToGroup coord1ToDrone = em.create(TeamToGroup.class);
            coord1ToDrone.setGroup(coordinator1);
            coord1ToDrone.setTeam(drone);
            coord1ToDrone.setRole(TeamToGroup.Role.COORDINATOR);
            coord1ToDrone.save();
            TeamToGroup coord1ToHTML5 = em.create(TeamToGroup.class);
            coord1ToHTML5.setGroup(coordinator1);
            coord1ToHTML5.setTeam(html5);
            coord1ToHTML5.setRole(TeamToGroup.Role.COORDINATOR);
            coord1ToHTML5.save();

            TimesheetEntry timesheetEntry = em.create(TimesheetEntry.class);
            timesheetEntry.save();
        }
    }
}
