package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.jira.component.ComponentAccessor;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Monitoring;
import org.catrobat.jira.timesheet.rest.MonitoringRest;
import org.catrobat.jira.timesheet.rest.SchedulingRest;
import org.catrobat.jira.timesheet.rest.json.JsonMonitoring;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.MonitoringService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.services.impl.CategoryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TeamServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetEntryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.TimesheetServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
@PrepareForTest({ComponentAccessor.class, SchedulingRest.class})
public class MonitoringRestTest {

    private PermissionService permissionServiceMock;
    private HttpServletRequest httpRequest;
    private MonitoringRest monitoringRest;

    @Before
    public void setUp() throws Exception {

        MonitoringService monitoringService = mock(MonitoringService.class, RETURNS_DEEP_STUBS);

        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);

        monitoringRest = new MonitoringRest(permissionServiceMock, monitoringService);

        PowerMockito.mockStatic(ComponentAccessor.class);
    }

    @Test
    public void testSaveAndRetrieveScheduling() {
        when(permissionServiceMock.checkRootPermission()).thenReturn(null);
        Monitoring monitoring = mock(Monitoring.class);
        JsonMonitoring jsonMonitoring = new JsonMonitoring(monitoring);
        Response response = monitoringRest.setMonitoring(jsonMonitoring, httpRequest);
        assertNull(response.getEntity());
        response = monitoringRest.getMonitoring(httpRequest);

        JsonMonitoring responseJson = (JsonMonitoring) response.getEntity();
        assertEquals(jsonMonitoring.getPeriod(), responseJson.getPeriod());
        assertEquals(jsonMonitoring.getRequiredHours(), responseJson.getRequiredHours());
        assertEquals(jsonMonitoring.getExceptions(), responseJson.getExceptions());
    }
}
