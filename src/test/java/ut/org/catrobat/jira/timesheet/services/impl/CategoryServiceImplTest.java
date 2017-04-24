package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import net.java.ao.DBParam;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.CategoryToTeam;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.impl.CategoryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(CategoryServiceImplTest.MyDatabaseUpdater.class)
public class CategoryServiceImplTest {

    private CategoryService categoryService;
    private ActiveObjects ao;
    private EntityManager entityManager;

    private int specialCategoryCount = SpecialCategories.AllSpecialCategories.size();

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        categoryService = new CategoryServiceImpl(ao);
    }

    @Test
    public void testGetCategoryByIDOk() {
        Category category = ao.create(Category.class,
            new DBParam("NAME", "test")
        );
        Category retrievedCategory = categoryService.getCategoryByID(category.getID());
        assertEquals(category, retrievedCategory);
    }

    @Test
    public void testGetCategoryByIDNull() {
        Category retrievedCategory = categoryService.getCategoryByID(-1);
        assertEquals(null, retrievedCategory);
    }

    @Test
    public void testIsPairProgrammingCategory() {
        Category pairProg = ao.create(Category.class,
            new DBParam("NAME", "Pair Programming")
        );
        Category refactorPP = ao.create(Category.class,
            new DBParam("NAME", "Refactoring (PP)")
        );
        Category notPair = ao.create(Category.class,
            new DBParam("NAME", "Some Category")
        );

        assertTrue(categoryService.isPairProgrammingCategory(pairProg));
        assertTrue(categoryService.isPairProgrammingCategory(refactorPP));
        assertFalse(categoryService.isPairProgrammingCategory(notPair));
    }

    @Test
    public void testGetCategoryByNameOk() throws Exception {
        String category1 = "Test1";
        String category2 = "Test2";

        categoryService.add(category1);
        categoryService.add(category2);

        Category receivedCategory1 = categoryService.getCategoryByName(category1);
        Category receivedCategory2 = categoryService.getCategoryByName(category2);

        Assert.assertEquals(category1, receivedCategory1.getName());
        Assert.assertEquals(category2, receivedCategory2.getName());
    }

    @Test
    public void testGetCategoryByNameNotOk() throws Exception {
        categoryService.add("Something");

        Category receivedCategory3 = categoryService.getCategoryByName("notExistent");

        Assert.assertNull(receivedCategory3);
    }

    @Test
    public void testGetAllCategories() throws Exception {
        List<Category> receivedCategories = categoryService.all();
        Assert.assertEquals(2+specialCategoryCount, receivedCategories.size());
    }

    @Test
    public void testAddCategory() throws Exception {
        int sizeBefore = categoryService.all().size();

        String category = "Test";

        Category insertedCategory = categoryService.add(category);
        Assert.assertEquals(category, insertedCategory.getName());

        List<Category> receivedCategories = categoryService.all();
        Assert.assertEquals(1, receivedCategories.size() - sizeBefore);
    }

    @Test
    public void testRemoveCategory() throws Exception {
        String category1 = "Test";

        Category inserted = categoryService.add(category1);

        categoryService.removeCategory(category1);

        Assert.assertFalse(categoryService.all().contains(inserted));
    }

    public static class MyDatabaseUpdater implements DatabaseUpdater {

        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(Category.class);
            em.migrate(CategoryToTeam.class);
            em.migrate(TimesheetEntry.class);
            em.migrate(Team.class);

            Category meeting = em.create(Category.class,
                new DBParam("NAME", "Meeting")
            );

            Category programming = em.create(Category.class,
                new DBParam("NAME", "Programming")
            );

            Team team = em.create(Team.class,
                new DBParam("TEAM_NAME", "Catroid")
            );

            CategoryToTeam categoryToTeam = em.create(CategoryToTeam.class,
                new DBParam("TEAM_ID", team),
                new DBParam("CATEGORY_ID", meeting)
            );

            TimesheetEntry timesheetEntry = em.create(TimesheetEntry.class);
        }
    }
}
