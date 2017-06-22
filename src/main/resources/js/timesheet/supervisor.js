"use strict";

function initTimesheetAdminUserList (totalUserList) {

    var allActives = "";
    var allDisabled = "";
    var allMasterActives = "";
    var allMasterDisabled = "";
    
    for (var i = 0; i < totalUserList.length; i++) {
    	if (totalUserList[i].state === "ACTIVE" && totalUserList[i].isMasterTimesheet === true)
    		allMasterActives = allMasterActives + "<div>" + totalUserList[i].userName + "</div>"; 
    	else if (totalUserList[i].state === "ACTIVE" && totalUserList[i].isMasterTimesheet === false)
        	allActives = allActives + "<div>" + totalUserList[i].userName + "</div>";
    	else if (totalUserList[i].state === "DISABLED" && totalUserList[i].isMasterTimesheet === true)
    		allMasterDisabled = allMasterDisabled + "<div>" + totalUserList[i].userName + "</div>";
    	else if (totalUserList[i].state === "DISABLED" && totalUserList[i].isMasterTimesheet === false)
    		allDisabled = allDisabled + "<div>" + totalUserList[i].userName + "</div>";	
    }
    
    AJS.$("#showAllAvailableTimesheetUsers").append("<h4><b>Active Timesheets</b></h4>");
    AJS.$("#showAllAvailableTimesheetUsers").append(allActives);
    AJS.$("#showAllAvailableTimesheetUsers").append("<h4><b>Active Master Timesheets</b></h4>");
    AJS.$("#showAllAvailableTimesheetUsers").append(allMasterActives);
    AJS.$("#showAllAvailableTimesheetUsers").append("<h4><b>Disabled Timesheets</b></h4>");
    AJS.$("#showAllAvailableTimesheetUsers").append(allDisabled);
    AJS.$("#showAllAvailableTimesheetUsers").append("<h4><b>Disabled Master Timesheets</b></h4>");
    AJS.$("#showAllAvailableTimesheetUsers").append(allMasterDisabled);
}

function initTimesheetAdminTimesheetSelect(jsonConfig, jsonUser, userList) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isSupervisedUser = isReadOnlyUser(userName, config);
    var listOfUsers = [];

    
    //Select Username of Timesheet
    AJS.$("#approvedUserTimesheetSelect").append("<field-group>");
    AJS.$("#approvedUserTimesheetSelect").append("<div class=\"field-group\"><label for=\"approvedUserSelect\">Timesheet Of</label><input class=\"text approvedUserSelectTimesheetOfUserField\" type=\"text\" id=\"approved-user-select2-field\"></div>");
    AJS.$("#approvedUserTimesheetSelect").append("<div class=\"field-group\"><label for=\"requestMTSheetCheckbox\">Get Master Thesis Timesheet</label><input class=\"checkbox\" type=\"checkbox\" name=\"requestMTSheetCheckbox\" id=\"requestMTSheetCheckbox\"></div>");
    AJS.$("#approvedUserTimesheetSelect").append("<div class=\"field-group\"><input type=\"submit\" value=\"Display\" class=\"aui-button aui-button-primary\"></field-group>");
    AJS.$("#approvedUserTimesheetSelect").append("</field-group>");

    if (isAdmin || isSupervisedUser) {
        for (var j = 0; j < userList[0].length; j++) {
            listOfUsers.push(userList[0][j]['userName']);
        }
    }

    listOfUsers = listOfUsers.filter(function (item, index, inputArray) {
        return inputArray.indexOf(item) == index;
    });

    AJS.$(".approvedUserSelectTimesheetOfUserField").auiSelect2({
        placeholder: "Select User",
        tags: listOfUsers.sort(),
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });

    /*
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
    */

    if(isAdmin) {
        initAdministratorButton();
    }
    if (isSupervisedUser || isAdmin) {
        initSelectTimesheetButton();
        AJS.$("#approvedUserTimesheetSelect").show();
        AJS.$("#coordinatorTimesheetSelect").hide();
        AJS.$("#showAllAvailableTimesheetUsers").show();
        AJS.$("#showAvailableTimesheetUsersForCoordinators").hide();
    } else {
        AJS.$("#approvedUserTimesheetSelect").hide();
    }
}