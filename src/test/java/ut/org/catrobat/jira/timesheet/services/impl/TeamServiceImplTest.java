package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.service.ServiceException;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.TeamToGroup;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.impl.TeamServiceImpl;
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

    private static Team catroid, html5, drone;
    @Rule
    public org.mockito.junit.MockitoRule mockitoRule = MockitoJUnit.rule();
    private EntityManager entityManager;
    private TeamService service;
    private ConfigService cs;
    private ActiveObjects ao;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        service = new TeamServiceImpl(ao, cs);
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

        service.getTeamByName("Test");
    }

    @Test
    public void testRemoveTeam() throws IllegalAccessException, ServiceException {
        ActiveObjects ao = Mockito.mock(ActiveObjects.class);

        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "ao").set(service, ao);

        Team team1 = Mockito.mock(Team.class);

        Team[] teams = {team1};

        Mockito.doReturn(teams).when(ao).find(Team.class, "TEAM_NAME = ?", "Test");

        boolean result = service.removeTeam("Test");
        assertEquals(true, result);

        Mockito.verify(ao).delete(teams);
    }

    @Test
    public void testRemoveTeamIsNull() throws IllegalAccessException, ServiceException {
        ActiveObjects ao = Mockito.mock(ActiveObjects.class);

        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "ao").set(service, ao);

        Mockito.doReturn(null).when(ao).find(Team.class, "TEAM_NAME = ?", "Test");

        boolean result = service.removeTeam("Test");

        assertEquals(false, result);

        Mockito.verify(ao).find(Team.class, "TEAM_NAME = ?", "Test");
    }

    @Test(expected = ServiceException.class)
    public void testRemoveTeamLengthCheck() throws IllegalAccessException, ServiceException {
        ActiveObjects ao = Mockito.mock(ActiveObjects.class);

        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "ao").set(service, ao);

        Team team1 = Mockito.mock(Team.class);
        Team team2 = Mockito.mock(Team.class);

        Team[] teams = {team1, team2};

        Mockito.doReturn(teams).when(ao).find(Team.class, "TEAM_NAME = ?", "Test");

        service.removeTeam("Test");
    }

    @Test
    public void testAll() {
        List<Team> teamList = service.all();

        assertEquals(3, teamList.size());
    }

    @Test
    public void testGetTeamsOfUser() throws IllegalAccessException {

        ConfigService configService = Mockito.mock(ConfigService.class);
        Config config = Mockito.mock(Config.class);

        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "configService").set(service, configService);

        Team teamA = Mockito.mock(Team.class);
        Team teamB = Mockito.mock(Team.class);
        Team teamC = Mockito.mock(Team.class);

        Mockito.doReturn("teamA").when(teamA).getTeamName();
        Mockito.doReturn("teamB").when(teamB).getTeamName();
        Mockito.doReturn("teamC").when(teamC).getTeamName();

        Team[] teams = {teamA, teamB, teamC};

        List<String> developerListA = new ArrayList<String>();
        developerListA.add("MarkusHobisch");
        developerListA.add("AdrianSchnedlitz");
        List<String> developerListB = new ArrayList<String>();
        developerListB.add("PatrickRadkohl");
        developerListB.add("GeraldWagner");
        List<String> developerListC = new ArrayList<String>();
        developerListC.add("ThomasGrossman");
        developerListC.add("MarkusHobisch");


        Mockito.doReturn(teams).when(config).getTeams();
        Mockito.doReturn(config).when(configService).getConfiguration();
        Mockito.doReturn(developerListA).when(configService).getGroupsForRole("teamA", TeamToGroup.Role.DEVELOPER);
        Mockito.doReturn(developerListB).when(configService).getGroupsForRole("teamB", TeamToGroup.Role.DEVELOPER);
        Mockito.doReturn(developerListC).when(configService).getGroupsForRole("teamC", TeamToGroup.Role.DEVELOPER);

        Set<Team> teamsOfUser = service.getTeamsOfUser("MarkusHobisch");
        assertEquals(2, teamsOfUser.size());
        assertTrue(teamsOfUser.contains(teamA));
        assertTrue(teamsOfUser.contains(teamC));

    }

    @Test
    public void testGetCoordinatorTeamsOfUser() throws IllegalAccessException {

        ConfigService configService = Mockito.mock(ConfigService.class);
        Config config = Mockito.mock(Config.class);

        // mock private field/variable
        MemberModifier.field(TeamServiceImpl.class, "configService").set(service, configService);

        Team teamA = Mockito.mock(Team.class);
        Team teamB = Mockito.mock(Team.class);
        Team teamC = Mockito.mock(Team.class);

        Mockito.doReturn("teamA").when(teamA).getTeamName();
        Mockito.doReturn("teamB").when(teamB).getTeamName();
        Mockito.doReturn("teamC").when(teamC).getTeamName();

        Team[] teams = {teamA, teamB, teamC};

        List<String> coordinatorListA = new ArrayList<String>();
        coordinatorListA.add("MarkusHobisch");
        coordinatorListA.add("AdrianSchnedlitz");
        List<String> coordinatorListB = new ArrayList<String>();
        coordinatorListB.add("PatrickRadkohl");
        coordinatorListB.add("GeraldWagner");
        List<String> coordinatorListC = new ArrayList<String>();
        coordinatorListC.add("ThomasGrossman");
        coordinatorListC.add("MarkusHobisch");


        Mockito.doReturn(teams).when(config).getTeams();
        Mockito.doReturn(config).when(configService).getConfiguration();
        Mockito.doReturn(coordinatorListA).when(configService).getGroupsForRole("teamA", TeamToGroup.Role.COORDINATOR);
        Mockito.doReturn(coordinatorListB).when(configService).getGroupsForRole("teamB", TeamToGroup.Role.COORDINATOR);
        Mockito.doReturn(coordinatorListC).when(configService).getGroupsForRole("teamC", TeamToGroup.Role.COORDINATOR);

        Set<Team> teamsOfCoords = service.getTeamsOfCoordinator("MarkusHobisch");
        assertEquals(2, teamsOfCoords.size());
        assertTrue(teamsOfCoords.contains(teamA));
        assertTrue(teamsOfCoords.contains(teamC));

    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Team.class);
            catroid = em.create(Team.class);
            catroid.setTeamName("Catroid");
            catroid.save();

            html5 = em.create(Team.class);
            html5.setTeamName("HTML5");
            html5.save();

            drone = em.create(Team.class);
            drone.setTeamName("Drone");
            drone.save();
        }
    }
}
