package ut.org.catrobat.jira.timesheet.activeobjects;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.service.ServiceException;
import net.java.ao.DBParam;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.activeobjects.impl.TimesheetImpl;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.services.impl.TimesheetEntryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetServiceImpl;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ut.org.catrobat.jira.timesheet.services.impl.TimesheetEntryServiceImplTest;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TimesheetEntryServiceImplTest.MyDatabaseUpdater.class)

public class TimesheetImplTest {

    private TimesheetEntryService service;
    private TimesheetService timesheetService;
    private EntityManager entityManager;
    private ActiveObjects ao;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        timesheetService = new TimesheetServiceImpl(ao);
        service = new TimesheetEntryServiceImpl(ao, timesheetService);
    }

    private Category createTestCategory() {
        Category category = ao.create(Category.class,
                new DBParam("NAME", "testCategory")
        );

        return category;
    }

    private Team createTestTeam() {
        Team team = ao.create(Team.class,
                new DBParam("TEAM_NAME", "testTeam")
        );

        return team;
    }

    private Timesheet createTestTimesheet() {
        Timesheet timesheet = ao.create(Timesheet.class,
                new DBParam("USER_KEY", "testTimesheet")
        );

        return timesheet;
    }

    @Test
    public void testFirstEntryNoEntries() {
        Timesheet timesheetMock = mock(Timesheet.class);
        TimesheetImpl timesheet = new TimesheetImpl(timesheetMock);

        assertNull(timesheet.firstEntry());
    }

    @Test
    public void testFirstEntry() {
        TimesheetEntry entryA = mock(TimesheetEntry.class);
        when(entryA.getBeginDate()).thenReturn(new Date());

        TimesheetEntry entryB = mock(TimesheetEntry.class);
        when(entryB.getBeginDate()).thenReturn(new Date(0));

        TimesheetEntry entryC = mock(TimesheetEntry.class);
        when(entryC.getBeginDate()).thenReturn(new Date(1000));

        Timesheet timesheetMock = mock(Timesheet.class);
        when(timesheetMock.getEntries()).thenReturn(new TimesheetEntry[] {entryA, entryB, entryC});
        TimesheetImpl timesheet = new TimesheetImpl(timesheetMock);

        TimesheetEntry firstEntry = timesheet.firstEntry();
        assertEquals(entryB, firstEntry);
    }
    @Test
    public void testGetTotalHours() {
        float compareValue = 70 / 60.0f;

        TimesheetEntry entryA = mock(TimesheetEntry.class);
        when(entryA.getDurationMinutes()).thenReturn(20);

        TimesheetEntry entryB = mock(TimesheetEntry.class);
        when(entryB.getDurationMinutes()).thenReturn(30);

        TimesheetEntry entryC = mock(TimesheetEntry.class);
        when(entryC.getDurationMinutes()).thenReturn(25);
        when(entryC.getPauseMinutes()).thenReturn(5);

        Timesheet timesheetMock = mock(Timesheet.class);
        when(timesheetMock.getEntries()).thenReturn(new TimesheetEntry[] {entryA, entryB, entryC});

        TimesheetImpl timesheet = new TimesheetImpl(timesheetMock);

        int totalHours = (int) timesheet.calculateTotalHours();
        // Temporary workaround with int till getHours is calculated in float

        assertEquals((Float.compare((float) totalHours, (float) (int) compareValue) == 0), true);

    }

    @Test
    public void testGetTotalHoursZero() {
        float compareValue = 0;
        TimesheetEntry entryA = mock(TimesheetEntry.class);
        when(entryA.getDurationMinutes()).thenReturn(0);

        Timesheet timesheetMock = mock(Timesheet.class);
        when(timesheetMock.getEntries()).thenReturn(new TimesheetEntry[] {entryA});

        TimesheetImpl timesheet = new TimesheetImpl(timesheetMock);

        int totalHours = timesheet.calculateTotalHours();
        assertEquals((Float.compare(totalHours, compareValue) == 0), true);

    }

    @Test
    public void testGetTotalHoursNull() {
        float compareValue = 0;
        Timesheet timesheetMock = mock(Timesheet.class);
        TimesheetImpl timesheet = new TimesheetImpl(timesheetMock);

        float totalHours = timesheet.calculateTotalHours();
        assertEquals((Float.compare(totalHours, compareValue) == 0), true);

    }

    @Test
    public void testUpdateHoursAllPeriodsEmpty() {
        Timesheet sheet = createTestTimesheet();

        sheet.updateHoursAllPeriods(null, null, null, null);

        assertEquals(0, sheet.getHoursCompleted());
        assertEquals(0, sheet.getHoursLastMonth());
        assertEquals(0, sheet.getHoursLastHalfYear());
        assertEquals(0, sheet.getHoursCurrentPeriod());
        assertEquals(0, sheet.getHoursLastPeriod());
    }

    @Test
    public void testUpdateHoursAllPeriods() throws ServiceException {
        Timesheet sheet = createTestTimesheet();
        Category category = createTestCategory();
        Team team = createTestTeam();

        Date inactiveEnd = Date.from(ZonedDateTime.now().minusMonths(6).toInstant());

        String desc = "Debugged this thingy...";
        int pause = 0;
        boolean isGoogleDocImport = false;
        String jiraTicketID = "ATLDEV-287";
        String pairProgrammingUserName = "TestUser";
        boolean teamroom = false;

        Date begin = Date.from(ZonedDateTime.now().toInstant());
        Date end = Date.from(ZonedDateTime.now().plusHours(1).toInstant());

        service.add(sheet, begin, end, category, desc, pause, team, isGoogleDocImport, inactiveEnd, jiraTicketID, pairProgrammingUserName, teamroom);

        begin = Date.from(ZonedDateTime.now().minusMonths(1).toInstant());
        end = Date.from(ZonedDateTime.now().minusMonths(1).plusHours(2).toInstant());

        service.add(sheet, begin, end, category, desc, pause, team, isGoogleDocImport, inactiveEnd, jiraTicketID, pairProgrammingUserName, teamroom);

        begin = Date.from(ZonedDateTime.now().minusMonths(2).toInstant());
        end = Date.from(ZonedDateTime.now().minusMonths(2).plusHours(3).toInstant());

        service.add(sheet, begin, end, category, desc, pause, team, isGoogleDocImport, inactiveEnd, jiraTicketID, pairProgrammingUserName, teamroom);

        LocalDate cur_period_end = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);
        LocalDate cur_period_begin = cur_period_end.withDayOfMonth(1);
        LocalDate last_period_end = cur_period_begin.minusDays(1);
        LocalDate last_period_begin = last_period_end.withDayOfMonth(1);

        sheet.updateHoursAllPeriods(cur_period_begin, cur_period_end, last_period_begin, last_period_end);

        assertEquals(6, sheet.getHoursCompleted());
        assertEquals(1, sheet.getHoursLastMonth());
        assertEquals(6, sheet.getHoursLastHalfYear());
        assertEquals(1, sheet.getHoursCurrentPeriod());
        assertEquals(2, sheet.getHoursLastPeriod());
    }

}
