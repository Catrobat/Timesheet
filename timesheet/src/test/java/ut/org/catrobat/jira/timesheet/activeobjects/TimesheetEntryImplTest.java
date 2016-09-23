package ut.org.catrobat.jira.timesheet.activeobjects;

import com.atlassian.activeobjects.external.ActiveObjects;
import net.java.ao.EntityManager;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.atlassian.activeobjects.test.TestActiveObjects;
import java.text.SimpleDateFormat;
import net.java.ao.Query;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)

public class TimesheetEntryImplTest {

	private EntityManager entityManager;
	private ActiveObjects ao;
	private SimpleDateFormat sdf;
	private TimesheetEntry entry;

	@Before
	public void setUp() throws Exception
	{
		assertNotNull(entityManager);
		ao = new TestActiveObjects(entityManager);
		sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
		TimesheetEntry[] entries = ao.find(
						TimesheetEntry.class,
						"DESCRIPTION = ?", "Master Fixen"
		);
		entry = entries[0];
	}

	@Test
	public void testGetDurationMinutes() throws Exception {

		TimesheetEntry[] entries = ao.find(
						TimesheetEntry.class,
						Query.select().order("DURATION_MINUTES ASC")
		);

		//assert
		assertEquals(entries[0].getDurationMinutes(), 10);
		assertEquals(entries[1].getDurationMinutes(), 50);
	}

	@Test
	public void testSetBeginDate() throws Exception {

		assertEquals(entry.getDurationMinutes(), 10);

		entry.setBeginDate(sdf.parse("02-01-2015 09:30"));
		entry.save();

		assertEquals(entry.getDurationMinutes(), 70);
	}

	@Test
	public void testSetEndDate() throws Exception {

		assertEquals(entry.getDurationMinutes(), 10);

		entry.setEndDate(sdf.parse("02-01-2015 11:00"));
		entry.save();

		assertEquals(entry.getDurationMinutes(), 25);
	}

	@Test
	public void testSetPauseMinutes() throws Exception {

		assertEquals(entry.getDurationMinutes(), 10);

		entry.setPauseMinutes(3);
		entry.save();

		assertEquals(entry.getDurationMinutes(), 12);
	}

	@Test
	public void testChangeDurationMinutesUpdate() throws Exception {

		assertEquals(entry.getDurationMinutes(), 10);

		entry.setBeginDate(sdf.parse("02-01-2015 08:00"));
		entry.setEndDate(sdf.parse("02-01-2015 10:00"));
		entry.setPauseMinutes(0);
		entry.save();

		assertEquals(entry.getDurationMinutes(), 120);
	}
}
