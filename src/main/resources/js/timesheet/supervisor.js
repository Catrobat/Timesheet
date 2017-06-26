"use strict";

var allUsersList = "";

function initTimesheetAdminUserList (totalUserList) {

    var allActives = "";
    var allDisabled = "";
    var allMasterActives = "";
    var allMasterDisabled = "";
    
    for (var i = 0; i < totalUserList.length; i++) {
    	
    	if (totalUserList[i].isMasterTimesheet === false && !(totalUserList[i].state === "DONE"))
    		allUsersList = allUsersList + "<option value=\"" + totalUserList[i].userName + "\"/>";
    	else if (totalUserList[i].isMasterTimesheet === true && !(totalUserList[i].state === "DONE"))
    		allUsersList = allUsersList + "<option value=\"" + totalUserList[i].userName + " (Master Timesheet)\"/>";
    	
    	if (totalUserList[i].state === "ACTIVE" && totalUserList[i].isMasterTimesheet === true)
    		allMasterActives = allMasterActives + "<div>" + totalUserList[i].userName + "</div>"; 
    	else if (totalUserList[i].state === "ACTIVE" && totalUserList[i].isMasterTimesheet === false)
        	allActives = allActives + "<div>" + totalUserList[i].userName + "</div>";
    	else if (totalUserList[i].state === "DISABLED" && totalUserList[i].isMasterTimesheet === true)
    		allMasterDisabled = allMasterDisabled + "<div>" + totalUserList[i].userName + "</div>";
    	else if (totalUserList[i].state === "DISABLED" && totalUserList[i].isMasterTimesheet === false)
    		allDisabled = allDisabled + "<div>" + totalUserList[i].userName + "</div>";	
    }
    
//    AJS.$("#showAllAvailableTimesheetUsers").append("<h4><b>Active Timesheets</b></h4>");
//    AJS.$("#showAllAvailableTimesheetUsers").append(allActives);
//    AJS.$("#showAllAvailableTimesheetUsers").append("<h4><b>Active Master Timesheets</b></h4>");
//    AJS.$("#showAllAvailableTimesheetUsers").append(allMasterActives);
//    AJS.$("#showAllAvailableTimesheetUsers").append("<h4><b>Disabled Timesheets</b></h4>");
//    AJS.$("#showAllAvailableTimesheetUsers").append(allDisabled);
//    AJS.$("#showAllAvailableTimesheetUsers").append("<h4><b>Disabled Master Timesheets</b></h4>");
//    AJS.$("#showAllAvailableTimesheetUsers").append(allMasterDisabled);
    
    console.log("ttttt allUsersList: " + allUsersList);

    AJS.$("#approved-user-select-div").append("<label for=\"approvedUserSelect\">Timesheet Of</label>" +
    		"<input class=\"text approvedUserSelectTimesheetOfUserField\" type=\"text\" id=\"approved-user-select2-field\" list=\"allusers\">" +
    		"<datalist id=\"allusers\"><select style=\"display: none;\">" + allUsersList + "</select></datalist>");
}

function initTimesheetAdminTimesheetSelect(jsonConfig, jsonUser, userList) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isSupervisedUser = isReadOnlyUser(userName, config);

    //Select Username of Timesheet
    AJS.$("#display-button").append("<input type=\"submit\" value=\"Display\" class=\"aui-button aui-button-primary\">");
    AJS.$("#approved-user-select-div-all-other-content").append("<form id=\"reset-timesheet-settings\">" +
	"<input type=\"submit\" value=\"View Own Timesheet\" class=\"aui-button aui-button-primary\"></form>");

    AJS.$(".approvedUserSelectTimesheetOfUserField").auiSelect2({
        placeholder: "Select User",
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
        AJS.$("#timesheet-hours-save-button").hide();
        AJS.$("#timesheet-hours-update-button").show();
        AJS.$("#timesheet-hours-update-button").click('click', function (e) {
            e.preventDefault();
            if (timesheetIDOfUser) {
                getExistingTimesheetHours(timesheetIDOfUser);
            } else {
                getExistingTimesheetHours(timesheetID);
            }
        });
    }
    if (isSupervisedUser || isAdmin) {
        initSelectTimesheetButton();
        AJS.$("#approvedUserTimesheetSelect").show();
        //TODO: set back to hide!
        AJS.$("#coordinatorTimesheetSelect").hide();


    } else {
        AJS.$("#approvedUserTimesheetSelect").hide();
    }
}