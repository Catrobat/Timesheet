package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.service.ServiceException;
import net.java.ao.DBParam;
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
        int targetHours = 300;
        String displayName = "Test User";
        int targetHoursRemoved = 0;
        int targetHoursCompleted = 150;
        String reason = "Agathe Bauer";
        Date latestEntryDate = new Date();

        service.add(userKey, displayName, targetHoursPractice, targetHours, targetHoursCompleted,
                targetHoursRemoved, lectures, reason, Timesheet.State.ACTIVE);
        Timesheet[] timesheet = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        //Assert
        assertEquals(1, timesheet.length);
        assertEquals(userKey, timesheet[0].getUserKey());
        assertEquals(targetHoursPractice, timesheet[0].getHoursPracticeCompleted());
        assertEquals(targetHours, timesheet[0].getTargetHours());
        assertEquals(targetHoursCompleted, timesheet[0].getHoursCompleted());
        assertEquals(lectures, timesheet[0].getLectures());
        assertTrue(latestEntryDate.getTime() - timesheet[0].getLatestEntryBeginDate().getTime() < 1000);
        assertEquals(Timesheet.State.ACTIVE, timesheet[0].getState());
    }

    @Test
    public void testAll() throws Exception {
        //Arange
        Timesheet sheet = ao.create(Timesheet.class,
            new DBParam("USER_KEY", "USER_001")
        );
        sheet.setHoursPracticeCompleted(targetHoursPractice);
        sheet.setLectures(lectures);
        sheet.setState(Timesheet.State.ACTIVE);
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
        Timesheet sheet = ao.create(Timesheet.class,
            new DBParam("USER_KEY", userKey)
        );
        sheet.setHoursPracticeCompleted(targetHoursPractice);
        sheet.setLectures(lectures);
        sheet.setState(Timesheet.State.ACTIVE);
        sheet.save();
        ao.flushAll();

        //Act
        Timesheet sheet0 = service.getTimesheetByUser("USER_000");

        //Assert
        assertNotNull(sheet0);
        assertEquals(sheet0.getUserKey(), "USER_000");

        //Act
        Timesheet sheet1 = service.getTimesheetByUser("USER_001");

        //Assert
        assertNotNull(sheet1);
        assertEquals(sheet1.getUserKey(), "USER_001");
    }

    @Test(expected = ServiceException.class)
    public void testGetTimesheetByUserNotFound() throws Exception {
        service.getTimesheetByUser("USER_DOES_NOT_EXIST");
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
        refSheet.setHoursPracticeCompleted(newPracticalHours);
        //Assert
        assertTrue(refSheet.getHoursPracticeCompleted() == 300);
    }

    @Test
    public void testUpdateTimesheet() {
        Timesheet timesheet = ao.create(Timesheet.class,
            new DBParam("USER_KEY", "USER_000")
        );

        int targetHoursCompletedOld = 100;
        int targetHoursPracticeOld = 150;
        Date latestEntryDateOld = new Date(100);
        Timesheet.State stateOld = Timesheet.State.AUTO_INACTIVE;

        timesheet.setHoursCompleted(targetHoursCompletedOld);
        timesheet.setHoursPracticeCompleted(targetHoursPracticeOld);
        timesheet.setLatestEntryBeginDate(latestEntryDateOld);
        timesheet.setState(stateOld);
        timesheet.save();

        int targetHoursCompletedNew = 110;
        int targetHoursPracticeNew = 170;
        Date latestEntryDateNew = new Date(200);
        Timesheet.State stateNew = Timesheet.State.ACTIVE;

        Timesheet updatedTimesheet = service.updateTimesheet(timesheet.getID(), targetHoursCompletedNew,
                targetHoursPracticeNew, latestEntryDateNew, stateNew);

        assertEquals(targetHoursCompletedNew, updatedTimesheet.getHoursCompleted());
        assertEquals(targetHoursPracticeNew, updatedTimesheet.getHoursPracticeCompleted());
        assertEquals(latestEntryDateNew, updatedTimesheet.getLatestEntryBeginDate());
        assertEquals(stateNew, updatedTimesheet.getState());
    }

    @Test
    public void testUpdateTimesheetEnableStateOk() throws ServiceException{
        Timesheet timesheet = ao.create(Timesheet.class,
            new DBParam("USER_KEY", "USER_001")
        );
        timesheet.setState(Timesheet.State.ACTIVE);
        timesheet.save();

        Timesheet stillEnabled = service.updateTimesheetEnableState(timesheet.getID(), true);
        assertEquals(Timesheet.State.ACTIVE, stillEnabled.getState());
        Timesheet nowDisabled = service.updateTimesheetEnableState(timesheet.getID(), false);
        assertEquals(Timesheet.State.DISABLED, nowDisabled.getState());
        Timesheet stillDisabled = service.updateTimesheetEnableState(timesheet.getID(), false);
        assertEquals(Timesheet.State.DISABLED, stillDisabled.getState());
        Timesheet enabledAgain = service.updateTimesheetEnableState(timesheet.getID(), true);
        assertEquals(Timesheet.State.ACTIVE, enabledAgain.getState());
    }

    @Test(expected = ServiceException.class)
    public void testUpdateTimesheetEnableStateNotFound() throws ServiceException {
        Timesheet unusedTimesheet = ao.create(Timesheet.class,
            new DBParam("USER_KEY", "USER_001")
        );
        service.updateTimesheetEnableState(unusedTimesheet.getID()+1, false);
    }

    @Test
    public void testEditTimesheets() throws ServiceException {
        int originalHoursPracticeCompleted = 150;
        int originalTargetHours = 200;
        int originalHoursCompleted = 100;
        int originalHoursDeducted = 0;
        String originalReason = "";
        Date originalLatestEntryDate = new Date(100);
        Timesheet.State originalState = Timesheet.State.AUTO_INACTIVE;

        int expectedHoursDeducted = -50;
        String expectedReason = "Reason for deduction";
        Timesheet.State expectedState = Timesheet.State.ACTIVE;

        Timesheet timesheet = ao.create(Timesheet.class, new DBParam("USER_KEY", userKey));
        timesheet.setHoursPracticeCompleted(originalHoursPracticeCompleted);
        timesheet.setTargetHours(originalTargetHours);
        timesheet.setHoursCompleted(originalHoursCompleted);
        timesheet.setHoursDeducted(originalHoursDeducted);
        timesheet.setLectures(lectures);
        timesheet.setReason(originalReason);
        timesheet.setLatestEntryBeginDate(originalLatestEntryDate);
        timesheet.setState(originalState);
        timesheet.save();

        Timesheet updatedTimesheet = service.editTimesheets(userKey, originalHoursPracticeCompleted,
                originalTargetHours, originalHoursCompleted, expectedHoursDeducted, lectures,
                expectedReason, originalLatestEntryDate, expectedState);

        assertNotNull(updatedTimesheet);

        assertEquals(originalHoursPracticeCompleted, updatedTimesheet.getHoursPracticeCompleted());
        assertEquals(originalTargetHours, updatedTimesheet.getTargetHours());
        assertEquals(originalHoursCompleted, updatedTimesheet.getHoursCompleted());
        assertEquals(expectedHoursDeducted, updatedTimesheet.getHoursDeducted());
        assertEquals(lectures, updatedTimesheet.getLectures());
        assertEquals(expectedReason, updatedTimesheet.getReason());
        assertEquals(originalLatestEntryDate, updatedTimesheet.getLatestEntryBeginDate());
        assertEquals(expectedState, updatedTimesheet.getState());
    }

    @Test(expected = ServiceException.class)
    public void testEditTimesheetsNotAvailable() throws ServiceException {
        service.editTimesheets("invalidUserKey", 0, 0,
                0, 0, "", "", null, null);
    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Timesheet.class);

            Timesheet sheet = em.create(Timesheet.class,
                new DBParam("USER_KEY", "USER_000")
            );
        }
    }
}
