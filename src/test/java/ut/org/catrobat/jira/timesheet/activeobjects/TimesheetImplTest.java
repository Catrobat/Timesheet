package ut.org.catrobat.jira.timesheet.activeobjects;

import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.activeobjects.impl.TimesheetImpl;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TimesheetImplTest {

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

        int totalHours = (int) timesheet.getTotalHours();
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

        int totalHours = timesheet.getTotalHours();
        assertEquals((Float.compare(totalHours, compareValue) == 0), true);

    }

    @Test
    public void testGetTotalHoursNull() {
        float compareValue = 0;
        Timesheet timesheetMock = mock(Timesheet.class);
        TimesheetImpl timesheet = new TimesheetImpl(timesheetMock);

        float totalHours = timesheet.getTotalHours();
        assertEquals((Float.compare(totalHours, compareValue) == 0), true);

    }
}
