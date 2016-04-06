"use strict";

function initApprovedUserTimesheetSelect(jsonConfig, jsonUser, userList) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isApprovedUser = isUserApprovedUser(userName, config);
    var listOfUsers = [];

    //Select Username of Timesheet
    AJS.$("#approvedUserTimesheetSelect").append("<field-group>");
    AJS.$("#approvedUserTimesheetSelect").append("<h3>Approved User Space</h3>");
    AJS.$("#approvedUserTimesheetSelect").append("<div class=\"field-group\"><label for=\"approvedUserSelect\">Timesheet Of</label><input class=\"text approvedUserSelectTimesheetOfUserField\" type=\"text\" id=\"approved-user-select2-field\"></div>");
    AJS.$("#approvedUserTimesheetSelect").append("<div class=\"field-group\"><label for=\"requestMTSheetCheckbox\">Get Master Thesis Timesheet</label><input class=\"checkbox\" type=\"checkbox\" name=\"requestMTSheetCheckbox\" id=\"requestMTSheetCheckbox\"></div>");
    AJS.$("#approvedUserTimesheetSelect").append("<div class=\"field-group\"><input type=\"submit\" value=\"Display\" class=\"aui-button aui-button-primary\"></field-group>");
    AJS.$("#approvedUserTimesheetSelect").append("</field-group>");

    if (isAdmin || isApprovedUser) {
        for (var j = 0; j < userList[0].length; j++) {
            listOfUsers.push(userList[0][j]['userName']);
        }
    }

    AJS.$(".approvedUserSelectTimesheetOfUserField").auiSelect2({
        placeholder: "Select User",
        tags: listOfUsers.sort(),
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });

    //Team Visualization
    var teamNameList = [];
    for (var i = 0; i < config.teams.length; i++) {
        teamNameList.push(config.teams[i]['teamName']);
    }

    AJS.$("#visualizationTeamSelect").append("<field-group>");
    AJS.$("#visualizationTeamSelect").append("<h3>Team</h3>");
    AJS.$("#visualizationTeamSelect").append("<div class=\"field-group\"><label for=\"teamSelect\">Visualize Team Data</label><input class=\"text teamSelectField\" type=\"text\" id=\"select-team-select2-field\"></div>");
    AJS.$("#visualizationTeamSelect").append("<div class=\"field-group\"><input type=\"submit\" value=\"Visualize\" class=\"aui-button aui-button-primary\"></field-group>");
    AJS.$("#visualizationTeamSelect").append("</field-group>");
    AJS.$(".teamSelectField").auiSelect2({
        placeholder: "Select Team",
        tags: teamNameList.sort(),
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });
    if(isAdmin) {
        initAdministratorButton();
    }
    if (isApprovedUser || isAdmin) {
        initSelectTimesheetButton();
        AJS.$("#approvedUserTimesheetSelect").show();
        AJS.$("#coordinatorTimesheetSelect").hide();
    } else {
        AJS.$("#approvedUserTimesheetSelect").hide();
    }
}