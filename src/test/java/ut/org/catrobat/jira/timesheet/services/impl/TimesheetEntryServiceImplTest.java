/*
 * Copyright 2015 Atlassian.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.services.impl.TimesheetEntryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import static org.junit.Assert.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TimesheetEntryServiceImplTest.MyDatabaseUpdater.class)

public class TimesheetEntryServiceImplTest {

    private EntityManager entityManager;
    private TimesheetService timesheetService;
    private TimesheetEntryService service;
    private ActiveObjects ao;

    private static final Date TODAY = new Date();

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        timesheetService = new TimesheetServiceImpl(ao);
        service = new TimesheetEntryServiceImpl(ao, timesheetService);
    }

    @Test
    public void testAdd() throws Exception {
        //Arrange
        long oneHourInMS = 60 * 60 * 1000;
        Timesheet sheet = ao.create(Timesheet.class);
        Category category = ao.create(Category.class);
        Team team = ao.create(Team.class);
        Date begin = new Date();
        Date end = new Date(begin.getTime() + oneHourInMS);
        String desc = "Debugged this thingy...";
        int pause = 0;
        boolean isGoogleDocImport = false;
        String jiraTicketID = "CAT-1530";
        String pairProgrammingUserName = "TestUser";
        

        //Act
        service.add(sheet, begin, end, category, desc, pause, team, isGoogleDocImport, TODAY, TODAY, jiraTicketID,
                pairProgrammingUserName);
        TimesheetEntry[] entries = ao.find(TimesheetEntry.class);

        //Assert
        assertEquals(1, entries.length);
        assertEquals(sheet, entries[0].getTimeSheet());
        assertEquals(category, entries[0].getCategory());
        assertEquals(team, entries[0].getTeam());
        assertEquals(begin, entries[0].getBeginDate());
        assertEquals(end, entries[0].getEndDate());
        assertEquals(desc, entries[0].getDescription());
        assertEquals(pause, entries[0].getPauseMinutes());
    }

    @Test
    public void testGetEntriesBySheet() throws Exception {
        //Arrange
        long oneHourInMS = 60 * 60 * 1000;
        Timesheet sheet = ao.create(Timesheet.class);
        Category category = ao.create(Category.class);
        Team team = ao.create(Team.class);
        Date begin = new Date();
        Date end = new Date(begin.getTime() + oneHourInMS);
        String desc = "Debugged this thingy...";
        int pause = 0;
        boolean isGoogleDocImport = false;
        String jiraTicketID = "CAT-1530";
        String pairProgrammingUserName = "TestUser";
        

        //Act
        service.add(sheet, begin, end, category, desc, pause, team, isGoogleDocImport, TODAY, TODAY, jiraTicketID,
                pairProgrammingUserName);
        TimesheetEntry[] entries = service.getEntriesBySheet(sheet);

        //Assert
        assertEquals(1, entries.length);
        assertEquals(sheet, entries[0].getTimeSheet());
        assertEquals(category, entries[0].getCategory());
        assertEquals(team, entries[0].getTeam());
        assertEquals(begin, entries[0].getBeginDate());
        assertEquals(end, entries[0].getEndDate());
        assertEquals(desc, entries[0].getDescription());
        assertEquals(pause, entries[0].getPauseMinutes());
    }

    @Test
    public void testEditTimesheetEntryByID() throws Exception {
        //Arrange
        long oneHourInMS = 60 * 60 * 1000;
        Timesheet sheet = ao.create(Timesheet.class);
        Category category = ao.create(Category.class);
        Team team = ao.create(Team.class);
        Date begin = new Date();
        Date end = new Date(begin.getTime() + oneHourInMS);
        String desc = "Debugged this thingy...";
        int pause = 0;
        boolean isGoogleDocImport = false;
        String jiraTicketID = "CAT-1530";
        String pairProgrammingUserName = "TestUser";
        

        //Act
        TimesheetEntry newEntry = service.add(sheet, begin, end, category, desc, pause, team, isGoogleDocImport,
                TODAY, TODAY, jiraTicketID, pairProgrammingUserName);

        String newDesc = "Changed Entry Content";
        int newPause = 30;

        TimesheetEntry changedEntry = service.edit(newEntry.getID(), sheet, begin, end, category,
                newDesc, newPause, team, isGoogleDocImport, TODAY, TODAY,
                pairProgrammingUserName, jiraTicketID);

        //Assert
        assertEquals(changedEntry.getDescription(), newDesc);
        assertEquals(changedEntry.getPauseMinutes(), newPause);
    }

    @Test
    public void testDeleteTimesheetEntry() throws Exception {
        //Arrange
        long oneHourInMS = 60 * 60 * 1000;
        Timesheet sheet = ao.create(Timesheet.class);
        Category category = ao.create(Category.class);
        Team team = ao.create(Team.class);
        Date begin = new Date();
        Date end = new Date(begin.getTime() + oneHourInMS);
        String desc = "Debugged this thingy...";
        int pause = 0;
        boolean isGoogleDocImport = false;
        String jiraTicketID = "CAT-1530";
        String pairProgrammingUserName = "TestUser";
        

        //Act
        service.add(sheet, begin, end, category, desc, pause, team, isGoogleDocImport, TODAY, TODAY, jiraTicketID,
                pairProgrammingUserName);
        TimesheetEntry[] entriesBeforeDelete = ao.find(TimesheetEntry.class);

        service.delete(entriesBeforeDelete[0]);
        TimesheetEntry[] entriesAfterDelete = ao.find(TimesheetEntry.class);

        Assert.assertTrue(entriesBeforeDelete.length > entriesAfterDelete.length);
    }

    @Test
    public void testEditTimesheetEntryWithSetter() throws Exception {
        //Arrange
        long oneHourInMS = 60 * 60 * 1000;
        Timesheet sheet = ao.create(Timesheet.class);
        Category category = ao.create(Category.class);
        Team team = ao.create(Team.class);
        Date begin = new Date();
        Date end = new Date(begin.getTime() + oneHourInMS);
        String desc = "Debugged this thingy...";
        int pause = 0;
        boolean isGoogleDocImport = false;
        String jiraTicketID = "CAT-1530";
        String pairProgrammingUserName = "TestUser";
        

        //Act
        service.add(sheet, begin, end, category, desc, pause, team, isGoogleDocImport, TODAY, TODAY, jiraTicketID,
                pairProgrammingUserName);
        Assert.assertNotNull(service.getEntriesBySheet(sheet));

        long newOneHourInMS = 60 * 60 * 1000;
        Date newBegin = new Date();
        Date newEnd = new Date(begin.getTime() + 2 * newOneHourInMS);
        String newDesc = "Changed this thingy...";
        int newPause = 100;

        TimesheetEntry[] changedEntry = service.getEntriesBySheet(sheet);

        changedEntry[0].setBeginDate(newBegin);
        changedEntry[0].setEndDate(newEnd);
        changedEntry[0].setDescription(newDesc);
        changedEntry[0].setPauseMinutes(newPause);

        //Assert
        assertEquals(changedEntry[0].getBeginDate(), newBegin);
        assertEquals(changedEntry[0].getEndDate(), newEnd);
        assertEquals(changedEntry[0].getDescription(), newDesc);
        assertEquals(changedEntry[0].getPauseMinutes(), newPause);
    }

    @Test
    public void testGetTimesheetEntryByID() throws Exception {
        //Arrange
        long oneHourInMS = 60 * 60 * 1000;
        Timesheet sheet = ao.create(Timesheet.class);
        Category category = ao.create(Category.class);
        Team team = ao.create(Team.class);
        Date begin = new Date();
        Date end = new Date(begin.getTime() + oneHourInMS);
        String desc = "Debugged this thingy...";
        int pause = 0;
        boolean isGoogleDocImport = false;
        String jiraTicketID = "CAT-1530";
        String pairProgrammingUserName = "TestUser";

        //Act
        service.add(sheet, begin, end, category, desc, pause, team, isGoogleDocImport, TODAY, TODAY, jiraTicketID,
                pairProgrammingUserName);
        TimesheetEntry[] entryList = service.getEntriesBySheet(sheet);
        TimesheetEntry receivedEntry = service.getEntryByID(sheet.getID());

        //Assert
        Assert.assertEquals(receivedEntry, entryList[0]);
    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Timesheet.class);
            em.migrate(Category.class);
            em.migrate(Team.class);
            em.migrate(TimesheetEntry.class);
        }
    }
}
