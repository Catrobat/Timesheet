"use strict";

function initCoordinatorTimesheetSelect(jsonConfig, jsonUser) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isTeamCoordinator = false;
    var isApprovedUser = isUserApprovedUser(userName, config);
    var listOfUsers = [];

    AJS.$("#coordinatorTimesheetSelect").append("<field-group>");
    AJS.$("#coordinatorTimesheetSelect").append("<h3>Coordinator Space</h3>");
    AJS.$("#coordinatorTimesheetSelect").append("<div class=\"field-group\"><label for=\"permission\">Timesheet Of</label><input class=\"text selectTimesheetOfUserField\" type=\"text\" id=\"user-select2-field\"></div>");
    AJS.$("#coordinatorTimesheetSelect").append("<div class=\"field-group\"><input type=\"submit\" value=\"Show\" class=\"aui-button aui-button-primary\"></field-group>");
    AJS.$("#coordinatorTimesheetSelect").append("</field-group>");

    AJS.$("#coordinatorTimesheetSelect").append("<field-group>");
    AJS.$("#coordinatorTimesheetSelect").append("<h3>Your Teams: </h3>");
    for (var i = 0; i < config.teams.length; i++) {
        var team = config.teams[i];
        //check if user is coordinator of a team
        for (var j = 0; j < team['coordinatorGroups'].length; j++) {
            if (team['coordinatorGroups'][j].localeCompare(userName) == 0) {
                //add users of that team to the select2
                for (var j = 0; j < team['developerGroups'].length; j++) {
                    if (!containsElement(listOfUsers, team['developerGroups'][j]))
                        listOfUsers.push(team['developerGroups'][j]);
                }
                AJS.$("#coordinatorTimesheetSelect").append("<div class=\"field-group\"><label>"+ team.teamName +"</label></div>");
                AJS.$("#coordinatorTimesheetSelect").append("</field-group>");
                isTeamCoordinator = true;
            }
        }
    }
    AJS.$(".selectTimesheetOfUserField").auiSelect2({
        placeholder: "Select User",
        tags: listOfUsers.sort(),
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });

    if (isTeamCoordinator && !isApprovedUser) {
        initSelectTimesheetButton();
        AJS.$("#coordinatorTimesheetSelect").show();
        AJS.$("#approvedUserTimesheetSelect").hide();
    } else if(isApprovedUser && !isTeamCoordinator) {
        AJS.$("#coordinatorTimesheetSelect").hide();
        AJS.$("#approvedUserTimesheetSelect").show();
    } else {
        AJS.$("#coordinatorTimesheetSelect").hide();
    }
}