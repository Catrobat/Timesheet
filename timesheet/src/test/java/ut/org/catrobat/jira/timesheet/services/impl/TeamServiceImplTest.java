package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.security.groups.GroupManager;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.impl.TeamServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;

import static org.junit.Assert.assertNotNull;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TeamServiceImplTest.MyDatabaseUpdater.class)

public class TeamServiceImplTest {

    private EntityManager entityManager;
    private TeamService service;
    private ActiveObjects ao;
    private GroupManager gm;
    private ConfigService cs;
    private static Team catroid, html5, drone;

    @Rule
    public org.mockito.junit.MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() throws Exception {
        gm = Mockito.mock(GroupManager.class);
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        service = new TeamServiceImpl(ao, cs);
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

  /*
  @Test
  public void testGetTeamsOfUser() throws Exception
  {
    //arrange
    String userName = "user_x";
    Mockito.when(ua.getGroupNamesForUserName(userName)).thenReturn(groups);
    Set<Team> expectedTeams = new HashSet<Team>(2);
    expectedTeams.add(catroid);
    expectedTeams.add(html5);

    Set<Team> teams = service.getTeamsOfUser(userName);

    //assert
    Assert.assertEquals(expectedTeams, teams);
  }

  @Test
  public void testGetCoordinatorTeamsOfUser() throws Exception
  {
    //arrange
    String userName = "user_x";
    Mockito.when(service.getCoordinatorTeamsOfUser(userName)).thenReturn();
    Set<Team> expectedTeams = new HashSet<Team>(1);
    expectedTeams.add(html5);

    //act
    Set<Team> teams = service.getCoordinatorTeamsOfUser(userName);

    //assert
    Assert.assertEquals(expectedTeams, teams);
  }
  */

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
}
