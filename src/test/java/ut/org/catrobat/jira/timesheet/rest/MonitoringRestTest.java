package ut.org.catrobat.jira.timesheet.rest;

import org.catrobat.jira.timesheet.activeobjects.Monitoring;
import org.catrobat.jira.timesheet.rest.MonitoringRest;
import org.catrobat.jira.timesheet.rest.json.JsonMonitoring;
import org.catrobat.jira.timesheet.services.MonitoringService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest(MonitoringRest.class)
public class MonitoringRestTest {

    private PermissionService permissionServiceMock;
    private HttpServletRequest httpRequest;
    private MonitoringRest monitoringRest;
    private MonitoringService monitoringService;

    @Before
    public void setUp() throws Exception {

        monitoringService = mock(MonitoringService.class, RETURNS_DEEP_STUBS);

        permissionServiceMock = mock(PermissionService.class, RETURNS_DEEP_STUBS);

        monitoringRest = new MonitoringRest(permissionServiceMock, monitoringService);
    }

    @Test
    public void testSaveAndRetrieveMonitoring() {
        when(permissionServiceMock.checkRootPermission()).thenReturn(null);
        LocalDate begin = LocalDate.now();
        Map.Entry<LocalDate, LocalDate> entry = new AbstractMap.SimpleEntry<>(begin.minusMonths(1), begin);
        when(monitoringService.getCurrentInterval()).thenReturn(entry);
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
