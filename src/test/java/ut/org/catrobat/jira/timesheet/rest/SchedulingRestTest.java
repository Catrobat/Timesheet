package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.mail.queue.MailQueue;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.rest.json.JsonScheduling;
import org.catrobat.jira.timesheet.services.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.rest.SchedulingRest;
import org.catrobat.jira.timesheet.scheduling.TimesheetScheduler;
import org.catrobat.jira.timesheet.services.*;
import org.catrobat.jira.timesheet.services.impl.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest({ComponentAccessor.class, SchedulingRest.class})
public class SchedulingRestTest {

    private SchedulingRest schedulingRestMock;
    private ConfigService configServiceMock;
    private PermissionService permissionServiceMock;
    private TimesheetService timesheetServiceMock;
    private HttpServletRequest httpRequest;
    private EntityManager entityManager;
    private CategoryServiceImpl categoryService;
    private TeamServiceImpl teamService;
    private TimesheetEntryServiceImpl timesheetEntryService;
    private TimesheetServiceImpl timesheetService;
    private SchedulingRest schedulingRest;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        TestActiveObjects ao = new TestActiveObjects(entityManager);

        configServiceMock = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        TimesheetEntryService timesheetEntryServiceMock = mock(TimesheetEntryService.class, RETURNS_DEEP_STUBS);
        timesheetServiceMock = mock(TimesheetService.class, RETURNS_DEEP_STUBS);
        TeamService teamServiceMock = mock(TeamService.class, RETURNS_DEEP_STUBS);
        UserManager userManagerJiraMock = mock(UserManager.class, RETURNS_DEEP_STUBS);
        httpRequest = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        TimesheetScheduler timesheetScheduler = mock(TimesheetScheduler.class, RETURNS_DEEP_STUBS);
        SchedulingService schedulingService = mock(SchedulingService.class, RETURNS_DEEP_STUBS);

        categoryService = new CategoryServiceImpl(ao);
        timesheetService = new TimesheetServiceImpl(ao);
        timesheetEntryService = new TimesheetEntryServiceImpl(ao, timesheetService);
        teamService = new TeamServiceImpl(ao, timesheetEntryService);
        ConfigServiceImpl configService = new ConfigServiceImpl(ao, categoryService, teamService);

        // For some tests we need a mock...
        schedulingRestMock = new SchedulingRest(configServiceMock, permissionServiceMock, timesheetEntryServiceMock,
                timesheetServiceMock, teamServiceMock, categoryService, timesheetScheduler, schedulingService);

        // ... and for some tests we need a real instance of the class
        schedulingRest = new SchedulingRest(configService, permissionServiceMock, timesheetEntryService,
                timesheetService, teamService, categoryService, timesheetScheduler, schedulingService);

        // ... and sometimes you would like to mix them together (see in test method)

        // info: mock static method
        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
    }

    @Test
    public void testSaveAndRetrieveScheduling() {
        when(permissionServiceMock.checkRootPermission()).thenReturn(null);
        Scheduling scheduling = mock(Scheduling.class);
        JsonScheduling jsonScheduling = new JsonScheduling(scheduling);
        schedulingRest.setScheduling(jsonScheduling, httpRequest);
        Response response = schedulingRest.getScheduling(httpRequest);

        JsonScheduling responseJson = (JsonScheduling)response.getEntity();
        assertEquals(jsonScheduling.getInactiveTime(), responseJson.getInactiveTime());
        assertEquals(jsonScheduling.getOfflineTime(), responseJson.getInactiveTime());
        assertEquals(jsonScheduling.getOutOfTime(), responseJson.getOutOfTime());
        assertEquals(jsonScheduling.getRemainingTime(), responseJson.getRemainingTime());
    }

    @Test
    public void testActivityNotification_unauthorized() throws Exception {
        //preparations
        when(permissionServiceMock.checkUserPermission()).thenReturn(mock(Response.class));

        //execution & verifying
        schedulingRestMock.activityNotification(httpRequest);
        Mockito.verify(timesheetServiceMock, never()).all();
    }

    @Test
    public void testActivityNotification_TimesheetEntryIsEmpty() throws Exception {
        timesheetService.add("key 1", "user 1", 450, 450, 900, 200, 0, "master thesis", "", true, true, Timesheet.State.ACTIVE); // master thesis
        timesheetService.add("key 2", "user 2", 450, 0, 450, 450, 0, "bachelor thesis", "", false, false, Timesheet.State.ACTIVE); // disabled
        timesheetService.add("key 3", "user 3", 450, 0, 450, 200, 20, "seminar paper", "", false, true, Timesheet.State.INACTIVE); // inactive

        ApplicationUser user1 = mock(ApplicationUser.class);
        ApplicationUser user2 = mock(ApplicationUser.class);

        when(user1.getName()).thenReturn("MarkusHobisch");
        when(user2.getName()).thenReturn("AdrianSchnedlitz");
        when(ComponentAccessor.getUserManager().getUserByName(user1.getName()).getKey()).thenReturn("key 1");
        when(ComponentAccessor.getUserManager().getUserByName(user2.getName()).getKey()).thenReturn("key 2");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);

        when(permissionServiceMock.checkUserPermission()).thenReturn(null);

        schedulingRest.activityNotification(httpRequest);
    }

    @Test
    public void testActivityNotification_differentKindsOfTimesheets() throws Exception {
        Timesheet timesheet1 = timesheetService.add("key 1", "user 1", 450, 450, 900, 200, 0, "master thesis", "", true, true, Timesheet.State.ACTIVE);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date yesterday = cal.getTime();

        Date today = new Date();

        cal.add(Calendar.DATE, +1);

        Category categoryDrone = categoryService.add("Drone");
        Team droneTeam = teamService.add("Drone Team");

        ApplicationUser user1 = mock(ApplicationUser.class);
        ApplicationUser user2 = mock(ApplicationUser.class);

        when(user1.getName()).thenReturn("MarkusHobisch");
        when(user2.getName()).thenReturn("AdrianSchnedlitz");
        when(ComponentAccessor.getUserManager().getUserByName(user1.getName()).getKey()).thenReturn("key 1");
        when(ComponentAccessor.getUserManager().getUserByName(user2.getName()).getKey()).thenReturn("key 2");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);

        when(permissionServiceMock.checkUserPermission()).thenReturn(null);

        timesheetEntryService.add(timesheet1, yesterday, today, categoryDrone, "testing a lot of things",
                30, droneTeam, false, yesterday, "123456", "MarkusHobisch"); // this should work

        // info: mock private method
        SchedulingRest spy = PowerMockito.spy(schedulingRest);

        // execute your test
        spy.activityNotification(httpRequest);

        // verify your calls
        //PowerMockito.verifyPrivate(spy, never()).invoke("sendMail", Matchers.anyObject());

        timesheetService.remove(timesheet1);
        Timesheet timesheet2 = timesheetService.add("key 1", "user 1", 450, 450, 900, 200, 0, "master thesis", "", true, true, Timesheet.State.INACTIVE); // inactive

        timesheetEntryService.add(timesheet2, yesterday, today, categoryDrone, "testing a lot of things",
                30, droneTeam, false, yesterday, "123456", "MarkusHobisch"); // this should work

        // execute your test
        spy.activityNotification(httpRequest);

        // verify your calls
        //PowerMockito.verifyPrivate(spy, never()).invoke("sendMail", Matchers.anyObject());
    }
}
