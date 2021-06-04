package ut.org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.osgi.test.Application;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.TimesheetRest;
import org.catrobat.jira.timesheet.scheduling.ActivityVerificationJob;
import org.catrobat.jira.timesheet.services.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.UserKeyService;
import com.atlassian.jira.user.util.UserManager;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.ZonedDateTime;
import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class ActivityVerificationJobTest {

    private ActivityVerificationJob activityVerificationJob;
    private TimesheetEntryService entryService;
    private SchedulingService schedulingService;
    private PermissionService permissionService;
    private Map<String, Object> params;
    private UserManager userManagerJiraMock;

    private ApplicationUser max;
    private ApplicationUser moritz;
    private ApplicationUser fritz;

    private Timesheet timesheet1;

    @Before
    public void setUp() {
        activityVerificationJob = new ActivityVerificationJob();

        TimesheetService sheetService = Mockito.mock(TimesheetService.class);
        entryService = Mockito.mock(TimesheetEntryService.class);
        TeamService teamService = Mockito.mock(TeamService.class);
        CategoryService categoryService = Mockito.mock(CategoryService.class);
        schedulingService = Mockito.mock(SchedulingService.class);
        permissionService = Mockito.mock(PermissionService.class);
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
        params.put("permissionService", permissionService);

        timesheet1 = Mockito.mock(Timesheet.class);
        Timesheet timesheet2 = Mockito.mock(Timesheet.class);
        Timesheet timesheet3 = Mockito.mock(Timesheet.class);

        Mockito.when(timesheet1.getLatestEntryBeginDate()).thenReturn(new Date());
        Mockito.when(timesheet2.getLatestEntryBeginDate()).thenReturn(new Date());
        Mockito.when(timesheet3.getLatestEntryBeginDate()).thenReturn(null);

        Mockito.when(timesheet1.getState()).thenReturn(Timesheet.State.ACTIVE);
        Mockito.when(timesheet2.getState()).thenReturn(Timesheet.State.ACTIVE);
        Mockito.when(timesheet3.getState()).thenReturn(Timesheet.State.DISABLED);

        List<Timesheet> timesheetList = new ArrayList<>();
        timesheetList.add(timesheet1);
        timesheetList.add(timesheet2);
        timesheetList.add(timesheet3);

        TimesheetEntry sheet1entry1 = Mockito.mock(TimesheetEntry.class);
        TimesheetEntry sheet1entry2 = Mockito.mock(TimesheetEntry.class);
        TimesheetEntry sheet2entry1 = Mockito.mock(TimesheetEntry.class);

        Team team = mock(Team.class);
        Category category = Mockito.mock(Category.class);
        Mockito.when(category.getName()).thenReturn("asdf");

        Mockito.when(sheet1entry1.getCategory()).thenReturn(category);
        Mockito.when(sheet1entry2.getCategory()).thenReturn(category);
        Mockito.when(sheet2entry1.getCategory()).thenReturn(category);

        Mockito.when(sheet1entry1.getTeam()).thenReturn(team);

        when(entryService.getLatestEntry(timesheet1)).thenReturn(sheet1entry1);

        TimesheetEntry[] sheet1Entries = {sheet1entry1, sheet1entry2};
        TimesheetEntry[] sheet2Entries = {sheet2entry1};
        TimesheetEntry[] sheet3Entries = {};

        Mockito.when(sheetService.all()).thenReturn(timesheetList);
        Mockito.when(timesheet1.getEntries()).thenReturn(sheet1Entries);
        Mockito.when(timesheet2.getEntries()).thenReturn(sheet2Entries);
        Mockito.when(timesheet3.getEntries()).thenReturn(sheet3Entries);

        max = mock(ApplicationUser.class);
        when(userManagerJiraMock.getUserByKey("max")).thenReturn(max);
        when(timesheet1.getUserKey()).thenReturn("max");
        when(max.getDisplayName()).thenReturn("max");

        moritz = mock(ApplicationUser.class);
        when(userManagerJiraMock.getUserByKey("moritz")).thenReturn(moritz);
        when(timesheet2.getUserKey()).thenReturn("moritz");
        when(max.getDisplayName()).thenReturn("moritz");

        fritz = mock(ApplicationUser.class);
        when(userManagerJiraMock.getUserByKey("fritz")).thenReturn(fritz);
        when(timesheet2.getUserKey()).thenReturn("fritz");
        when(max.getDisplayName()).thenReturn("fritz");

        Set<Team> teamSet = new HashSet<>();
        teamSet.add(Mockito.mock(Team.class));

        Mockito.when(teamService.getTeamsOfUser(timesheet1.getUserKey())).thenReturn(teamSet);
    }

    @Test
    public void testAutoInactive() {
        ZonedDateTime tooOldDateTime = ZonedDateTime.now().minusDays(15);
        Date tooOld = Date.from(tooOldDateTime.toInstant());

        Mockito.when(timesheet1.getLatestEntryBeginDate()).thenReturn(tooOld);
        Mockito.when(schedulingService.isOlderThanInactiveTime(tooOld)).thenReturn(true);
        when(permissionService.isUserInUserGroupDisabled(max)).thenReturn(false);
        when(permissionService.isUserInUserGroupDisabled(moritz)).thenReturn(false);
        when(permissionService.isUserInUserGroupDisabled(fritz)).thenReturn(true);
        activityVerificationJob.execute(params);

        Mockito.verify(timesheet1).setState(Timesheet.State.AUTO_INACTIVE);
    }
}
