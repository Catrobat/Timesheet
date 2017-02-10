package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.service.ServiceException;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.services.impl.TimesheetServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TimesheetServiceImplTest.MyDatabaseUpdater.class)

public class TimesheetServiceImplTest {

    private final String userKey = "USER_001";
    private final int targetHoursPractice = 150;
    private final int targetHoursTheory = 0;
    private final int targetHours = 300;
    private final int targetHoursCompleted = 150;
    private final Date latestEntryDate = new Date();
    private final String lectures = "Mobile Applications (705.881)";
    private EntityManager entityManager;
    private TimesheetService service;
    private ActiveObjects ao;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        service = new TimesheetServiceImpl(ao);
    }

    @Test
    public void testAdd() throws Exception {
        //Act
        String displayName = "Test User";
        int targetHoursRemoved = 0;
        String reason = "Agathe Bauer";
        service.add(userKey, displayName, targetHoursPractice, targetHoursTheory, targetHours, targetHoursCompleted,
                targetHoursRemoved, lectures, reason, false, true, Timesheet.State.ACTIVE);
        Timesheet[] timesheet = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        //Assert
        assertEquals(1, timesheet.length);
        assertEquals(userKey, timesheet[0].getUserKey());
        assertEquals(targetHoursPractice, timesheet[0].getTargetHoursPractice());
        assertEquals(targetHoursTheory, timesheet[0].getTargetHoursTheory());
        assertEquals(targetHours, timesheet[0].getTargetHours());
        assertEquals(targetHoursCompleted, timesheet[0].getTargetHoursCompleted());
        assertEquals(lectures, timesheet[0].getLectures());
        assertTrue(latestEntryDate.getTime() - timesheet[0].getLatestEntryBeginDate().getTime() < 1000);
        assertEquals(Timesheet.State.ACTIVE, timesheet[0].getState());
        assertEquals(true, timesheet[0].getIsEnabled());
    }

    @Test
    public void testAll() throws Exception {
        //Arange
        Timesheet sheet = ao.create(Timesheet.class);
        sheet.setUserKey(userKey);
        sheet.setTargetHoursPractice(targetHoursPractice);
        sheet.setTargetHoursTheory(targetHoursTheory);
        sheet.setTargetHoursTheory(targetHours);
        sheet.setTargetHoursTheory(targetHoursCompleted);
        sheet.setLectures(lectures);
        sheet.setState(Timesheet.State.ACTIVE);
        sheet.setIsEnabled(true);
        sheet.save();
        ao.flushAll();

        //Act
        List<Timesheet> timesheets = service.all();

        //Assert
        assertEquals(timesheets.size(), 2);
    }

    @Test
    public void testGetTimesheetByUser() throws Exception {
        //Arange
        Timesheet sheet = ao.create(Timesheet.class);
        sheet.setUserKey(userKey);
        sheet.setTargetHoursPractice(targetHoursPractice);
        sheet.setTargetHoursTheory(targetHoursTheory);
        sheet.setTargetHoursTheory(targetHours);
        sheet.setTargetHoursTheory(targetHoursCompleted);
        sheet.setLectures(lectures);
        sheet.setState(Timesheet.State.ACTIVE);
        sheet.setIsEnabled(true);
        sheet.save();
        ao.flushAll();

        //Act
        Timesheet sheet0 = service.getTimesheetByUser("USER_000", false);

        //Assert
        assertNotNull(sheet0);
        assertEquals(sheet0.getUserKey(), "USER_000");

        //Act
        Timesheet sheet1 = service.getTimesheetByUser("USER_001", false);

        //Assert
        assertNotNull(sheet1);
        assertEquals(sheet1.getUserKey(), "USER_001");
    }

    @Test(expected = ServiceException.class)
    public void testGetTimesheetByUserNotFound() throws Exception {
        //Act
        Timesheet missingSheet = service.getTimesheetByUser("USER_DOES_NOT_EXIST", false);
    }

    @Test
    public void testGetTimesheetByID() throws Exception {
        //Act
        Timesheet[] sheets = ao.find(Timesheet.class, "USER_KEY = ?", "USER_000");
        Timesheet refSheet = sheets[0];
        Timesheet checkSheet = service.getTimesheetByID(refSheet.getID());

        //Assert
        assertEquals(refSheet, checkSheet);
    }

    @Test
    public void testGetTimesheetByMissingID() throws Exception {
        //Act
        Timesheet sheet = service.getTimesheetByID(12345);

        //Assert
        assertNull(sheet);
    }

    @Test
    public void testUpdateTimesheetPractialhours() throws Exception {
        //Act
        Timesheet[] sheets = ao.find(Timesheet.class, "USER_KEY = ?", "USER_000");
        Timesheet refSheet = sheets[0];
        Timesheet checkSheet = service.getTimesheetByID(refSheet.getID());

        //Assert
        assertEquals(refSheet, checkSheet);

        //Act
        final int newPracticalHours = 300;
        refSheet.setTargetHoursPractice(newPracticalHours);
        //Assert
        assertTrue(refSheet.getTargetHoursPractice() == 300);
    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Timesheet.class);

            Timesheet sheet = em.create(Timesheet.class);
            sheet.setUserKey("USER_000");
            sheet.save();
        }
    }
}
