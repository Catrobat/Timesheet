"use strict";

var coordUsersList = "";

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
        var row = "<tr>" +
            "<td headers='ti-users'>" + userInformation[i].userName +
            "</td><td headers='ti-teams'>" + userInformation[i].teams +
            "</td><td headers='ti-state'>" + userInformation[i].state +
            "</td><td headers='ti-inactive-end-date'>" + inactiveEndDate +
            "</td><td headers='ti-remaining-hours'>" + userInformation[i].remainingHours +
            "</td><td headers='ti-target-total-hours'>" + userInformation[i].targetTotalHours +
            "</td><td headers='ti-total-practice-hours'>" + userInformation[i].totalPracticeHours +
            "</td><td headers='ti-hours-per-half-year'>" + userInformation[i].hoursPerHalfYear +
            "</td><td headers='ti-hours-per-month'>" + userInformation[i].hoursPerMonth +
            "</td><td headers='ti-latest-entry-date'>" + latestEntryDate +
            "</td><td headers='ti-latest-entry-hours'>" + userInformation[i].latestEntryHours +
            "</td><td headers='ti-latest-entry-description'>" + userInformation[i].latestEntryDescription +
            "</td></tr>";
        AJS.$("#team-information-table-content").append(row);
    }
    
    var sortedUserList = userListToSort.sort();
    var sortedUserListMaster = userListToSortMaster.sort();
    
    for (var i = 0; i < sortedUserList.length; i++)
    	coordUsersList = coordUsersList + "<option value=\"" + sortedUserList[i] + "\"/>";
    
    for (var i = 0; i < sortedUserListMaster.length; i++)
    	coordUsersList = coordUsersList + "<option value=\"" + sortedUserListMaster[i] + " (Master Timesheet)\"/>";

    // Provide a sorted List of UserNames for the View Other Timesheet Enter Box of Team-Coordinators
    AJS.$("#coordinatorTimesheetSelect-div").append("<label for=\"permission\">Timesheet Of</label>" +
    		"<input class=\"text selectTimesheetOfUserField\" type=\"text\" id=\"user-select2-field\" list=\"coordusers\">" +
    		"<datalist id=\"coordusers\"><select style=\"display: none;\">" + coordUsersList + "</select></datalist>");
    
    AJS.$("#team-information-table").trigger("update");
}

function initCoordinatorTimesheetSelect(jsonConfig, jsonUser, userInformation) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isTeamCoordinator = false;
    var isSupervisedUser = isReadOnlyUser(userName, config);
    var listOfUsers = [];

    AJS.$("#coordinatorTimesheetSelect-div-all-other-content").append("<input type=\"submit\" value=\"Show\" class=\"aui-button aui-button-primary\">");
    AJS.$("#coordinatorTimesheetSelect-div-all-other-content-1").append("<form id=\"reset-timesheet-settings-coord\">" +
	"<input type=\"submit\" value=\"View Own Timesheet\" class=\"aui-button aui-button-primary\"></form>");

    for (var i = 0; i < config.teams.length; i++) {
        var team = config.teams[i];
        //check if user is coordinator of a team
        for (var j = 0; j < team['coordinatorGroups'].length; j++) {
            if (team['coordinatorGroups'][j].localeCompare(userName) == 0) {
            	AJS.$("#coordinatorTimesheetSelect-div-all-other-content-2").append("<h3>Coordinator of Team: " + team.teamName +"</h3>");
                isTeamCoordinator = true;
            }
        }
    }

    AJS.$(".selectTimesheetOfUserField").auiSelect2({
        placeholder: "Select User",
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });

    if (isTeamCoordinator && !isSupervisedUser && !isAdmin) {
        initSelectTimesheetButton();
        AJS.$("#coordinatorTimesheetSelect").show();
        AJS.$("#approvedUserTimesheetSelect").hide();
        AJS.$("#visualizationTeamSelect").show();
    } else if(isSupervisedUser && !isTeamCoordinator) {
        AJS.$("#coordinatorTimesheetSelect").hide();
        AJS.$("#approvedUserTimesheetSelect").show();
        AJS.$("#visualizationTeamSelect").show();
    } else if(!isTeamCoordinator) {
        AJS.$("#coordinatorTimesheetSelect").hide();
        AJS.$("#visualizationTeamSelect").hide();
    }
}