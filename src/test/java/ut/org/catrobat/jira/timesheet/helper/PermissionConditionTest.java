package ut.org.catrobat.jira.timesheet.helper;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.helper.MasterThesisTimesheetPermissionCondition;
import org.catrobat.jira.timesheet.helper.PluginPermissionCondition;
import org.catrobat.jira.timesheet.helper.TimesheetPermissionCondition;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentAccessor.class)
public class PermissionConditionTest {

    private MasterThesisTimesheetPermissionCondition masterThesisTimesheetPermissionCondition;
    private PluginPermissionCondition pluginPermissionCondition;
    private TimesheetPermissionCondition timesheetPermissionCondition;
    private ApplicationUser bachelorStudent;
    private ApplicationUser masterStudent;
    private ApplicationUser admin;
    private JiraHelper jiraHelper;

    @Before
    public void setUp() {
        GlobalPermissionManager globalPermissionManager = mock(GlobalPermissionManager.class);
        PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.checkUserPermission()).thenReturn(null);
        Response response = mock(Response.class);
        when(permissionService.checkRootPermission()).thenReturn(response);

        bachelorStudent = mock(ApplicationUser.class);
        masterStudent = mock(ApplicationUser.class);

        when(permissionService.isUserEligibleForTimesheet(bachelorStudent)).thenReturn(true);
        when(permissionService.isUserEligibleForTimesheet(masterStudent)).thenReturn(true);

        jiraHelper = mock(JiraHelper.class);

        GroupManager groupManager = mock(GroupManager.class);
        PowerMockito.mockStatic(ComponentAccessor.class);
        PowerMockito.when(ComponentAccessor.getGroupManager()).thenReturn(groupManager);
        when(groupManager.isUserInGroup(bachelorStudent, "Master-Students")).thenReturn(false);
        when(groupManager.isUserInGroup(masterStudent, "Master-Students")).thenReturn(true);


        masterThesisTimesheetPermissionCondition =
                new MasterThesisTimesheetPermissionCondition(globalPermissionManager, permissionService);
        pluginPermissionCondition = new PluginPermissionCondition(globalPermissionManager, permissionService);
        timesheetPermissionCondition = new TimesheetPermissionCondition(globalPermissionManager, permissionService);
    }

    @Test
    public void testMasterThesisCondition() {
        boolean display = masterThesisTimesheetPermissionCondition.shouldDisplay(masterStudent, jiraHelper);
        assertTrue(display);
        boolean dontDisplay = masterThesisTimesheetPermissionCondition.shouldDisplay(bachelorStudent, jiraHelper);
        assertFalse(dontDisplay);
    }

    @Test
    public void testPluginPermissionCondition() {
        boolean dontDisplay = pluginPermissionCondition.shouldDisplay(bachelorStudent, jiraHelper);
        assertFalse(dontDisplay);
        dontDisplay = pluginPermissionCondition.shouldDisplay(masterStudent, jiraHelper);
        assertFalse(dontDisplay);
//        boolean display = pluginPermissionCondition.shouldDisplay(admin, jiraHelper);
//        assertTrue(display);
    }

    @Test
    public void testTimesheetPermissionCondition() {
        boolean display = timesheetPermissionCondition.shouldDisplay(bachelorStudent, jiraHelper);
        assertTrue(display);
        display = timesheetPermissionCondition.shouldDisplay(masterStudent, jiraHelper);
        assertTrue(display);
    }
}
