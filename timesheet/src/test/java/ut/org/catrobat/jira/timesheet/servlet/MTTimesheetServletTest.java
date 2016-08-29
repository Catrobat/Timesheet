package ut.org.catrobat.jira.timesheet.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TeamService;
import org.catrobat.jira.timesheet.services.TimesheetService;
import org.catrobat.jira.timesheet.servlet.MasterThesisTimesheetServlet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MTTimesheetServletTest {

    private MasterThesisTimesheetServlet masterThesisTimesheetServlet;

    private LoginUriProvider loginUriProvider;
    private TemplateRenderer templateRenderer;
    private TimesheetService sheetService;
    private PermissionService permissionService;

    private Timesheet timeSheet;
    private UserProfile userProfile;
    private TeamService teamService;
    private UserManager userManager;

    private HttpServletResponse response;
    private HttpServletRequest request;

    private ComponentAccessor componentAccessor;

    private Team team;
    private UserProfile admin;

    final String userKey = "USER_001";
    final int targetHoursPractice = 150;
    final int targetHoursTheory = 0;
    final int targeHours = 300;
    final int targetHoursCompleted = 150;
    final int targetHoursRemoved = 0;
    final int ects = 10;
    final String latestEntryDate = "Not Available";
    final String lectures = "Mobile Applications (705.881)";
    final String reason = "Agathe Bauer";

    @Before
    public void setUp() throws Exception {
        new MockComponentWorker().init();

        teamService = Mockito.mock(TeamService.class);
        userManager = Mockito.mock(UserManager.class);
        loginUriProvider = Mockito.mock(LoginUriProvider.class);
        templateRenderer = Mockito.mock(TemplateRenderer.class);
        sheetService = Mockito.mock(TimesheetService.class);
        permissionService = Mockito.mock(PermissionService.class);
        componentAccessor = Mockito.mock(ComponentAccessor.class);

        userProfile = Mockito.mock(UserProfile.class);
        timeSheet = Mockito.mock(Timesheet.class);
        sheetService = Mockito.mock(TimesheetService.class);
        team = Mockito.mock(Team.class);

        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);

        masterThesisTimesheetServlet = new MasterThesisTimesheetServlet(loginUriProvider, templateRenderer, sheetService, permissionService);

        admin = Mockito.mock(UserProfile.class);
        UserKey admin_key = new UserKey("admin_key");
        Mockito.when(admin.getUserKey()).thenReturn(admin_key);
        Mockito.when(admin.getUsername()).thenReturn("admin");
        Mockito.when(userManager.isAdmin(admin_key)).thenReturn(true);
        Mockito.when(userManager.getUserProfile(admin_key.getStringValue())).thenReturn(admin);
        Mockito.when(userManager.getRemoteUser(request)).thenReturn(admin);
        Mockito.when(permissionService.checkIfUserExists(request)).thenReturn(admin);
        Mockito.when(sheetService.getTimesheetByUser("admin_key", false)).thenReturn(timeSheet);
        Mockito.when(permissionService.checkIfUserIsGroupMember(request, "Timesheet", false)).thenReturn(true);
        Mockito.when(timeSheet.getID()).thenReturn(1);
        Mockito.when(timeSheet.getUserKey()).thenReturn(admin_key.getStringValue());
        Mockito.when(timeSheet.getIsActive()).thenReturn(true);
        Mockito.when(timeSheet.getIsEnabled()).thenReturn(false);
        Mockito.when(timeSheet.getIsMasterThesisTimesheet()).thenReturn(false);
        Mockito.when(permissionService.checkIfUserIsGroupMember(request, "jira-administrators", false)).thenReturn(true);
    }

    //Maps is a Google Classe -> can not be mocked
    /*
    @Test
    public void testDoGet() throws Exception {
        timeSheet = sheetService.add(userKey, targetHoursPractice, targetHoursTheory, targeHours, targetHoursCompleted, targetHoursRemoved, lectures, reason, ects, latestEntryDate, true, true, false);
        Mockito.when(sheetService.getTimesheetByUser("admin_key", false)).thenReturn(timeSheet);
        Mockito.when(sheetService.getTimesheetByUser(componentAccessor.getUserKeyService().
                getKeyForUsername(admin.getUsername()), false)).thenReturn(timeSheet);

        timesheetServlet.doGet(request, response);
    }
    */

    @Test(expected = NullPointerException.class)
    public void testDoGetNullPointerException() throws Exception {
        Mockito.when(userManager.getRemoteUser(request)).thenReturn(admin);
        Mockito.when(permissionService.checkIfUserExists(request)).thenReturn(admin);
        Mockito.when(sheetService.getTimesheetByUser("admin_key", false)).thenReturn(null);

        masterThesisTimesheetServlet.doGet(request, response);
    }
}
