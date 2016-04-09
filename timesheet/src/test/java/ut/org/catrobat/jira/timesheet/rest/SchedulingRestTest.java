package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.mail.queue.MailQueue;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.rest.SchedulingRest;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.TimesheetEntryService;
import org.catrobat.jira.timesheet.services.TimesheetService;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest(ComponentAccessor.class)
public class SchedulingRestTest {

    private SchedulingRest schedulingRestMock;
    private com.atlassian.jira.user.util.UserManager userManagerJiraMock;
    private com.atlassian.sal.api.user.UserManager userManagerLDAPMock;
    private ConfigService configServiceMock;
    private PermissionService permissionServiceMock;
    private TimesheetEntryService timesheetEntryServiceMock;
    private TimesheetService timesheetServiceMock;
    private TeamService teamServiceMock;
    private UserUtil userUtilMock;
    private MailQueue mailQueueMock;
    private HttpServletRequest httpRequest;
    private TestActiveObjects ao;
    private EntityManager entityManager;
    private CategoryServiceImpl categoryService;
    private ConfigServiceImpl configService;
    private TeamServiceImpl teamService;
    private PermissionServiceImpl permissionService;
    private TimesheetEntryServiceImpl timesheetEntryService;
    private TimesheetServiceImpl timesheetService;
    private SchedulingRest schedulingRest;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        configServiceMock = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        timesheetEntryServiceMock = mock(TimesheetEntryService.class, RETURNS_DEEP_STUBS);
        timesheetServiceMock = mock(TimesheetService.class, RETURNS_DEEP_STUBS);
        teamServiceMock = mock(TeamService.class, RETURNS_DEEP_STUBS);
        userManagerLDAPMock = mock(com.atlassian.sal.api.user.UserManager.class, RETURNS_DEEP_STUBS);
        userManagerJiraMock = mock(com.atlassian.jira.user.util.UserManager.class, RETURNS_DEEP_STUBS);
        userUtilMock = mock(UserUtil.class, RETURNS_DEEP_STUBS);
        mailQueueMock = mock(MailQueue.class, RETURNS_DEEP_STUBS);
        httpRequest = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);

        categoryService = new CategoryServiceImpl(ao);
        configService = new ConfigServiceImpl(ao, categoryService);
        teamService = new TeamServiceImpl(ao, configService);
        permissionService = new PermissionServiceImpl(userManagerLDAPMock, teamService, configService);
        timesheetEntryService = new TimesheetEntryServiceImpl(ao);
        timesheetService = new TimesheetServiceImpl(ao);

        // For some tests we need a mock...
        schedulingRestMock = new SchedulingRest(configServiceMock, permissionServiceMock, timesheetEntryServiceMock,
                timesheetServiceMock, teamServiceMock, userManagerLDAPMock);

        // ... and for some tests we need a real instance of the class
        schedulingRest = new SchedulingRest(configService, permissionService, timesheetEntryService,
                timesheetService, teamService, userManagerLDAPMock);

        // ... and sometimes you would like to mix them together (see in test method)


        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getUserManager()).thenReturn(userManagerJiraMock);
        PowerMockito.when(ComponentAccessor.getUserUtil()).thenReturn(userUtilMock);
        PowerMockito.when(ComponentAccessor.getMailQueue()).thenReturn(mailQueueMock);
    }

    @Test
    public void testActivityNotification_unauthorized() throws Exception {
        //preparations
        when(permissionServiceMock.checkPermission(httpRequest)).thenReturn(mock(Response.class));

        //execution & verifying
        schedulingRestMock.activityNotification(httpRequest);
        Mockito.verify(timesheetServiceMock, never()).all();
    }

    @Test
    public void testActivityNotification_TimesheetEntryIsEmpty() throws Exception {
        SchedulingRest schedulingRest = new SchedulingRest(configService, permissionServiceMock, timesheetEntryService,
                timesheetService, teamService, userManagerLDAPMock);

        TimesheetServiceImpl timesheetService = new TimesheetServiceImpl(ao);

        timesheetService.add("key 1",450,450,900,200,0,"Masterarbeit","",30,"",true,true,true); // master thesis
        timesheetService.add("key 2",450,0,450,450,0,"Bachelorarbeit","",15,"",true,false,false); // disabled
        timesheetService.add("key 3",450,0,450,200,20,"Seminararbeit","",7.5,"",false,true,false); // inactive

        User user1 = mock(User.class);
        User user2 = mock(User.class);
        Set<User> userSet = new HashSet<User>(Arrays.asList(user1, user2));
        when(ComponentAccessor.getUserManager().getAllUsers()).thenReturn(userSet);

        when(user1.getName()).thenReturn("MarkusHobisch");
        when(user2.getName()).thenReturn("AdrianSchnedlitz");
        when(ComponentAccessor.getUserManager().getUserByName(user1.getName()).getKey()).thenReturn("key 1");
        when(ComponentAccessor.getUserManager().getUserByName(user2.getName()).getKey()).thenReturn("key 2");

        Config config = mock(Config.class);
        when(configServiceMock.getConfiguration()).thenReturn(config);

        when(permissionServiceMock.checkPermission(httpRequest)).thenReturn(null);

        schedulingRest.activityNotification(httpRequest);
    }
}