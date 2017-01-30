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

package org.catrobat.jira.timesheet.services;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.service.ServiceException;
import org.catrobat.jira.timesheet.activeobjects.Category;

import javax.annotation.Nullable;
import java.util.List;

@Transactional
public interface CategoryService {

    @Nullable
    Category getCategoryByID(int id);

    @Nullable
    Category getCategoryByName(String name);

    List<Category> all();

    Category add(String name) throws ServiceException;

    void removeCategory(String name) throws ServiceException;

    boolean isPairProgrammingCategory(Category category);

}