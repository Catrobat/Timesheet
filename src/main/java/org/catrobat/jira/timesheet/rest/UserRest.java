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

package org.catrobat.jira.timesheet.rest;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.group.search.GroupPickerSearchService;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserUtil;

import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.json.JsonUser;
import org.catrobat.jira.timesheet.rest.json.JsonTeamInformation;
import org.catrobat.jira.timesheet.rest.json.JsonUserInformation;
import org.catrobat.jira.timesheet.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Path("/user")
public class UserRest {
    private static final String DISABLED_GROUP = "Disabled";
    private final ConfigService configService;
    private final PermissionService permissionService;
    private final TimesheetService timesheetService;
    private final TimesheetEntryService timesheetEntryService;
    private final TeamService teamService;

    private final UserSearchService userSearchService;
    private final GroupPickerSearchService groupPickerSearchService;
    
    private static final Logger logger = LoggerFactory.getLogger(UserRest.class);

    public UserRest(ConfigService configService, PermissionService permissionService,
                    TimesheetService timesheetService, TimesheetEntryService timesheetEntryService, TeamService teamService,
                    UserSearchService userSearchService, GroupPickerSearchService groupPickerSearchService) {
        this.configService = configService;
        this.permissionService = permissionService;
        this.timesheetService = timesheetService;
        this.timesheetEntryService = timesheetEntryService;
        this.teamService = teamService;
        this.userSearchService = userSearchService;
        this.groupPickerSearchService = groupPickerSearchService;
    }

    @GET
    @Path("/getUsers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers(@Context HttpServletRequest request) {
        // TODO: only Admins & Read Only Users should have permission to this, change JavaScript accordingly
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        UserUtil userUtil = ComponentAccessor.getUserUtil();
        List<JsonUser> jsonUserList = new ArrayList<>();
        JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(permissionService.getLoggedInUser());
        List<ApplicationUser> allUsers = userSearchService.findUsersAllowEmptyQuery(jiraServiceContext, "");
        for (ApplicationUser user : allUsers) {
            JsonUser jsonUser = new JsonUser();
            jsonUser.setEmail(user.getEmailAddress());
            jsonUser.setUserName(user.getName());

            String displayName = user.getDisplayName();
            int lastSpaceIndex = displayName.lastIndexOf(' ');
            if (lastSpaceIndex >= 0) {
                jsonUser.setFirstName(displayName.substring(0, lastSpaceIndex));
                jsonUser.setLastName(displayName.substring(lastSpaceIndex + 1));
            } else {
                jsonUser.setFirstName(displayName);
            }

            boolean isActive = true;
            for (Group group : userUtil.getGroupsForUser(user.getName())) {
                if (group.getName().toLowerCase().equals(DISABLED_GROUP.toLowerCase())) {
                    isActive = false;
                    break;
                }
            }

            jsonUser.setActive(isActive);
            jsonUserList.add(jsonUser);
        }

        return Response.ok(jsonUserList).build();
    }

    @GET
    @Path("/getUserInformation")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInformation(@Context HttpServletRequest request) {
    	
    	logger.error("1 /getUserInformation reached");
    	Date date = new Date();
    	
        Response response = permissionService.checkRootPermission();
        if (response != null) {
            return response;
        }

        List<JsonUserInformation> jsonUserInformationList = new ArrayList<>();
        
        for (Timesheet timesheet : timesheetService.all()) {
            JsonUserInformation jsonUserInformation = new JsonUserInformation();
            // TODO: check whether user key == name
            jsonUserInformation.setUserName(getUserNameOfUserKey(timesheet.getUserKey()));
            jsonUserInformation.setState(timesheet.getState());
            jsonUserInformation.setLatestEntryDate(timesheet.getLatestEntryBeginDate());
            jsonUserInformation.setHoursPerHalfYear(timesheetEntryService.getHoursOfLastXMonths(timesheet, 6));
            jsonUserInformation.setHoursPerMonth(timesheetEntryService.getHoursOfLastXMonths(timesheet, 1));
            
            jsonUserInformation.setIsMasterTimesheet(timesheet.getIsMasterThesisTimesheet());
            jsonUserInformation.setRemainingHours(timesheet.getTargetHours() - timesheet.getHoursCompleted() 
					+ timesheet.getHoursDeducted());
            jsonUserInformation.setTargetTotalHours(timesheet.getTargetHours());

            TimesheetEntry latestInactiveEntry = timesheetEntryService.getLatestInactiveEntry(timesheet);
            if (latestInactiveEntry != null && (timesheet.getState() == Timesheet.State.INACTIVE
                    || timesheet.getState() == Timesheet.State.INACTIVE_OFFLINE)) {
                Date inactiveEndDate = latestInactiveEntry.getInactiveEndDate();
                jsonUserInformation.setInactiveEndDate(inactiveEndDate);
            }
            jsonUserInformation.setTotalPracticeHours(timesheet.getHoursPracticeCompleted());
            TimesheetEntry latestEntry = timesheetEntryService.getLatestEntry(timesheet);
            if (latestEntry != null) {
                jsonUserInformation.setLatestEntryHours(latestEntry.getDurationMinutes() / 60);
                jsonUserInformation.setLatestEntryDescription(latestEntry.getDescription());
            } else {
                jsonUserInformation.setLatestEntryHours(0);
                jsonUserInformation.setLatestEntryDescription("");
            }

            jsonUserInformation.setEmail(getEmailOfUserKey(timesheet.getUserKey()));
            StringBuilder teamString = new StringBuilder();
            Set<Team> developerSet = teamService.getTeamsOfUser(getUserNameOfUserKey(timesheet.getUserKey()));
            Set<Team> coordinatorSet = teamService.getTeamsOfCoordinator(getUserNameOfUserKey(timesheet.getUserKey()));
            Set<Team> allTeams = new HashSet<>();
            allTeams.addAll(developerSet);
            allTeams.addAll(coordinatorSet);
            for (Team team : allTeams) {
                if (!teamString.toString().equals("")) {
                    teamString.append(", ");
                }
                teamString.append(team.getTeamName());
            }
            jsonUserInformation.setTeams(teamString.toString());
            jsonUserInformation.setTimesheetID(timesheet.getID());

            jsonUserInformationList.add(jsonUserInformation);
        }
        
        logger.error("2 /getUserInformation just before Response");
    	Date date1 = new Date();
    	long diffInMillies = date1.getTime() - date.getTime();
    	TimeUnit timeUnit = TimeUnit.SECONDS;
    	logger.error("3 /getUserInformation diffInMillies/diffInSeconds: " + diffInMillies + "/" + timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS));

        return Response.ok(jsonUserInformationList).build();
    }

    @GET
    @Path("/getUsersForCoordinator")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersForCoordinator(@Context HttpServletRequest request) {
        ApplicationUser user;
        boolean isAdmin = false;
        try {
            user = permissionService.checkIfUserExists();
        } catch (PermissionException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }

        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }
        
        boolean adminExists = permissionService.timesheetAdminExists();
        if (adminExists && permissionService.isTimesheetAdmin(user)) {
            isAdmin = true;
        } else if (!adminExists && permissionService.isJiraAdministrator(user)) {
            isAdmin = true;
        }

        if (!(permissionService.isUserTeamCoordinator(user) || permissionService.isReadOnlyUser(user) || isAdmin)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Access Forbidden: You are neither a team " +
                    "coordinator nor a read only user nor a administrator!").build();
        }

        List<JsonTeamInformation> jsonTeamInformationList = new ArrayList<>();

        for (Timesheet timesheet : timesheetService.all()) {
            if (permissionService.isUserCoordinatorOfTimesheet(user, timesheet)) {
                JsonTeamInformation jsonTeamInformation = new JsonTeamInformation();
                // TODO: check whether user key == name
                jsonTeamInformation.setUserName(getUserNameOfUserKey(timesheet.getUserKey()));
                jsonTeamInformation.setState(timesheet.getState());
                jsonTeamInformation.setLatestEntryDate(timesheet.getLatestEntryBeginDate());
                jsonTeamInformation.setHoursPerHalfYear(timesheetEntryService.getHoursOfLastXMonths(timesheet, 6));
                jsonTeamInformation.setHoursPerMonth(timesheetEntryService.getHoursOfLastXMonths(timesheet, 1));
                jsonTeamInformation.setIsMasterTimesheet(timesheet.getIsMasterThesisTimesheet());
                jsonTeamInformation.setRemainingHours(timesheet.getTargetHours() - timesheet.getHoursCompleted() 
                										+ timesheet.getHoursDeducted());
                jsonTeamInformation.setTargetTotalHours(timesheet.getTargetHours());
                TimesheetEntry latestInactiveEntry = timesheetEntryService.getLatestInactiveEntry(timesheet);
                if (latestInactiveEntry != null && (timesheet.getState() == Timesheet.State.INACTIVE
                        || timesheet.getState() == Timesheet.State.INACTIVE_OFFLINE)) {
                    Date inactiveEndDate = latestInactiveEntry.getInactiveEndDate();
                    jsonTeamInformation.setInactiveEndDate(inactiveEndDate);
                }
                jsonTeamInformation.setTotalPracticeHours(timesheet.getHoursPracticeCompleted());
                TimesheetEntry latestEntry = timesheetEntryService.getLatestEntry(timesheet);
                if (latestEntry != null) {
                    jsonTeamInformation.setLatestEntryHours(latestEntry.getDurationMinutes() / 60);
                    jsonTeamInformation.setLatestEntryDescription(latestEntry.getDescription());
                } else {
                    jsonTeamInformation.setLatestEntryHours(0);
                    jsonTeamInformation.setLatestEntryDescription("");
                }
                
                StringBuilder teamString = new StringBuilder();
                Set<Team> developerSet = teamService.getTeamsOfUser(getUserNameOfUserKey(timesheet.getUserKey()));
                Set<Team> coordinatorSet = teamService.getTeamsOfCoordinator(getUserNameOfUserKey(timesheet.getUserKey()));
                Set<Team> allTeams = new HashSet<>();
                allTeams.addAll(developerSet);
                allTeams.addAll(coordinatorSet);
                for (Team team : allTeams) {
                    if (!teamString.toString().equals("")) {
                        teamString.append(", ");
                    }
                    teamString.append(team.getTeamName());
                }
                jsonTeamInformation.setTeams(teamString.toString());
                
                
                jsonTeamInformationList.add(jsonTeamInformation);
            }
        }

        return Response.ok(jsonTeamInformationList).build();
    }

    private String getUserNameOfUserKey(String userKey) {
        return ComponentAccessor.getUserManager().getUserByKey(userKey).getName();
    }

    private String getEmailOfUserKey(String userKey) {
        return ComponentAccessor.getUserManager().getUserByKey(userKey).getEmailAddress();
    }

    @GET
    @Path("/getPairProgrammingUsers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPairProgrammingUsers(@Context HttpServletRequest request) {
        Response response = permissionService.checkUserPermission();
        if (response != null) {
            return response;
        }

        ApplicationUser loggedInUser = permissionService.getLoggedInUser();

        String pairProgrammingGroup = configService.getConfiguration().getPairProgrammingGroup();
        Collection<ApplicationUser> pairProgrammingUsers;
        if (pairProgrammingGroup == null || pairProgrammingGroup.isEmpty()) {
            JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(loggedInUser);
            pairProgrammingUsers = userSearchService.findUsersAllowEmptyQuery(jiraServiceContext, "");
        } else {
            pairProgrammingUsers = ComponentAccessor.getGroupManager().getUsersInGroup(pairProgrammingGroup);
        }

        List<String> jsonUserList = new ArrayList<>();

        for (ApplicationUser user : pairProgrammingUsers) {
            if (!user.equals(loggedInUser)) {
                jsonUserList.add(user.getName());
            }
        }

        return Response.ok(jsonUserList).build();
    }

    @GET
    @Path("/getGroups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups(@Context HttpServletRequest request) {
        Response response = permissionService.checkRootPermission();
        if (response != null) {
            return response;
        }

        List<String> groupList = new ArrayList<>();
        List<Group> allGroups = groupPickerSearchService.findGroups("");
        for (Group group : allGroups) {
            groupList.add(group.getName());
        }

        return Response.ok(groupList).build();
    }
}
