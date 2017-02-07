/*
 * Copyright 2016 Adrian Schnedlitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.service.ServiceException;
import net.java.ao.Query;
import org.catrobat.jira.timesheet.activeobjects.Category;
import org.catrobat.jira.timesheet.activeobjects.CategoryToTeam;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.CategoryService;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class CategoryServiceImpl implements CategoryService {

    private final ActiveObjects ao;
    private boolean isInitialised = false;

    public CategoryServiceImpl(ActiveObjects ao) {
        this.ao = ao;
    }

    private void init() {
        for (String special : SpecialCategories.AllSpecialCategories) {
            Category[] found = ao.find(Category.class, "NAME = ?", special);
            if (found.length == 0) {
                Category category = ao.create(Category.class);
                category.setName(special);
                category.save();
            }
        }
        isInitialised = true;
    }

    @Override
    public Category getCategoryByID(int id) {
        Category[] found = ao.find(Category.class, "ID = ?", id);
        assert (found.length <= 1);
        return (found.length > 0) ? found[0] : null;
    }

    @Override
    public Category getCategoryByName(String name) {
        if (SpecialCategories.AllSpecialCategories.contains(name)) {
            initIfNotAlready();
        }
        Category[] found = ao.find(Category.class, "NAME = ?", name);
        return (found.length == 1) ? found[0] : null;
    }

    @Override
    public List<Category> all() {
        initIfNotAlready();
        return newArrayList(ao.find(Category.class, Query.select().order("NAME ASC")));
    }

    private void initIfNotAlready() {
        if (!isInitialised) {
            init();
        }
    }

    @Override
    public Category add(String name) throws ServiceException {
        Category[] found = ao.find(Category.class, "NAME = ?", name);
        if (found.length > 0) {
            throw new ServiceException("This category already exists!");
        }

        Category category = ao.create(Category.class);
        category.setName(name);
        category.save();
        return category;
    }

    @Override
    public void removeCategory(String name) throws ServiceException {
        if (SpecialCategories.AllSpecialCategories.contains(name)) {
            throw new ServiceException("This is a special category that cannot be deleted!");
        }

        Category[] found = ao.find(Category.class, "NAME = ?", name);

        if (found.length > 1) {
            throw new ServiceException("Found multiple categories with the same name!");
        } else if (found.length == 0) {
            throw new ServiceException("Could not find category with this name!");
        }

        if (ao.find(CategoryToTeam.class, "CATEGORY_ID = ?", found[0].getID()).length != 0) {
            throw new ServiceException("Category is still in use by some teams.");
        }

        Category[] defaultCategory = ao.find(Category.class, "NAME = ?", SpecialCategories.DEFAULT);

        // TODO: inject TimesheetEntryService and do it there
        for (TimesheetEntry entry : ao.find(TimesheetEntry.class)) {
            if (entry.getCategory() != null && entry.getCategory().equals(found[0])) {
                entry.setCategory(defaultCategory[0]);
                entry.save();
            }
        }

        ao.delete(found);
    }

    public boolean isPairProgrammingCategory(Category category) {
        String categoryName = category.getName();
        if (categoryName.toLowerCase().contains("(pp)") || categoryName.toLowerCase().contains("pair")) {
            return true;
        }
        return false;
    }
}
