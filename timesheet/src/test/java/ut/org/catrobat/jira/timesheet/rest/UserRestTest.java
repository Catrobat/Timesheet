package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import junit.framework.Assert;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.activeobjects.impl.ConfigServiceImpl;
import org.catrobat.jira.timesheet.rest.TimesheetRest;
import org.catrobat.jira.timesheet.rest.UserRest;
import org.catrobat.jira.timesheet.rest.json.JsonCategory;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.services.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import ut.org.catrobat.jira.timesheet.activeobjects.MySampleDatabaseUpdater;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(MySampleDatabaseUpdater.class)
public class UserRestTest {

    private UserRest userRest;
    private TeamService teamService;
    private UserManager userManager;
    private UserProfile userProfile;
    private Team team;
    UserKey userKey = new UserKey("USER_KEY_1");
    private ComponentAccessor componentAccessor;
    private PermissionService permissionService;
    private Config config;
    private Response response;
    private HttpServletRequest request;

    private EntityManager entityManager;
    private ActiveObjects ao;
    private CategoryService cs;
    private ConfigService configService;


    @Before
    public void setUp() throws Exception {
        new MockComponentWorker().init();

        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        configService = new ConfigServiceImpl(ao, cs);

        userRest = Mockito.mock(UserRest.class);
        teamService = Mockito.mock(TeamService.class);
        userManager = Mockito.mock(UserManager.class);
        userProfile = Mockito.mock(UserProfile.class);
        team = Mockito.mock(Team.class);
        request = Mockito.mock(HttpServletRequest.class);
        componentAccessor = Mockito.mock(ComponentAccessor.class);
        permissionService = Mockito.mock(PermissionService.class);

        userRest = new UserRest(userManager, configService, teamService);

        Mockito.when(userProfile.getUsername()).thenReturn("testUser");
        Mockito.when(userProfile.getUserKey()).thenReturn(userKey);

        Mockito.when(userManager.getRemoteUser()).thenReturn(userProfile);

        Mockito.when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);
        Mockito.when(permissionService.checkIfUserIsGroupMember(request, "jira-administrators")).thenReturn(true);
        Mockito.when(permissionService.checkIfUserIsGroupMember(request, "Timesheet")).thenReturn(true);
    }

    @Test
    public void testGetUsers() throws Exception {
        assertEquals(0, ao.find(ApprovedUser.class).length);
        assertNotNull(configService.addApprovedUser("testUser", "USER_KEY_1"));
        ao.flushAll();
        assertEquals(1, ao.find(ApprovedUser.class).length);

        assertTrue(configService.isUserApproved("USER_KEY_1"));

        Mockito.when(componentAccessor.getUserKeyService().getKeyForUsername(userProfile.getUsername()))
                .thenReturn("USER_KEY_1");

        response = userRest.getUsers(request);
    }
}
