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
}
