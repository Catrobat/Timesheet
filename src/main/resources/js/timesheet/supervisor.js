"use strict";

var allUsersList = "";

function initTimesheetAdminUserList (totalUserList) {

    var allActives = "";
    var allDisabled = "";
    var userListToSort = [];
    
    for (var i = 0; i < totalUserList.length; i++) {
    	
    	if (!(totalUserList[i].state === "DONE"))
    		userListToSort.push(totalUserList[i].userName);    	
    	if (totalUserList[i].state === "ACTIVE")
        	allActives = allActives + "<div>" + totalUserList[i].userName + "</div>";
    	else if (totalUserList[i].state === "DISABLED")
    		allDisabled = allDisabled + "<div>" + totalUserList[i].userName + "</div>";	
    }
    
    var sortedUserList = userListToSort.sort();
    
    for (var i = 0; i < sortedUserList.length; i++)
    	allUsersList = allUsersList + "<option value=\"" + sortedUserList[i] + "\"/>";
    
}

function initTimesheetAdminTimesheetSelect(jsonConfig, jsonUser, userList) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isSupervisedUser = isReadOnlyUser(userName, config);

    if (isSupervisedUser || isAdmin) {
        initSelectTimesheetButton();
    } 
}