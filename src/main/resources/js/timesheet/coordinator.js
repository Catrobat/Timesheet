"use strict";

function initCoordinatorUserList(userInformation) {
	
    for (var i = 0; i < userInformation.length; i++) {
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
            "</td><td headers='ti-state'>" + userInformation[i].state +
            "</td><td headers='ti-inactive-end-date'>" + inactiveEndDate +
            "</td><td headers='ti-remaining-hours'>" + userInformation[i].remainingHours +
            "</td><td headers='ti-total-practice-hours'>" + userInformation[i].totalPracticeHours +
            "</td><td headers='ti-hours-per-half-year'>" + userInformation[i].hoursPerHalfYear +
            "</td><td headers='ti-hours-per-month'>" + userInformation[i].hoursPerMonth +
            "</td><td headers='ti-latest-entry-date'>" + latestEntryDate +
            "</td><td headers='ti-latest-entry-hours'>" + userInformation[i].latestEntryHours +
            "</td><td headers='ti-latest-entry-description'>" + userInformation[i].latestEntryDescription +
            "</td></tr>";
        AJS.$("#team-information-table-content").append(row);
        
//      Show Users for Coordinator in "View Other Timesheet" tab
        var userListe = "<div>" + userInformation[i].userName + "</div>";
        AJS.$("#showAvailableTimesheetUsersForCoordinators").append(userListe);
    }
    AJS.$("#team-information-table").trigger("update");
}

function initCoordinatorTimesheetSelect(jsonConfig, jsonUser, userInformation) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isTeamCoordinator = false;
    var isSupervisedUser = isReadOnlyUser(userName, config);
    var listOfUsers = [];

    AJS.$("#coordinatorTimesheetSelect").append("<field-group>");
    AJS.$("#coordinatorTimesheetSelect").append("<div class=\"field-group\"><label for=\"permission\">Timesheet Of</label><input class=\"text selectTimesheetOfUserField\" type=\"text\" id=\"user-select2-field\"></div>");
    AJS.$("#coordinatorTimesheetSelect").append("<div class=\"field-group\"><input type=\"submit\" value=\"Show\" class=\"aui-button aui-button-primary\"></field-group>");
    AJS.$("#coordinatorTimesheetSelect").append("</field-group>");

    AJS.$("#coordinatorTimesheetSelect").append("<field-group>");
    AJS.$("#coordinatorTimesheetSelect").append("<h3>Coordinator of Team: </h3>");
    for (var i = 0; i < config.teams.length; i++) {
        var team = config.teams[i];
        //check if user is coordinator of a team
        for (var j = 0; j < team['coordinatorGroups'].length; j++) {
            if (team['coordinatorGroups'][j].localeCompare(userName) == 0) {
                //add users of that team to the select2
                for (var j = 0; j < team['developerGroups'].length; j++) {
                    if(team['developerGroups'][j] != userName)
                        if (!containsElement(listOfUsers, team['developerGroups'][j]))
                            listOfUsers.push(team['developerGroups'][j]);
                }
                AJS.$("#coordinatorTimesheetSelect").append("<div class=\"field-group\"><label>"+ team.teamName +"</label></div>");
                AJS.$("#coordinatorTimesheetSelect").append("</field-group>");
                isTeamCoordinator = true;
            }
        }
    }
    
    listOfUsers = listOfUsers.filter(function (item, index, inputArray) {
        return inputArray.indexOf(item) == index;
    });
    

    AJS.$(".selectTimesheetOfUserField").auiSelect2({
        placeholder: "Select User",
        tags: listOfUsers.sort(),
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });

    if (isTeamCoordinator && !isSupervisedUser && !isAdmin) {
        initSelectTimesheetButton();
        AJS.$("#coordinatorTimesheetSelect").show();
        AJS.$("#approvedUserTimesheetSelect").hide();
        AJS.$("#visualizationTeamSelect").show();
        AJS.$("#showAvailableTimesheetUsersForCoordinators").show();
        AJS.$("#showAllAvailableTimesheetUsers").hide();
    } else if(isSupervisedUser && !isTeamCoordinator) {
        AJS.$("#coordinatorTimesheetSelect").hide();
        AJS.$("#approvedUserTimesheetSelect").show();
        AJS.$("#visualizationTeamSelect").show();
        AJS.$("#showAllAvailableTimesheetUsers").show();
    } else if(!isTeamCoordinator) {
        AJS.$("#coordinatorTimesheetSelect").hide();
        AJS.$("#visualizationTeamSelect").hide();
    }
}