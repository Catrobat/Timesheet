package ut.org.catrobat.jira.timesheet.services.impl;

import org.junit.Assert;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.List;

public class CategoryServiceTest {

    private CategoryService categoryService;

    @Before
    public void setUp() throws Exception {
        categoryService = Mockito.mock(CategoryService.class);
    }

    @Test
    public void testGetCategoryByIDOk() throws Exception {
        Category category1 = Mockito.mock(Category.class);
        Mockito.when(category1.getID()).thenReturn(1);
        Mockito.when(category1.getName()).thenReturn("Meeting");

        Category category2 = Mockito.mock(Category.class);
        Mockito.when(category2.getID()).thenReturn(2);
        Mockito.when(category2.getName()).thenReturn("Programming");

        List<Category> categories = new LinkedList<Category>();
        categories.add(category1);
        categories.add(category2);

        Mockito.when(categoryService.getCategoryByID(1)).thenReturn(category1);
        Mockito.when(categoryService.getCategoryByID(2)).thenReturn(category2);

        Category receivedCategory1 = categoryService.getCategoryByID(1);
        Category receivedCategory2 = categoryService.getCategoryByID(2);

        Assert.assertEquals(categories.get(0), receivedCategory1);
        Assert.assertEquals(categories.get(1), receivedCategory2);
    }

    @Test
    public void testGetCategoryByIDNotOk() throws Exception {
        Category category1 = Mockito.mock(Category.class);
        Mockito.when(category1.getID()).thenReturn(1);
        Mockito.when(category1.getName()).thenReturn("Meeting");

        Category category2 = Mockito.mock(Category.class);
        Mockito.when(category2.getID()).thenReturn(2);
        Mockito.when(category2.getName()).thenReturn("Programming");

        List<Category> categories = new LinkedList<Category>();
        categories.add(category1);
        categories.add(category2);

        Mockito.when(categoryService.getCategoryByID(1)).thenReturn(category1);
        Mockito.when(categoryService.getCategoryByID(2)).thenReturn(category2);

        Category receivedCategory3 = categoryService.getCategoryByID(3);

        Assert.assertNull(receivedCategory3);
    }

    @Test
    public void testGetAllCategories() throws Exception {
        Category category1 = Mockito.mock(Category.class);
        Mockito.when(category1.getID()).thenReturn(2);
        Mockito.when(category1.getName()).thenReturn("Meeting");

        Category category2 = Mockito.mock(Category.class);
        Mockito.when(category2.getID()).thenReturn(1);
        Mockito.when(category2.getName()).thenReturn("Programming");

        List<Category> categories = new LinkedList<Category>();
        categories.add(category1);
        categories.add(category2);

        Mockito.when(categoryService.all()).thenReturn(categories);

        List<Category> receivedCategories = categoryService.all();
        Assert.assertEquals(categories, receivedCategories);
    }

    @Test
    public void testAddCategory() throws Exception {
        Category category1 = Mockito.mock(Category.class);
        Mockito.when(category1.getID()).thenReturn(1);
        Mockito.when(category1.getName()).thenReturn("Meeting");

        Category category2 = Mockito.mock(Category.class);
        Mockito.when(category2.getID()).thenReturn(2);
        Mockito.when(category2.getName()).thenReturn("Programming");

        Category category3 = Mockito.mock(Category.class);
        Mockito.when(category2.getID()).thenReturn(3);
        Mockito.when(category2.getName()).thenReturn("Test");

        List<Category> categoriesBeforeAdd = new LinkedList<Category>();
        categoriesBeforeAdd.add(category1);
        categoriesBeforeAdd.add(category2);

        Mockito.when(categoryService.all()).thenReturn(categoriesBeforeAdd);

        List<Category> receivedCategories = categoryService.all();
        Assert.assertEquals(categoriesBeforeAdd, receivedCategories);

        List<Category> categoriesAfterAdd = new LinkedList<Category>();
        categoriesBeforeAdd.add(category1);
        categoriesBeforeAdd.add(category2);
        categoriesBeforeAdd.add(category3);

        Mockito.when(categoryService.add("Test")).thenReturn(category3);

        Category insertedCategory = categoryService.add("Test");
        Assert.assertEquals(category3.getName(), insertedCategory.getName());

        Mockito.when(categoryService.all()).thenReturn(categoriesAfterAdd);

        receivedCategories = categoryService.all();
        Assert.assertEquals(categoriesAfterAdd, receivedCategories);
    }
}
