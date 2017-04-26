package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Scheduling;
import org.catrobat.jira.timesheet.services.SchedulingService;
import org.catrobat.jira.timesheet.services.impl.SchedulingServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(SchedulingServiceImplTest.MyDatabaseUpdater.class)
public class SchedulingServiceImplTest {

    private EntityManager entityManager;
    private SchedulingService schedulingService;

    @Before
    public void setUp() {
        assertNotNull(entityManager);
        ActiveObjects ao = new TestActiveObjects(entityManager);
        schedulingService = new SchedulingServiceImpl(ao);
    }

    @Test
    public void testSaveAndRetrieveScheduling() {
        schedulingService.setScheduling(1, 2, 3, 4);
        Scheduling scheduling = schedulingService.getScheduling();
        assertEquals(1, scheduling.getInactiveTime());
        assertEquals(2, scheduling.getOfflineTime());
        assertEquals(3, scheduling.getRemainingTime());
        assertEquals(4, scheduling.getOutOfTime());
    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Scheduling.class);
        }
    }
}
