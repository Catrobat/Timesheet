package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import javafx.util.Pair;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Monitoring;
import org.catrobat.jira.timesheet.services.MonitoringService;
import org.catrobat.jira.timesheet.services.impl.MonitoringServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(MonitoringServiceImplTest.MyDatabaseUpdater.class)
public class MonitoringServiceImplTest {

    private EntityManager entityManager;
    private MonitoringService monitoringService;

    @Before
    public void setUp() {
        assertNotNull(entityManager);
        ActiveObjects ao = new TestActiveObjects(entityManager);
        monitoringService = new MonitoringServiceImpl(ao);
    }

    @Test
    public void testSaveAndRetrieveMonitoring() {
        monitoringService.setMonitoring(1, 2, 3);
        Monitoring monitoring = monitoringService.getMonitoring();
        assertEquals(1, monitoring.getPeriod());
        assertEquals(2, monitoring.getRequiredHours());
        assertEquals(3, monitoring.getExceptions());
    }

    @Test
    public void testNegativeValues(){
        monitoringService.setMonitoring(-1,-1,-1);
        Monitoring monitoring = monitoringService.getMonitoring();
        assertEquals(1, monitoring.getPeriod());
        assertEquals(1, monitoring.getRequiredHours());
        assertEquals(1, monitoring.getExceptions());
    }

    @Test
    public void testGetLastIntervalTwoMonths(){
        monitoringService.setMonitoring(2,1,1);
        Pair<LocalDate, LocalDate> interval = monitoringService.getLastInterval();
        assertEquals(interval, new Pair<>(LocalDate.now().withDayOfMonth(1).minusMonths(1), LocalDate.now()));
    }

    @Test
    public void testGetLastIntervalOneMonth(){
        monitoringService.setMonitoring(1,1,1);
        Pair<LocalDate, LocalDate> interval = monitoringService.getLastInterval();
        assertEquals(interval, new Pair<>(LocalDate.now().withDayOfMonth(1), LocalDate.now()));
    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Monitoring.class);
        }
    }
}
