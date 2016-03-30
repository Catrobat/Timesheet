/*
 * Copyright 2014 Stephan Fellhofer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ut.org.catrobat.jira.timesheet.activeobjects;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
public class ConfigServiceImplTest {

    @SuppressWarnings("UnusedDeclaration")
    private EntityManager entityManager;
    private ActiveObjects ao;
    private CategoryService cs;
    private ConfigService configurationService;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        configurationService = new ConfigServiceImpl(ao, cs);
    }

    @Test
    public void testGetConfiguration() {
        assertEquals(0, ao.find(Config.class).length);
        assertNotNull(configurationService.getConfiguration());
        ao.flushAll();
        configurationService.getConfiguration();
        configurationService.getConfiguration();
        ao.flushAll();
        assertEquals(1, ao.find(Config.class).length);

        Config configuration = configurationService.getConfiguration();
        assertTrue(configuration.getID() != 0);
        assertEquals(0, configuration.getApprovedUsers().length);
        assertEquals(0, configuration.getApprovedGroups().length);
        assertEquals(0, configuration.getTeams().length);
    }



    @Test
    public void testEditMail() {
        Config config = configurationService.getConfiguration();
        assertNull(config.getMailFromName());
        assertNull(config.getMailFrom());
        assertNull(config.getMailSubjectTime());
        assertNull(config.getMailBodyTime());
        assertNull(config.getMailSubjectInactive());
        assertNull(config.getMailBodyInactive());
        assertNull(config.getMailSubjectEntry());
        assertNull(config.getMailBodyEntry());

        String mailDummyText = "Hi {{name}},\n" +
                "Your account has been created and you may login to Jira\n" +
                "(https://jira.catrob.at/) and other resources with the following\n" +
                "credentials:\n" +
                "\n" +
                "Username: {{username}}\n" +
                "Password: {{password}}\n" +
                "\n" +
                "Important: User name is case-sensitive, so please write PrenameSurname,\n" +
                "especially on IRC!\n" +
                "\n" +
                "Best regards,\n" +
                "Your Catrobat-Admins";

        assertNotNull(configurationService.editMail("mailFromName", "mailFrom", "[Subject] Time",
                "[Subject] Inactive", "[Subject] Entry" ,mailDummyText, mailDummyText, mailDummyText));
        ao.flushAll();
        config = configurationService.getConfiguration();
        assertEquals("mailFromName", config.getMailFromName());
        assertEquals("mailFrom", config.getMailFrom());
        assertEquals("[Subject] Time", config.getMailSubjectTime());
        assertEquals(mailDummyText, config.getMailBodyTime());
        assertEquals("[Subject] Inactive", config.getMailSubjectInactive());
        assertEquals(mailDummyText, config.getMailBodyInactive());
        assertEquals("[Subject] Entry", config.getMailSubjectEntry());
        assertEquals(mailDummyText, config.getMailBodyEntry());
    }

    @Test
    public void testAddTeam() {
        Config config;

        //Team1
        List<String> coordinatorListTeam1 = new ArrayList<String>();
        coordinatorListTeam1.add("Ich");
        coordinatorListTeam1.add("und");
        coordinatorListTeam1.add("Du");

        List<String> developerListTeam1 = new ArrayList<String>();
        developerListTeam1.add("Er");
        developerListTeam1.add("Sie");
        developerListTeam1.add("Es");

        List<String> categoryListTeam1 = new ArrayList<String>();
        categoryListTeam1.add("Testing");
        categoryListTeam1.add("Tests");

        //Team 2
        List<String> coordinatorListTeam2 = new ArrayList<String>();
        coordinatorListTeam2.add("A");
        coordinatorListTeam2.add("B");
        coordinatorListTeam2.add("C");

        List<String> developerListTeam2 = new ArrayList<String>();
        developerListTeam2.add("D");
        developerListTeam2.add("E");
        developerListTeam2.add("F");

        List<String> categoryListTeam2 = new ArrayList<String>();
        categoryListTeam2.add("Programming");
        categoryListTeam2.add("Pair-Programming");


        assertNotNull(configurationService.addTeam("team1", coordinatorListTeam1, developerListTeam1, categoryListTeam1));
        assertNotNull(configurationService.addTeam("team 2", coordinatorListTeam2, developerListTeam2, categoryListTeam2));

        ao.flushAll();
        config = configurationService.getConfiguration();
        assertEquals(2, config.getTeams().length);
        assertEquals("team1", config.getTeams()[0].getTeamName());
        assertEquals("team 2", config.getTeams()[1].getTeamName());
        assertEquals(6, config.getTeams()[0].getGroups().length);
        assertEquals(12, ao.find(Group.class).length);
        assertEquals(TeamToGroup.Role.COORDINATOR, config.getTeams()[0].getGroups()[1].getTeamToGroups()[0].getRole());
        assertEquals("und", config.getTeams()[0].getGroups()[1].getGroupName());
        assertEquals(TeamToGroup.Role.DEVELOPER, config.getTeams()[0].getGroups()[4].getTeamToGroups()[0].getRole());
        assertEquals("B", config.getTeams()[1].getGroups()[1].getGroupName());

        assertNotNull(configurationService.addTeam("team3", null, null, null));
        assertNull(configurationService.addTeam(null, null, null, null));
        assertNull(configurationService.addTeam("", null, null, null));
        assertNull(configurationService.addTeam("  ", null, null, null));
    }

    @Test
    public void testEditTeamName() {
        testAddTeam();
        assertEquals(3, configurationService.getConfiguration().getTeams().length);
        assertNotNull(configurationService.editTeamName("team1", "new-team"));
        ao.flushAll();
        assertEquals(3, configurationService.getConfiguration().getTeams().length);
        assertEquals("new-team", configurationService.getConfiguration().getTeams()[0].getTeamName());

        assertNull(configurationService.editTeamName(null, "new-name"));
        assertNull(configurationService.editTeamName("team1", "new-name"));
        ao.flushAll();
        assertEquals(3, configurationService.getConfiguration().getTeams().length);

        assertNull(configurationService.editTeamName("new-team", "team 2"));
        assertNull(configurationService.editTeamName("new-team", null));
        ao.flushAll();
        assertEquals(3, configurationService.getConfiguration().getTeams().length);
    }

    @Test
    public void testRemoveTeam() {
        testAddTeam();
        assertEquals(3, configurationService.getConfiguration().getTeams().length);
        assertNotNull(configurationService.removeTeam("team1"));
        ao.flushAll();
        assertEquals(2, configurationService.getConfiguration().getTeams().length);

        assertNull(configurationService.removeTeam(null));
        assertNull(configurationService.removeTeam("blob"));
        ao.flushAll();
        assertEquals(2, configurationService.getConfiguration().getTeams().length);
    }

    @Test
    public void testGetGroupsForRole() {
        testAddTeam();
        assertEquals(3, configurationService.getConfiguration().getTeams().length);

        List<String> groupList = configurationService.getGroupsForRole("team1", TeamToGroup.Role.COORDINATOR);
        assertEquals(3, groupList.size());
        assertEquals("Ich", groupList.get(0));
        assertEquals("und", groupList.get(1));
        assertEquals("Du", groupList.get(2));
    }

    @Test
    public void testIsGroupApproved() {
        // should return true when no group and no user is defined (or you'll get locked out)
        assertTrue(configurationService.isGroupApproved("blob"));
        assertTrue(configurationService.isGroupApproved(null));
        assertTrue(configurationService.isGroupApproved("  "));

        configurationService.addApprovedUser("blub", "USER_KEY_1");
        ao.flushAll();
        assertFalse(configurationService.isGroupApproved("blob"));
        assertFalse(configurationService.isGroupApproved(null));
        assertFalse(configurationService.isGroupApproved("  "));

        assertEquals(0, ao.find(ApprovedGroup.class).length);
        assertNotNull(configurationService.addApprovedGroup("blob"));
        ao.flushAll();

        assertTrue(configurationService.isGroupApproved("blob"));
        assertTrue(configurationService.isGroupApproved("BlOb"));
        assertTrue(configurationService.isGroupApproved(" BLOB  "));
        assertFalse(configurationService.isGroupApproved(null));
        assertFalse(configurationService.isGroupApproved(""));
        assertFalse(configurationService.isGroupApproved("  "));
        assertFalse(configurationService.isGroupApproved("blab"));
    }

    @Test
    public void testAddApprovedGroup() {
        assertEquals(0, ao.find(ApprovedGroup.class).length);
        assertNotNull(configurationService.addApprovedGroup("blob"));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedGroup.class).length);

        assertNotNull(configurationService.addApprovedGroup("blob"));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedGroup.class).length);

        assertNull(configurationService.addApprovedGroup(null));
        assertNull(configurationService.addApprovedGroup(""));
        assertNull(configurationService.addApprovedGroup("  "));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedGroup.class).length);
    }

    @Test
    public void testClearApprovedGroups() {
        assertNotNull(configurationService.addApprovedGroup("1"));
        assertNotNull(configurationService.addApprovedGroup("2"));
        assertNotNull(configurationService.addApprovedGroup("3"));
        assertNotNull(configurationService.addApprovedGroup("4"));
        ao.flushAll();
        assertEquals(4, ao.find(ApprovedGroup.class).length);

        configurationService.clearApprovedGroups();
        ao.flushAll();
        assertEquals(0, ao.find(ApprovedGroup.class).length);
    }

    @Test
    public void testRemoveApprovedGroup() {
        assertNotNull(configurationService.addApprovedGroup("blob"));
        assertNotNull(configurationService.addApprovedGroup("BLAB"));
        ao.flushAll();
        assertEquals(2, ao.find(ApprovedGroup.class).length);

        assertNotNull(configurationService.removeApprovedGroup(" BLOB   "));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedGroup.class).length);

        assertNull(configurationService.removeApprovedGroup(null));
        assertNull(configurationService.removeApprovedGroup("  "));
        assertNull(configurationService.removeApprovedGroup(""));
        assertNull(configurationService.removeApprovedGroup("blob"));
        assertNull(configurationService.removeApprovedGroup("blah"));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedGroup.class).length);

        assertNotNull(configurationService.removeApprovedGroup("blab"));
        ao.flushAll();
        assertEquals(0, ao.find(ApprovedGroup.class).length);
    }

    @Test
    public void testIsUserApproved() {
        // should return false when no user is defined
        assertFalse(configurationService.isUserApproved("blob"));
        assertFalse(configurationService.isUserApproved(null));
        assertFalse(configurationService.isUserApproved("  "));

        assertEquals(0, ao.find(ApprovedUser.class).length);
        assertNotNull(configurationService.addApprovedUser("blob", "blob"));
        ao.flushAll();

        assertTrue(configurationService.isUserApproved("blob"));
        assertTrue(configurationService.isUserApproved("BlOb"));
        assertTrue(configurationService.isUserApproved(" BLOB  "));
        assertFalse(configurationService.isUserApproved(null));
        assertFalse(configurationService.isUserApproved(""));
        assertFalse(configurationService.isUserApproved("  "));
        assertFalse(configurationService.isUserApproved("blab"));
    }

    @Test
    public void testAddApprovedUser() {
        assertEquals(0, ao.find(ApprovedUser.class).length);
        assertNotNull(configurationService.addApprovedUser("blob", "USER_KEY_1"));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedUser.class).length);

        assertNotNull(configurationService.addApprovedUser("blob", "USER_KEY_1"));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedUser.class).length);

        assertNull(configurationService.addApprovedUser(null, null));
        assertNull(configurationService.addApprovedUser("", ""));
        assertNull(configurationService.addApprovedUser("  ", "  "));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedUser.class).length);
    }

    @Test
    public void testClearApprovedUsers() {
        assertNotNull(configurationService.addApprovedUser("1", "1"));
        assertNotNull(configurationService.addApprovedUser("2", "2"));
        assertNotNull(configurationService.addApprovedUser("3", "3"));
        assertNotNull(configurationService.addApprovedUser("4", "4"));
        ao.flushAll();
        assertEquals(4, ao.find(ApprovedUser.class).length);

        configurationService.clearApprovedUsers();
        ao.flushAll();
        assertEquals(0, ao.find(ApprovedUser.class).length);
    }

    @Test
    public void testRemoveApprovedUser() {
        assertNotNull(configurationService.addApprovedUser("blob", "blob"));
        assertNotNull(configurationService.addApprovedUser("BLAB", "BLAB"));
        ao.flushAll();
        assertEquals(2, ao.find(ApprovedUser.class).length);

        assertNotNull(configurationService.removeApprovedUser(" BLOB   "));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedUser.class).length);

        assertNull(configurationService.removeApprovedUser(null));
        assertNull(configurationService.removeApprovedUser("  "));
        assertNull(configurationService.removeApprovedUser(""));
        assertNull(configurationService.removeApprovedUser("blob"));
        assertNull(configurationService.removeApprovedUser("blah"));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedUser.class).length);

        assertNotNull(configurationService.removeApprovedUser("blab"));
        ao.flushAll();
        assertEquals(0, ao.find(ApprovedUser.class).length);
    }
}
