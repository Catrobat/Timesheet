package ut.org.catrobat.jira.timesheet.scheduling;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.TimesheetRest;
import org.catrobat.jira.timesheet.scheduling.ActivityVerificationJob;
import org.catrobat.jira.timesheet.services.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.UserKeyService;
import com.atlassian.jira.user.util.UserManager;

import java.time.ZonedDateTime;
import java.util.*;

public class ActivityVerificationJobTest {

    private ActivityVerificationJob activityVerificationJob;
    private SchedulingService schedulingService;
    private Map<String, Object> params;
    private UserManager userManagerJiraMock;

    private Timesheet timesheet1;

    @Before
    @PrepareForTest({ComponentAccessor.class})
    public void setUp() {
        activityVerificationJob = new ActivityVerificationJob();

        TimesheetService sheetService = Mockito.mock(TimesheetService.class);
        TimesheetEntryService entryService = Mockito.mock(TimesheetEntryService.class);
        TeamService teamService = Mockito.mock(TeamService.class);
        CategoryService categoryService = Mockito.mock(CategoryService.class);
        schedulingService = Mockito.mock(SchedulingService.class);
        params = new HashMap<>();
        userManagerJiraMock = mock(UserManager.class, RETURNS_DEEP_STUBS);
        UserKeyService userKeyServiceMock = mock(UserKeyService.class, RETURNS_DEEP_STUBS);
        
        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserKeyService()).thenReturn(userKeyServiceMock);

        params.put("sheetService", sheetService);
        params.put("entryService", entryService);
        params.put("teamService", teamService);
        params.put("categoryService", categoryService);
        params.put("schedulingService", schedulingService);

        timesheet1 = Mockito.mock(Timesheet.class);
        Timesheet timesheet2 = Mockito.mock(Timesheet.class);
        Timesheet timesheet3 = Mockito.mock(Timesheet.class);

        Mockito.when(timesheet1.getLatestEntryBeginDate()).thenReturn(new Date());
        Mockito.when(timesheet2.getLatestEntryBeginDate()).thenReturn(new Date());
        Mockito.when(timesheet3.getLatestEntryBeginDate()).thenReturn(null);

        List<Timesheet> timesheetList = new ArrayList<>();
        timesheetList.add(timesheet1);
        timesheetList.add(timesheet2);
        timesheetList.add(timesheet3);

        TimesheetEntry sheet1entry1 = Mockito.mock(TimesheetEntry.class);
        TimesheetEntry sheet1entry2 = Mockito.mock(TimesheetEntry.class);
        TimesheetEntry sheet2entry1 = Mockito.mock(TimesheetEntry.class);

        Category category = Mockito.mock(Category.class);
        Mockito.when(category.getName()).thenReturn("asdf");

        Mockito.when(sheet1entry1.getCategory()).thenReturn(category);
        Mockito.when(sheet1entry2.getCategory()).thenReturn(category);
        Mockito.when(sheet2entry1.getCategory()).thenReturn(category);

        TimesheetEntry[] sheet1Entries = {sheet1entry1, sheet1entry2};
        TimesheetEntry[] sheet2Entries = {sheet2entry1};
        TimesheetEntry[] sheet3Entries = {};

        Mockito.when(sheetService.all()).thenReturn(timesheetList);
        Mockito.when(entryService.getEntriesBySheet(timesheet1)).thenReturn(sheet1Entries);
        Mockito.when(entryService.getEntriesBySheet(timesheet2)).thenReturn(sheet2Entries);
        Mockito.when(entryService.getEntriesBySheet(timesheet3)).thenReturn(sheet3Entries);

        Set<Team> teamSet = new HashSet<>();
        teamSet.add(Mockito.mock(Team.class));

        Mockito.when(teamService.getTeamsOfUser(timesheet1.getUserKey())).thenReturn(teamSet);
    }

    @Test
    public void testExecute() {
        Mockito.when(timesheet1.getLatestEntryBeginDate()).thenReturn(new Date());
        activityVerificationJob.execute(params);
    }

    public void testAutoInactive() {
        Mockito.when(timesheet1.getState()).thenReturn(Timesheet.State.ACTIVE);
        ZonedDateTime tooOldDateTime = ZonedDateTime.now().minusDays(15);
        Date tooOld = Date.from(tooOldDateTime.toInstant());
        Mockito.when(timesheet1.getLatestEntryBeginDate()).thenReturn(tooOld);
        Mockito.when(schedulingService.isOlderThanInactiveTime(tooOld)).thenReturn(true);

        activityVerificationJob.execute(params);

        Mockito.verify(timesheet1).setState(Timesheet.State.AUTO_INACTIVE);
    }
}
