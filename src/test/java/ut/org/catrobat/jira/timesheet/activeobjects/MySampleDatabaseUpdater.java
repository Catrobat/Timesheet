package ut.org.catrobat.jira.timesheet.activeobjects;

import net.java.ao.DBParam;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.DatabaseUpdater;
import org.catrobat.jira.timesheet.activeobjects.*;

import java.text.SimpleDateFormat;

public class MySampleDatabaseUpdater implements DatabaseUpdater {

    @Override
    public void update(EntityManager em) throws Exception {
        em.migrate(Config.class);
        em.migrate(TimesheetAdmin.class);
        em.migrate(Group.class);

        em.migrate(Timesheet.class);
        em.migrate(Category.class);
        em.migrate(CategoryToTeam.class);
        em.migrate(Team.class);
        em.migrate(TeamToGroup.class);
        em.migrate(TimesheetEntry.class);

        Timesheet chrisSheet = em.create(Timesheet.class,
            new DBParam("USER_KEY", "chris")
        );

        Timesheet jsonSheet = em.create(Timesheet.class,
            new DBParam("USER_KEY", "joh")
        );

        Team scratchTeam = em.create(Team.class,
            new DBParam("TEAM_NAME", "SCRATCH")
        );

        Team confluenceTeam = em.create(Team.class,
            new DBParam("TEAM_NAME", "CONFLUENCE")
        );

        Team catrobatTeam = em.create(Team.class,
            new DBParam("TEAM_NAME", "CATROBAT")
        );

        Category meetingCategory = em.create(Category.class,
            new DBParam("NAME", "Meeting")
        );

        Category programmingCategory = em.create(Category.class,
            new DBParam("NAME", "Programming")
        );

        CategoryToTeam catHasMee = em.create(CategoryToTeam.class,
            new DBParam("TEAM_ID", catrobatTeam),
            new DBParam("CATEGORY_ID", meetingCategory)
        );

        CategoryToTeam catHasPro = em.create(CategoryToTeam.class,
            new DBParam("TEAM_ID", catrobatTeam),
            new DBParam("CATEGORY_ID", programmingCategory)
        );

        CategoryToTeam conHasPro = em.create(CategoryToTeam.class,
            new DBParam("TEAM_ID", confluenceTeam),
            new DBParam("CATEGORY_ID", programmingCategory)
        );

        CategoryToTeam scrHasMee = em.create(CategoryToTeam.class,
            new DBParam("TEAM_ID", scratchTeam),
            new DBParam("CATEGORY_ID", meetingCategory)
        );

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy hh:mm");

        TimesheetEntry entry1 = em.create(TimesheetEntry.class);
        entry1.setCategory(meetingCategory);
        entry1.setBeginDate(sdf.parse("01-01-2015 09:00"));
        entry1.setEndDate(sdf.parse("01-01-2015 10:00"));
        entry1.setTimeSheet(chrisSheet);
        entry1.setTeam(scratchTeam);
        entry1.setPauseMinutes(10);
        entry1.setDescription("Besprechung: Team Fetcher");
        entry1.save();

        TimesheetEntry entry2 = em.create(TimesheetEntry.class);
        entry2.setCategory(programmingCategory);
        entry2.setBeginDate(sdf.parse("02-01-2015 10:30"));
        entry2.setEndDate(sdf.parse("02-01-2015 10:45"));
        entry2.setPauseMinutes(5);
        entry2.setTimeSheet(jsonSheet);
        entry2.setTeam(catrobatTeam);
        entry2.setDescription("Master Fixen");
        entry2.save();

    }
}
