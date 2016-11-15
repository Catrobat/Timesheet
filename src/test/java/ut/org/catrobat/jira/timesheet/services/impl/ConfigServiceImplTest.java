/*
 * Copyright 2014 Stephan Fellhofer
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

package ut.org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import net.java.ao.EntityManager;
import net.java.ao.Query;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.CategoryToTeam;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.impl.CategoryServiceImpl;
import org.catrobat.jira.timesheet.services.impl.SpecialCategories;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
public class ConfigServiceImplTest {

    @SuppressWarnings("UnusedDeclaration")
    private EntityManager entityManager;
    private ActiveObjects ao;
    private CategoryService cs;
    private ConfigService configurationService;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        cs = new CategoryServiceImpl(ao);
        configurationService = new ConfigServiceImpl(ao, cs);
    }

    @Test
    public void testGetConfiguration() {
        assertEquals(0, ao.find(Config.class).length);
        assertNotNull(configurationService.getConfiguration());
        ao.flushAll();
        configurationService.getConfiguration();
        configurationService.getConfiguration();
        ao.flushAll();
        assertEquals(1, ao.find(Config.class).length);

        Config configuration = configurationService.getConfiguration();
        assertTrue(configuration.getID() != 0);
        assertEquals(0, configuration.getTimesheetAdminUsers().length);
        assertEquals(0, configuration.getTimesheetAdminGroups().length);
        assertEquals(0, configuration.getTeams().length);
    }

    @Test
    public void testAddedTeamHasSpecialCategories() {
        Team testteam = configurationService.addTeam("Test", null, null, null);
        CategoryToTeam[] categoryToTeamArray = ao.find(CategoryToTeam.class, Query.select().where("\"TEAM_ID\" = ?", testteam.getID()));

        List<CategoryToTeam> categoryToTeamList = Arrays.asList(categoryToTeamArray);

        for (String specialCategory : SpecialCategories.LIST) {
            Assert.assertFalse(categoryToTeamList.contains(specialCategory));
        }
    }
}
