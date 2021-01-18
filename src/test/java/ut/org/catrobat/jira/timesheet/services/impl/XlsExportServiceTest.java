package ut.org.catrobat.jira.timesheet.services.impl;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.XlsxExportService;
import org.catrobat.jira.timesheet.services.impl.XlsxExportServiceImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class XlsExportServiceTest {

    private XlsxExportService xlsxExportService;

    @Before
    public void setUp() {
        xlsxExportService = new XlsxExportServiceImpl();
    }

    @Test
    public void testExportTimesheets() {
        Timesheet timesheetA = mock(Timesheet.class);
        Timesheet timesheetB = mock(Timesheet.class);

        TimesheetEntry timesheetEntry = mock(TimesheetEntry.class);
        when(timesheetEntry.getInactiveEndDate()).thenReturn(new Date());
        when(timesheetEntry.getBeginDate()).thenReturn(new Date());
        when(timesheetEntry.getEndDate()).thenReturn(new Date());
        when(timesheetEntry.getCategory()).thenReturn(mock(Category.class));
        when(timesheetEntry.getTeam()).thenReturn(mock(Team.class));
        when(timesheetEntry.getTimeSheet()).thenReturn(timesheetA);

        when(timesheetA.getEntries()).thenReturn(new TimesheetEntry[] { timesheetEntry, timesheetEntry });
        when(timesheetB.getEntries()).thenReturn(new TimesheetEntry[] { timesheetEntry });

        Workbook workbook = xlsxExportService.exportTimesheets(Arrays.asList(timesheetA, timesheetB));
        assertEquals(2, workbook.getNumberOfSheets());

        Sheet timesheets = workbook.getSheet(XlsxExportServiceImpl.TIMESHEET_WORKBOOK_NAME);
        assertEquals(2, timesheets.getLastRowNum());

        Sheet timesheetEntries = workbook.getSheet(XlsxExportServiceImpl.TIMESHEET_ENTRIES_WORKBOOK_NAME);
        assertEquals(3, timesheetEntries.getLastRowNum());
    }
}
