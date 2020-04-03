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
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;

import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.rest.json.JsonUser;
import org.catrobat.jira.timesheet.rest.json.JsonUserInformation;
import org.catrobat.jira.timesheet.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(UserRest.class);

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
        LOGGER.debug("Retrieving all users from system");
        UserUtil userUtil = ComponentAccessor.getUserUtil();
        List<JsonUser> jsonUserList = new ArrayList<>();
        JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(permissionService.getLoggedInUser());
        List<ApplicationUser> allUsers = userSearchService.findUsersAllowEmptyQuery(jiraServiceContext, "");

        for (ApplicationUser user : allUsers) {
            JsonUser jsonUser = new JsonUser();
            jsonUser.setEmail(user.getEmailAddress());
            jsonUser.setUserName(user.getName());

            String displayName = user.getDisplayName();
            LOGGER.info("Got User: " + displayName);
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
    	
    	logger.debug("1 /getUserInformation reached");
    	Date date = new Date();
    	
        Response response = permissionService.checkRootPermission();
        if (response != null) {
            return response;
        }

        List<JsonUserInformation> jsonUserInformationList = new ArrayList<>();
        
        for (Timesheet timesheet : timesheetService.all()) {

            ApplicationUser u = ComponentAccessor.getUserManager().getUserByKey(timesheet.getUserKey());
            if(u == null) {
                try {
                    timesheetService.remove(timesheet);
                } catch (ServiceException e) {
                    //ignore
                }
                continue;
            }

        	JsonUserInformation jsonUserInformation = new JsonUserInformation(timesheet);
        	
            jsonUserInformation.setHoursPerHalfYear(timesheetEntryService.getHoursOfLastXMonths(timesheet, 6));
            jsonUserInformation.setHoursPerMonth(timesheetEntryService.getHoursOfLastXMonths(timesheet, 1));

            TimesheetEntry latestInactiveEntry = timesheetEntryService.getLatestInactiveEntry(timesheet);
            if (latestInactiveEntry != null && (timesheet.getState() == Timesheet.State.INACTIVE
                    || timesheet.getState() == Timesheet.State.INACTIVE_OFFLINE)) {
                Date inactiveEndDate = latestInactiveEntry.getInactiveEndDate();
                jsonUserInformation.setInactiveEndDate(inactiveEndDate);
            }

            TimesheetEntry latestEntry = timesheetEntryService.getLatestEntry(timesheet);
            if (latestEntry != null) {
                jsonUserInformation.setLatestEntryHours(latestEntry.getDurationMinutes() / 60);
                jsonUserInformation.setLatestEntryDescription(latestEntry.getDescription());
            } else {
                jsonUserInformation.setLatestEntryHours(0);
                jsonUserInformation.setLatestEntryDescription("");
            }

            String userName = jsonUserInformation.getUserName();
            ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(userName);
            StringBuilder teamString = new StringBuilder();
            
            for (Team jsonteam : teamService.all()) {
            	org.catrobat.jira.timesheet.activeobjects.Group[] teamGroups = jsonteam.getGroups();
            	for (org.catrobat.jira.timesheet.activeobjects.Group groupName : teamGroups) {
            		if (groupName.getGroupName().equalsIgnoreCase(userName) ||
                            ComponentAccessor.getGroupManager().isUserInGroup(applicationUser,groupName.getGroupName())) {
            			if (!teamString.toString().equals("")) {
                          teamString.append(", ");
            			}
	            		if (!teamString.toString().contains(jsonteam.getTeamName())) {
	            			teamString.append(jsonteam.getTeamName());
	                		break;
	            		}
            		}
            	}
            }
            jsonUserInformation.setTeams(teamString.toString());

            jsonUserInformationList.add(jsonUserInformation);
        }
        
    	Date date1 = new Date();
    	long diffInMillies = date1.getTime() - date.getTime();
    	TimeUnit timeUnit = TimeUnit.SECONDS;
    	logger.debug("2 /getUserInformation diffInMillies/diffInSeconds: " + diffInMillies + "/" + timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS));

        return Response.ok(jsonUserInformationList).build();
    } 

    @GET
    @Path("/getUsersForCoordinator/{currentTimesheetID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersForCoordinator(@Context HttpServletRequest request, @PathParam("currentTimesheetID") String currentTimesheetID) {

    	LOGGER.debug("1 /getUsersForCoordinator reached");
    	
    	Date date = new Date();
    	ApplicationUser user = null;
    	boolean isAdmin = false;
    	
    	LOGGER.trace("getRequestURI : " + request.getRequestURI());
    	LOGGER.trace("currentTimesheetID : " + currentTimesheetID);
    	
    	if (currentTimesheetID != null && !currentTimesheetID.equals("undefined")) {
    		int currentTimesheetIDint = Integer.parseInt(currentTimesheetID);
    		Timesheet currentTimesheet = timesheetService.getTimesheetByID(currentTimesheetIDint);
        	String currentUserKey = currentTimesheet.getUserKey();
        	LOGGER.trace("currentUserKey: " + currentUserKey);
        	user = ComponentAccessor.getUserManager().getUserByKey(currentUserKey);
        	LOGGER.debug("with ID > USER FOR COORD TEAM INFO VIEW IS: " + user.getDisplayName());		
    	}
        
        if (user == null || user.getUsername().equals("")) {
        	try {
                user = permissionService.checkIfUserExists();
                LOGGER.debug("without ID > USER FOR COORD TEAM INFO VIEW IS: " + user.getDisplayName());
            } catch (PermissionException e) {
                return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
            }
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
            return Response.status(Response.Status.UNAUTHORIZED).entity("Access Forbidden: " + user.getDisplayName() + " is neither a team " +
                    "coordinator nor a read only user nor an administrator!").build();
        }

        Set<Team> teamsOfCoordinator = teamService.getTeamsOfCoordinator(user.getUsername());
        ArrayList<String> teamsOfCoordArray = new ArrayList<String>();
        for (Team t : teamsOfCoordinator) {
        	teamsOfCoordArray.add(t.getTeamName());
        }
        
        
        List<JsonUserInformation> jsonUserInformationListForCoordinator = new ArrayList<>();

        for (Timesheet timesheet : timesheetService.all()) {

            ApplicationUser u = ComponentAccessor.getUserManager().getUserByKey(timesheet.getUserKey());
            if(u == null) {
                try {
                    timesheetService.remove(timesheet);
                } catch (ServiceException e) {
                    //ignore
                }
                continue;
            }
        	JsonUserInformation jsonUserInformation = new JsonUserInformation(timesheet);
        	
            String userName = jsonUserInformation.getUserName();
            ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(userName);
            StringBuilder teamString = new StringBuilder();
            ArrayList<String> teamArray = new ArrayList<String>();
            
            for (Team jsonteam : teamService.all()) {
            	org.catrobat.jira.timesheet.activeobjects.Group[] teamGroups = jsonteam.getGroups();
            	for (org.catrobat.jira.timesheet.activeobjects.Group groupName : teamGroups) {
            		if (groupName.getGroupName().equalsIgnoreCase(userName) ||
                            ComponentAccessor.getGroupManager().isUserInGroup(applicationUser,groupName.getGroupName())) {
            			if (!teamString.toString().equals("")) {
                          teamString.append(", ");
            			}
                		if (!teamString.toString().contains(jsonteam.getTeamName())) {
                			teamString.append(jsonteam.getTeamName());
                			
                			if (!teamArray.contains(jsonteam.getTeamName())) {
                				teamArray.add(jsonteam.getTeamName());
                			}
                			
                			break;
                		}
            		}
            	}
            }
            
            teamArray.retainAll(teamsOfCoordArray);
            if (teamArray.size() > 0) {
            	LOGGER.trace(userName + " - is a teammember of: " + teamArray.toString());
            	
            }
            else {
            	LOGGER.trace("CONTINUE REACHED...");
            	continue;
            }

            jsonUserInformation.setTeams(teamString.toString());
        	jsonUserInformation.setHoursPerHalfYear(timesheetEntryService.getHoursOfLastXMonths(timesheet, 6));
        	jsonUserInformation.setHoursPerMonth(timesheetEntryService.getHoursOfLastXMonths(timesheet, 1));

            TimesheetEntry latestInactiveEntry = timesheetEntryService.getLatestInactiveEntry(timesheet);
            if (latestInactiveEntry != null && (timesheet.getState() == Timesheet.State.INACTIVE
                    || timesheet.getState() == Timesheet.State.INACTIVE_OFFLINE)) {
                Date inactiveEndDate = latestInactiveEntry.getInactiveEndDate();
                jsonUserInformation.setInactiveEndDate(inactiveEndDate);
            }

            TimesheetEntry latestEntry = timesheetEntryService.getLatestEntry(timesheet);
            if (latestEntry != null) {
            	jsonUserInformation.setLatestEntryHours(latestEntry.getDurationMinutes() / 60);
            	jsonUserInformation.setLatestEntryDescription(latestEntry.getDescription());
            } else {
            	jsonUserInformation.setLatestEntryHours(0);
            	jsonUserInformation.setLatestEntryDescription("");
            }
            
            jsonUserInformationListForCoordinator.add(jsonUserInformation);
        }

        Date date1 = new Date();
    	long diffInMillies1 = date1.getTime() - date.getTime();
    	TimeUnit timeUnit1 = TimeUnit.SECONDS;
    	logger.debug("3 /getUsersForCoordinator diffInMillies/diffInSeconds: " + diffInMillies1 + "/" + timeUnit1.convert(diffInMillies1,TimeUnit.MILLISECONDS));
        
        return Response.ok(jsonUserInformationListForCoordinator).build();
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

    @GET
    @Path("/getActiveTimesheetUsers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveTimesheetUsers(@Context HttpServletRequest request){
        UserManager userManager = ComponentAccessor.getUserManager();
        ArrayList<String> queried_user_key = new ArrayList<>();
        ArrayList<Object> active_user_list = new ArrayList<>();

        for(Timesheet sheet : timesheetService.all()){
            Map<String, String> result = new HashMap<>();
            ApplicationUser current_user = userManager.getUserByKey(sheet.getUserKey());

            if(current_user == null){
                return Response.status(Response.Status.CONFLICT).entity("No User Was found!").build();
            }

            if(queried_user_key.contains(current_user.getKey())){
                LOGGER.info("User has already been queried continue");
                continue;
            }
            if(sheet.getState() != Timesheet.State.ACTIVE){
                LOGGER.info("Current Timesheet is not active continue");
                continue;
            }

            result.put("userKey", sheet.getUserKey());
            result.put("displayName", sheet.getDisplayName());
            queried_user_key.add(sheet.getUserKey());
            active_user_list.add(result);
        }

        return Response.ok().entity(active_user_list).build();
    }
}
