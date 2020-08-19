package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
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

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Monitoring.class);
        }
    }
}
