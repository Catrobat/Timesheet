"use strict";

var coordUsersList = "";
var idOfCurrentTimesheet = "";

function initCoordinatorUserList(userInformation) {
    AJS.$(".loadingDiv").show();
    var userListToSort = [];
	
    for (var i = 0; i < userInformation.length; i++) {
    	
    	if (!(userInformation[i].state === "DONE"))
    		userListToSort.push(userInformation[i].userName);
    	
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
        var view_timesheet_button = "<button class='aui-button aui-button-primary view-timesheet-button' " +
        "data-timesheet-id='" + userInformation[i].timesheetID + "'>Timesheet</button>";
        
        var row = "<tr>" +
            "<td headers='ti-users'>" + userInformation[i].userName +
            "</td><td headers='ti-view-timesheet'>"+ view_timesheet_button +
            "</td><td headers='ti-teams'>" + userInformation[i].teams +
            "</td><td headers='ti-state'>" + userInformation[i].state +
            "</td><td headers='ti-inactive-end-date'>" + inactiveEndDate +
            "</td><td headers='ti-remaining-hours'>" + userInformation[i].remainingHours +
            "</td><td headers='ti-target-total-hours'>" + userInformation[i].targetTotalHours +
            "</td><td headers='ti-total-hours'>" + userInformation[i].totalHours +
            "</td><td headers='ti-hours-per-half-year'>" + userInformation[i].hoursPerHalfYear +
            "</td><td headers='ti-hours-per-monitoring-period'>" + userInformation[i].hoursPerMonitoringPeriod +
            "</td><td headers='ti-hours-per-last-monitoring-period'>" + userInformation[i].hoursPerLastMonitoringPeriod +
            "</td><td headers='ti-latest-entry-date'>" + latestEntryDate +
            "</td><td headers='ti-latest-entry-description'>" + userInformation[i].latestEntryDescription +
            "</td></tr>";
        AJS.$("#team-information-table-content").append(row);
    }
    
    var sortedUserList = userListToSort.sort();
    
    for (var i = 0; i < sortedUserList.length; i++)
    	coordUsersList = coordUsersList + "<option value=\"" + sortedUserList[i] + "\"/>";
    
    
    AJS.$("#team-information-table").trigger("update");
    
    AJS.$(".view-timesheet-button").on("click", function (e) {
        var timesheet_id = e.target.getAttribute("data-timesheet-id");
        window.open(AJS.params.baseURL + "/plugins/servlet/timesheet?timesheetID=" + timesheet_id, "_blank");
    });
    AJS.$(".loadingDiv").hide();
}

function initCoordinatorTimesheetSelect(jsonConfig, jsonUser, userInformation) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isTeamCoordinator = false;
    var isSupervisedUser = isReadOnlyUser(userName, config);


    for (var i = 0; i < config.teams.length; i++) {
        var team = config.teams[i];
        //check if user is coordinator of a team
        for (var j = 0; j < team['coordinatorGroups'].length; j++) {
            if (team['coordinatorGroups'][j].localeCompare(userName) == 0) {
            	var teamNameForTeamInformation = team.teamName;
                isTeamCoordinator = true;
            }
        }
    }

    if (isTeamCoordinator && !isSupervisedUser && !isAdmin) {
        AJS.$("#visualizationTeamSelect").show();
    } else if(isSupervisedUser && !isTeamCoordinator) {
        AJS.$("#visualizationTeamSelect").show();
    } else if(!isTeamCoordinator) {
        AJS.$("#visualizationTeamSelect").hide();
    }
}