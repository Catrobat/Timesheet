"use strict";

var allUsersList = "";

function initTimesheetAdminUserList (totalUserList) {

    var allActives = "";
    var allDisabled = "";
    var allMasterActives = "";
    var allMasterDisabled = "";
    var userListToSort = [];
    var userListToSortMaster = [];
    
    for (var i = 0; i < totalUserList.length; i++) {
    	
    	if (totalUserList[i].isMasterTimesheet === false && !(totalUserList[i].state === "DONE"))
    		userListToSort.push(totalUserList[i].userName);
    	else if (totalUserList[i].isMasterTimesheet === true && !(totalUserList[i].state === "DONE"))
    		userListToSortMaster.push(totalUserList[i].userName);
    	
    	if (totalUserList[i].state === "ACTIVE" && totalUserList[i].isMasterTimesheet === true)
    		allMasterActives = allMasterActives + "<div>" + totalUserList[i].userName + "</div>"; 
    	else if (totalUserList[i].state === "ACTIVE" && totalUserList[i].isMasterTimesheet === false)
        	allActives = allActives + "<div>" + totalUserList[i].userName + "</div>";
    	else if (totalUserList[i].state === "DISABLED" && totalUserList[i].isMasterTimesheet === true)
    		allMasterDisabled = allMasterDisabled + "<div>" + totalUserList[i].userName + "</div>";
    	else if (totalUserList[i].state === "DISABLED" && totalUserList[i].isMasterTimesheet === false)
    		allDisabled = allDisabled + "<div>" + totalUserList[i].userName + "</div>";	
    }
    
    var sortedUserList = userListToSort.sort();
    var sortedUserListMaster = userListToSortMaster.sort();
    
    for (var i = 0; i < sortedUserList.length; i++)
    	allUsersList = allUsersList + "<option value=\"" + sortedUserList[i] + "\"/>";
    
    for (var i = 0; i < sortedUserListMaster.length; i++)
    	allUsersList = allUsersList + "<option value=\"" + sortedUserListMaster[i] + " (Master Timesheet)\"/>";
}

function initTimesheetAdminTimesheetSelect(jsonConfig, jsonUser, userList) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isSupervisedUser = isReadOnlyUser(userName, config);

    if (isSupervisedUser || isAdmin) {
        initSelectTimesheetButton();
    } 
}