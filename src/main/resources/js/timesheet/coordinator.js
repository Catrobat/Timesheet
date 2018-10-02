"use strict";

var coordUsersList = "";
var currentUserOfCurrentTimesheet = "";

function initCoordinatorUserList(userInformation) {
	
    var userListToSort = [];
    var userListToSortMaster = [];
	
    for (var i = 0; i < userInformation.length; i++) {
    	
    	if (userInformation[i].isMasterTimesheet === false && !(userInformation[i].state === "DONE"))
    		userListToSort.push(userInformation[i].userName);
    	else if (userInformation[i].isMasterTimesheet === true && !(userInformation[i].state === "DONE"))
    		userListToSortMaster.push(userInformation[i].userName);
    	
        var latestEntryDate;
        if (new Date(userInformation[i].latestEntryDate).getTime() == new Date(0).getTime()) {
            latestEntryDate = "none";
        } else {
            latestEntryDate = (new Date(userInformation[i].latestEntryDate)).toLocaleDateString("en-US");
        }
        var inactiveEndDate;
        if (userInformation[i].inactiveEndDate == null || new Date(userInformation[i].inactiveEndDate).getTime() == new Date(0).getTime()) {
            inactiveEndDate = "";
        } else {
            inactiveEndDate = (new Date(userInformation[i].inactiveEndDate)).toLocaleDateString("en-US");
        }
        thisUsersName = userInformation[i].userName;
        if (userInformation[i].isMasterTimesheet === true) {
        	thisUsersName = thisUsersName + " (MT)"
        }
        
        var view_timesheet_button = "<button class='aui-button aui-button-primary view-timesheet-button' " +
        "data-timesheet-id='" + userInformation[i].timesheetID + "'>Timesheet</button>";
        
        var row = "<tr>" +
            "<td headers='ti-users'>" + thisUsersName +
            "</td><td headers='ti-teams'>" + userInformation[i].teams +
            "</td><td headers='ti-state'>" + userInformation[i].state +
            "</td><td headers='ti-inactive-end-date'>" + inactiveEndDate +
            "</td><td headers='ti-remaining-hours'>" + userInformation[i].remainingHours +
            "</td><td headers='ti-target-total-hours'>" + userInformation[i].targetTotalHours +
            "</td><td headers='ti-total-practice-hours'>" + userInformation[i].totalPracticeHours +
            "</td><td headers='ti-hours-per-half-year'>" + userInformation[i].hoursPerHalfYear +
            "</td><td headers='ti-hours-per-month'>" + userInformation[i].hoursPerMonth +
            "</td><td headers='ti-latest-entry-date'>" + latestEntryDate +
            "</td><td headers='ti-latest-entry-description'>" + userInformation[i].latestEntryDescription +
            "</td><td headers='ti-view-timesheet'>"+ view_timesheet_button +
            "</td></tr>";
        AJS.$("#team-information-table-content").append(row);
    }
    
    var sortedUserList = userListToSort.sort();
    var sortedUserListMaster = userListToSortMaster.sort();
    
    for (var i = 0; i < sortedUserList.length; i++)
    	coordUsersList = coordUsersList + "<option value=\"" + sortedUserList[i] + "\"/>";
    
    for (var i = 0; i < sortedUserListMaster.length; i++)
    	coordUsersList = coordUsersList + "<option value=\"" + sortedUserListMaster[i] + " (Master Timesheet)\"/>";
    
    AJS.$("#team-information-table").trigger("update");
    
    AJS.$(".view-timesheet-button").on("click", function (e) {
        var timesheet_id = e.target.getAttribute("data-timesheet-id");
        currentUserOfCurrentTimesheet = thisUsersName;
        window.open(AJS.params.baseURL + "/plugins/servlet/timesheet?timesheetID=" + timesheet_id, "_blank");
    });
}

function viewedTimesheetUserName() {
	return currentUserOfCurrentTimesheet;
}

function initCoordinatorTimesheetSelect(jsonConfig, jsonUser, userInformation) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isTeamCoordinator = false;
    var isSupervisedUser = isReadOnlyUser(userName, config);
    var listOfUsers = [];

    for (var i = 0; i < config.teams.length; i++) {
        var team = config.teams[i];
        //check if user is coordinator of a team
        for (var j = 0; j < team['coordinatorGroups'].length; j++) {
            if (team['coordinatorGroups'][j].localeCompare(userName) == 0) {
            	var teamNameForTeamInformation = team.teamName;
            	AJS.$("#team-information-teamname").append(teamNameForTeamInformation);
                isTeamCoordinator = true;
            }
        }
    }

    if (isTeamCoordinator && !isSupervisedUser && !isAdmin) {
        initSelectTimesheetButton();
        AJS.$("#visualizationTeamSelect").show();
    } else if(isSupervisedUser && !isTeamCoordinator) {
        AJS.$("#visualizationTeamSelect").show();
    } else if(!isTeamCoordinator) {
        AJS.$("#visualizationTeamSelect").hide();
    }
}