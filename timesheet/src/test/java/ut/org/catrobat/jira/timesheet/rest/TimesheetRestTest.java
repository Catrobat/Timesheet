package ut.org.catrobat.jira.timesheet.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import junit.framework.Assert;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.rest.TimesheetRest;
import org.catrobat.jira.timesheet.rest.json.JsonCategory;
import org.catrobat.jira.timesheet.rest.json.JsonTeam;
import org.catrobat.jira.timesheet.rest.json.JsonTimesheet;
import org.catrobat.jira.timesheet.services.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TimesheetRestTest {

    private CategoryService categoryService;
    private TeamService teamService;
    private UserManager userManager;
    private PermissionService permissionService;
    private Response response;
    private HttpServletRequest request;
    private UserProfile userProfile;
    private TimesheetService sheetService;
    private Timesheet timeSheet;
    private TimesheetEntry timeSheetEntry;
    private TimesheetRest timesheetRest;
    private TimesheetEntryService entryService;
    private Team team;
    UserKey userKey = new UserKey("USER_001");
    private ConfigService configService;
    private UserPreferencesManager userPreferencesManager;
    private ComponentAccessor componentAccessor;

    private SimpleDateFormat sdf;

    @Before
    public void setUp() throws Exception {
        new MockComponentWorker().init();
        entryService = Mockito.mock(TimesheetEntryService.class);
        sheetService = Mockito.mock(TimesheetService.class);
        categoryService = Mockito.mock(CategoryService.class);
        teamService = Mockito.mock(TeamService.class);
        userManager = Mockito.mock(UserManager.class);
        permissionService = Mockito.mock(PermissionService.class);
        request = Mockito.mock(HttpServletRequest.class);
        userProfile = Mockito.mock(UserProfile.class);
        timeSheet = Mockito.mock(Timesheet.class);
        timeSheetEntry = Mockito.mock(TimesheetEntry.class);
        team = Mockito.mock(Team.class);
        configService = Mockito.mock(ConfigService.class);
        userPreferencesManager = Mockito.mock(UserPreferencesManager.class);
        componentAccessor = Mockito.mock(ComponentAccessor.class);

        timesheetRest = new TimesheetRest(entryService, sheetService, categoryService, userManager, teamService, permissionService, configService);
        Mockito.when(permissionService.checkIfUserExists(request)).thenReturn(userProfile);
        Mockito.when(userProfile.getUsername()).thenReturn("testUser");
        Mockito.when(userProfile.getUserKey()).thenReturn(userKey);
        Mockito.when(permissionService.userCanViewTimesheet(userProfile, timeSheet)).thenReturn(true);

        //Timesheet
        Mockito.when(sheetService.getTimesheetByID(1)).thenReturn(timeSheet);
        Mockito.when(sheetService.getTimesheetByUser(userProfile.getUserKey().getStringValue(), false)).thenReturn(timeSheet);
        Mockito.when(timeSheet.getTargetHoursPractice()).thenReturn(50);
        Mockito.when(timeSheet.getTargetHoursTheory()).thenReturn(100);
        Mockito.when(timeSheet.getTargetHours()).thenReturn(300);
        Mockito.when(timeSheet.getTargetHoursCompleted()).thenReturn(150);
        Mockito.when(timeSheet.getEcts()).thenReturn(10);
        Mockito.when(timeSheet.getLatestEntryDate()).thenReturn(new DateTime().toString());
        Mockito.when(timeSheet.getLectures()).thenReturn("Mobile Computing");
        Mockito.when(timeSheet.getIsActive()).thenReturn(true);
        Mockito.when(timeSheet.getIsEnabled()).thenReturn(true);
        Mockito.when(timeSheet.getUserKey()).thenReturn(userKey.getStringValue());
        Mockito.when(timeSheet.getTargetHoursRemoved()).thenReturn(0);
        Mockito.when(timeSheet.getReason()).thenReturn("Agathe Bauer");
        Mockito.when(timeSheet.getIsEnabled()).thenReturn(true);
        Mockito.when(timeSheet.getIsMasterThesisTimesheet()).thenReturn(false);

        //TimesheetEntry
        sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
        Mockito.when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-2015 00:01"));
        Mockito.when(timeSheetEntry.getEndDate()).thenReturn(sdf.parse("31-12-2015 23:59"));
        Mockito.when(timeSheetEntry.getDescription()).thenReturn("My First Entry");
        Mockito.when(timeSheetEntry.getPauseMinutes()).thenReturn(30);
        Mockito.when(timeSheetEntry.getID()).thenReturn(1);
        Mockito.when(teamService.getTeamByID(1)).thenReturn(team);
        Mockito.when(sheetService.getTimesheetByID(1)).thenReturn(timeSheet);
        Mockito.when(entryService.getEntryByID(1)).thenReturn(timeSheetEntry);
    }

    @Test
    public void testGetTeamsOk() throws Exception {
        List<JsonTeam> expectedTeams = new LinkedList<JsonTeam>();
        expectedTeams.add(new JsonTeam(1, "Catroid", new int[0]));
        expectedTeams.add(new JsonTeam(2, "IRC", new int[0]));

        Team team1 = Mockito.mock(Team.class);
        Mockito.when(team1.getID()).thenReturn(1);
        Mockito.when(team1.getTeamName()).thenReturn("Catroid");
        Mockito.when(team1.getCategories()).thenReturn(new Category[0]);

        Team team2 = Mockito.mock(Team.class);
        Mockito.when(team2.getID()).thenReturn(2);
        Mockito.when(team2.getTeamName()).thenReturn("IRC");
        Mockito.when(team2.getCategories()).thenReturn(new Category[0]);

        Set<Team> teams = new HashSet<Team>();
        teams.add(team1);
        teams.add(team2);

        Mockito.when(teamService.getTeamsOfUser("testUser")).thenReturn(teams);

        response = timesheetRest.getTeams(request);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        Assert.assertTrue(responseTeamList.contains(expectedTeams.get(0)));
        Assert.assertTrue(responseTeamList.contains(expectedTeams.get(1)));
    }

    @Test
    public void testGetTeamsNotOk() throws Exception {
        List<JsonTeam> expectedTeams = new LinkedList<JsonTeam>();
        expectedTeams.add(new JsonTeam(1, "Catroid", new int[0]));
        expectedTeams.add(new JsonTeam(2, "IRC", new int[0]));
        expectedTeams.add(new JsonTeam(3, "Test", new int[0]));

        Team team1 = Mockito.mock(Team.class);
        Mockito.when(team1.getID()).thenReturn(1);
        Mockito.when(team1.getTeamName()).thenReturn("Catroid");
        Mockito.when(team1.getCategories()).thenReturn(new Category[0]);

        Team team2 = Mockito.mock(Team.class);
        Mockito.when(team2.getID()).thenReturn(2);
        Mockito.when(team2.getTeamName()).thenReturn("IRC");
        Mockito.when(team2.getCategories()).thenReturn(new Category[0]);

        Set<Team> teams = new HashSet<Team>();
        teams.add(team1);
        teams.add(team2);

        Mockito.when(teamService.getTeamsOfUser("testUser")).thenReturn(teams);

        response = timesheetRest.getTeams(request);
        List<JsonTeam> responseTeamList = (List<JsonTeam>) response.getEntity();
        Assert.assertTrue(responseTeamList.contains(expectedTeams.get(0)));
        Assert.assertTrue(responseTeamList.contains(expectedTeams.get(1)));
        Assert.assertFalse(responseTeamList.contains(expectedTeams.get(2)));
    }

    @Test
    public void testGetCategoriesOk() throws Exception {
        List<JsonCategory> expectedCategories = new LinkedList<JsonCategory>();
        expectedCategories.add(new JsonCategory(1, "Programming"));
        expectedCategories.add(new JsonCategory(2, "Meeting"));

        Category category1 = Mockito.mock(Category.class);
        Mockito.when(category1.getID()).thenReturn(2);
        Mockito.when(category1.getName()).thenReturn("Meeting");

        Category category2 = Mockito.mock(Category.class);
        Mockito.when(category2.getID()).thenReturn(1);
        Mockito.when(category2.getName()).thenReturn("Programming");

        List<Category> categories = new LinkedList<Category>();
        categories.add(category2);
        categories.add(category1);

        Mockito.when(categoryService.all()).thenReturn(categories);

        response = timesheetRest.getCategories(request);
        Assert.assertEquals(expectedCategories, response.getEntity());
    }

    @Test
    public void testGetCategoriesNotOk() throws Exception {
        List<JsonCategory> expectedCategories = new LinkedList<JsonCategory>();
        expectedCategories.add(new JsonCategory(1, "Programming"));
        expectedCategories.add(new JsonCategory(2, "Meeting"));
        expectedCategories.add(new JsonCategory(3, "Test"));

        Category category1 = Mockito.mock(Category.class);
        Mockito.when(category1.getID()).thenReturn(2);
        Mockito.when(category1.getName()).thenReturn("Meeting");

        Category category2 = Mockito.mock(Category.class);
        Mockito.when(category2.getID()).thenReturn(1);
        Mockito.when(category2.getName()).thenReturn("Programming");

        List<Category> categories = new LinkedList<Category>();
        categories.add(category2);
        categories.add(category1);

        Mockito.when(categoryService.all()).thenReturn(categories);

        response = timesheetRest.getCategories(request);
        Assert.assertFalse(expectedCategories == response.getEntity());
    }

    @Test
    public void testGetAndVerifyTimeSheetOk() throws Exception {
        JsonTimesheet expectedTimesheet = new JsonTimesheet(1, timeSheet.getLectures(), timeSheet.getReason(), timeSheet.getEcts(),
                timeSheet.getLatestEntryDate(), timeSheet.getTargetHoursPractice(), timeSheet.getTargetHoursTheory(),
                timeSheet.getTargetHours(), timeSheet.getTargetHoursCompleted(), timeSheet.getTargetHoursRemoved(),
                timeSheet.getIsActive(), timeSheet.getIsEnabled());

        response = timesheetRest.getTimesheet(request, 1);

        Assert.assertEquals(expectedTimesheet, response.getEntity());
    }

    @Test
    public void testGetAndVerifyTimeSheetNotOk() throws Exception {
        JsonTimesheet expectedTimesheet = new JsonTimesheet(5, timeSheet.getLectures(), timeSheet.getReason(), timeSheet.getEcts(),
                timeSheet.getLatestEntryDate(), timeSheet.getTargetHoursPractice(), timeSheet.getTargetHoursTheory(),
                timeSheet.getTargetHours(), timeSheet.getTargetHoursCompleted(), timeSheet.getTargetHoursRemoved(),
                timeSheet.getIsActive(), timeSheet.getIsEnabled());

        response = timesheetRest.getTimesheet(request, 1);

        Assert.assertFalse(expectedTimesheet == response.getEntity());
    }

    /*
    @Test
    public void testPostTimesheetEntry() throws Exception {

        JsonTimesheetEntry expectedTimesheetEntry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), timeSheetEntry.getBeginDate(),
                timeSheetEntry.getPauseMinutes(), timeSheetEntry.getDescription(), 1, 1,
                "CAT-1530", "Partner", false);

        TimesheetEntry[] entries = {timeSheetEntry};

        Category category1 = Mockito.mock(Category.class);
        Mockito.when(category1.getID()).thenReturn(1);
        Mockito.when(category1.getName()).thenReturn("Meeting");

        Category category2 = Mockito.mock(Category.class);
        Mockito.when(category2.getID()).thenReturn(2);
        Mockito.when(category2.getName()).thenReturn("Programming");

        Category[] categories = new Category[2];
        categories[0] = category1;
        categories[1] = category2;

        Mockito.when(team.getCategories()).thenReturn(categories);
        Mockito.when(categoryService.getCategoryByID(1)).thenReturn(category1);

        TimesheetEntry newEntry = Mockito.mock(TimesheetEntry.class);
        Mockito.when(newEntry.getID()).thenReturn(1);

        Mockito.when(entryService.add(timeSheet,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), category1,
                timeSheetEntry.getDescription(), timeSheetEntry.getPauseMinutes(), team, false,
                timeSheetEntry.getBeginDate(), "CAT-1530", "Partner")).thenReturn(newEntry);

        Mockito.when(newEntry.getBeginDate()).thenReturn(sdf.parse("01-01-2016 00:01"));
        Mockito.when(timeSheet.getEntries()).thenReturn(entries);
        Mockito.when(entryService.getEntriesBySheet(timeSheet)).thenReturn(entries);
        Mockito.when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-2015 00:01"));
        Mockito.when(timeSheetEntry.getPairProgrammingUserName()).thenReturn("Partner");

        final ApplicationUser applicationUser = new MockApplicationUser("Partner");
        //com.atlassian.jira.user.util.UserManager um = Mockito.mock(com.atlassian.jira.user.util.UserManager.class);
        Mockito.when(ComponentAccessor.getUserUtil().getUserByName("Partner")).thenReturn(applicationUser);
        new MockComponentWorker()
                .addMock(ComponentAccessor.class, new ComponentAccessor())
                .init();

        sheetService.add(userKey.getStringValue(), timeSheet.getTargetHoursPractice(), timeSheet.getTargetHoursTheory(),
                timeSheet.getTargetHours(), timeSheet.getTargetHoursCompleted(), timeSheet.getTargetHoursRemoved(),
                timeSheet.getLectures(), timeSheet.getReason(), timeSheet.getEcts(), timeSheet.getLatestEntryDate(),
                timeSheet.getIsActive(), timeSheet.getIsEnabled(), timeSheet.getIsMasterThesisTimesheet());

        response = timesheetRest.postTimesheetEntry(request, expectedTimesheetEntry, 1);

        Mockito.verify(entryService).add(timeSheet,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), category1,
                timeSheetEntry.getDescription(), timeSheetEntry.getPauseMinutes(), team, false,
                timeSheetEntry.getBeginDate(), "CAT-1530", "Partner");

        Assert.assertEquals(expectedTimesheetEntry, response.getEntity());
    }
   /*
    @Test
    public void testPutTimesheetEntry() throws Exception {
        String changedDescription = "My changed entry";

        JsonTimesheetEntry expectedTimesheetEntry = new JsonTimesheetEntry(1,
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(),
                timeSheetEntry.getBeginDate(), timeSheetEntry.getPauseMinutes(),
                changedDescription, 1, 2, "CAT-1530", "Partner", false);

        Category category1 = Mockito.mock(Category.class);
        Mockito.when(category1.getID()).thenReturn(1);
        Mockito.when(category1.getName()).thenReturn("Meeting");

        Category category2 = Mockito.mock(Category.class);
        Mockito.when(category2.getID()).thenReturn(2);
        Mockito.when(category2.getName()).thenReturn("Programming");

        Category[] categories = new Category[2];
        categories[0] = category1;
        categories[1] = category2;

        Mockito.when(team.getCategories()).thenReturn(categories);
        Mockito.when(categoryService.getCategoryByID(1)).thenReturn(category1);
        Mockito.when(categoryService.getCategoryByID(2)).thenReturn(category2);

        TimesheetEntry newEntry = Mockito.mock(TimesheetEntry.class);
        Mockito.when(newEntry.getID()).thenReturn(1);

        TimesheetEntry[] entries = {timeSheetEntry};
        Mockito.when(timeSheet.getEntries()).thenReturn(entries);
        Mockito.when(entryService.getEntriesBySheet(timeSheet)).thenReturn(entries);
        Mockito.when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-2015 00:01"));

        Mockito.when(entryService.edit(1, timeSheetEntry.getTimeSheet(),
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), category2,
                changedDescription, timeSheetEntry.getPauseMinutes(), team, false,
                timeSheetEntry.getBeginDate(), "None", "Partner")).thenReturn(newEntry);

        Mockito.when(timeSheet.getIsEnabled()).thenReturn(true);
        Mockito.when(timeSheetEntry.getTimeSheet()).thenReturn(timeSheet);
        Mockito.when(timeSheetEntry.getPairProgrammingUserName()).thenReturn("Partner");

        com.atlassian.jira.user.util.UserManager jiraUserManager = Mockito.mock(com.atlassian.jira.user.util.UserManager.class);

        Mockito.when(jiraUserManager.getUserByName(expectedTimesheetEntry.getPairProgrammingUserName()).getUsername()).thenReturn("Partner");

        response = timesheetRest.putTimesheetEntry(request, expectedTimesheetEntry, 1);

        Mockito.verify(entryService).edit(1, timeSheetEntry.getTimeSheet(),
                timeSheetEntry.getBeginDate(), timeSheetEntry.getEndDate(), category2,
                changedDescription, timeSheetEntry.getPauseMinutes(), team, false,
                timeSheetEntry.getBeginDate(), "None", "Partner");

        Assert.assertEquals(expectedTimesheetEntry, response.getEntity());
    }
    */

    @Test
    public void testDeleteTimesheetEntryOk() throws Exception {

        TimesheetEntry[] entries = {timeSheetEntry};

        TimesheetEntry newEntry = Mockito.mock(TimesheetEntry.class);
        Mockito.when(newEntry.getID()).thenReturn(1);
        Mockito.when(newEntry.getBeginDate()).thenReturn(sdf.parse("01-01-2016 00:01"));
        Mockito.when(timeSheet.getEntries()).thenReturn(entries);
        Mockito.when(entryService.getEntriesBySheet(timeSheet)).thenReturn(entries);
        Mockito.when(timeSheetEntry.getBeginDate()).thenReturn(sdf.parse("01-01-2015 00:01"));
        Mockito.when(timeSheet.getIsEnabled()).thenReturn(true);
        Mockito.when(sheetService.getTimesheetByUser(componentAccessor.getUserKeyService().
                getKeyForUsername(userProfile.getUsername()), false)).thenReturn(timeSheet);

        sheetService.add(userKey.getStringValue(), timeSheet.getTargetHoursPractice(), timeSheet.getTargetHoursTheory(),
                timeSheet.getTargetHours(), timeSheet.getTargetHoursCompleted(), timeSheet.getTargetHoursRemoved(),
                timeSheet.getLectures(), timeSheet.getReason(), timeSheet.getEcts(), timeSheet.getLatestEntryDate(),
                timeSheet.getIsActive(), timeSheet.getIsEnabled(), timeSheet.getIsMasterThesisTimesheet());

        response = timesheetRest.deleteTimesheetEntry(request, 1);

        Mockito.verify(entryService).delete(timeSheetEntry);
    }
}
