"use strict";

var restBaseUrl;
var hostname = location.hostname;
var baseUrl;
var timesheetData_;
var timesheetIDOfUser;

AJS.toInit(function () {
    window.onbeforeunload = null;
    if (hostname.includes("catrob.at")) {
        baseUrl = "https://" + hostname;
    }
    else {
        baseUrl = AJS.params.baseURL;
    }

    restBaseUrl = baseUrl + "/rest/timesheet/latest/";

    // printDomainAttributes();

    AJS.$("#timesheet-table").hide();
    AJS.$("#table-header").hide();
    checkConstrains();

    if (isCoordinator) {
        AJS.$("#coord_private_table").show();
    } else {
        AJS.$("#coord_private_table").hide();
    }

    if (isCoordinator || isSupervisor || isAdmin) {
        fetchUsers();
        AJS.$("#coord_private").show();
        AJS.$("#timesheet-owner").show();
        AJS.$("#timesheet-owner-private").hide();
    } else {
        AJS.$("#coord_private").hide();
        AJS.$("#timesheet-owner").hide();
        AJS.$("#timesheet-owner-private").show();
    }

    if(isAdmin)
        AJS.$("#timesheet-hours-save-button").show();
    else
        AJS.$("#timesheet-hours-save-button").hide();

// Don't touch the .submit function below - otherwise Firefox doesn't like it any more!

    AJS.$("#timesheet-information").submit(function (e) {
        e.preventDefault();
        
        require('aui/flag')({
            type: 'info',
            title: 'Page will be reloaded',
            body: '<p>Page will be loaded soon. Please wait...</p>' +
            'You can <a href="javascript:window.location.reload();">quick reload</a> by pressing the F5 key.',
            close: 'auto'
        });
        var timeSheetIDtoUse;
    	if (timesheetIDOfUser && isAdmin) 
    		timeSheetIDtoUse = timesheetIDOfUser;
         else 
        	timeSheetIDtoUse = timesheetID; 

        var timesheetFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'timesheets/' + timeSheetIDtoUse,
            contentType: "application/json"
        });
        
      //browser reload
        window.setTimeout(function () {
            location.reload()
        }, 4000);
            
        AJS.$.when(timesheetFetched)
            .done(function (existingTimesheetData) {
            	var lectures = "";

//                console.log("elements");
//                console.log(AJS.$("[name=timesheet-hours-lectures]"));

                for(var i = 0; i < AJS.$("[name=timesheet-hours-lectures]").length; i++){
                    var item = AJS.$("[name=timesheet-hours-lectures]")[i];

                    if(i == 0)
                        lectures += AJS.$(item).val();
                    else
                        lectures += "@/@" +  AJS.$(item).val();
                }

                var timesheetUpdateData = {
                    timesheetID: existingTimesheetData.timesheetID,
                    lectures: lectures,
                    reason: AJS.$("#timesheet-substract-hours-text").val(),
                    targetHourPractice: toFixed(AJS.$("#timesheet-hours-practical").val(), 2),
                    targetHours: AJS.$("#timesheet-hours-text").val(),
                    targetHoursCompleted: toFixed(AJS.$("#timesheet-hours-practical").val() - AJS.$("#timesheet-hours-substract").val(), 2),
                    targetHoursRemoved: toFixed(AJS.$("#timesheet-hours-substract").val(), 2),
                    latestEntryDate: existingTimesheetData.latestEntryDate,
                    state: existingTimesheetData.state
                };

                var timesheetUpdated = AJS.$.ajax({
                    type: 'POST',
                    url: restBaseUrl + 'timesheets/update/' + existingTimesheetData.timesheetID,
                    contentType: "application/json",
                    data: JSON.stringify(timesheetUpdateData)
                });
                
              //browser reload
                window.setTimeout(function () {
                    location.reload()
                }, 4000);
                
                AJS.$.when(timesheetUpdated)
                    .fail(function (error) {
                        AJS.messages.error({
                            title: 'There was an error while updating the timesheet data.',
                            body: '<p>Reason: ' + error.responseText + '</p>'
                        });
                    });
            })
            .fail(function () {
                AJS.messages.error({
                    title: 'There was an error while fetching existing timesheet data.',
                    body: '<p>Reason: ' + "Please use Google Chrome or Orpera" + '</p>'
                });
            });
        });


    timesheetIDOfUser = sessionStorage.getItem('timesheetID');
    if (timesheetIDOfUser) {
        fetchData(timesheetIDOfUser);
        var param = "?id=" + timesheetIDOfUser;

        var url_json = AJS.$("#download-json").attr("href");
        var url_csv = AJS.$("#download-csv").attr("href");

        AJS.$("#download-json").attr("href", url_json + param);
        AJS.$("#download-csv").attr("href", url_csv + param);
    }
    else {
        fetchData(timesheetID);

    }

    hideGoogleDocsImportButtonWhenDisabled();
});

function hideGoogleDocsImportButtonWhenDisabled() {
    AJS.$.ajax({
        type: "GET",
        url: restBaseUrl + "config/isGoogleDocsImportEnabled",
        success: function (enabled) {
            if (!enabled) {
                AJS.$("#button-wrapper").hide();
            }
        }
    });
}

// outerHTML "<span class=\"td ticket\">ATLDEV-250: Create User Helper - Mandatory Fields broken,ATLDEV-252: Clean up Timesheet plugin code from unneeded libraries/dependencies and so on</span>"
// text "ATLDEV-250: Create User Helper - Mandatory Fields broken,ATLDEV-252: Clean up Timesheet plugin code from unneeded libraries/dependencies and so on"
function replaceJiraTicketLinks() {
    
	var tickets = AJS.$(".td.ticket a");    
//    console.log("tickets: ", tickets);   
    
    for (var i = 0; i < tickets.length; i++) {
        var ticket = tickets[i];
        
        if (ticket.text == "None") {
            AJS.$(ticket).removeAttr("href");
        }
        else if (ticket.text.includes("  ---  ")) {
        	
        	var ticketsKeyStringArray = [];
        	var thisTicketStringArray = ticket.text.split("  ---  ");
        	
        	for (var j = 0; j < thisTicketStringArray.length; j++) {
        		
        		var thisTicketString = thisTicketStringArray[j];
        		var ticketKey = "";
        		
        		if (thisTicketString.includes(" : ")) {
        			ticketKey = thisTicketString.split(" : ")[0];
        			ticketsKeyStringArray.push(ticketKey);
        		}
        		else {
        			ticketKey = thisTicketString;
        			ticketsKeyStringArray.push(ticketKey);
        		}        		       		
        	}
        	//jql=key in (atldev-225%2C atldev-224)
        	AJS.$(ticket).attr("href", baseUrl + "/issues/?jql=key in (" + ticketsKeyStringArray.toString() + ")");       	     	
        }
        else {
        	AJS.$(ticket).attr("href", baseUrl + "/browse/" + ticket.text.split(" ")[0]);
        }
    }
}


function checkConstrains() {
    return AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'checkConstrains',
        success: function (isValid) {

            if (isValid == true) {
                AJS.$("#timesheet-table").show();
                AJS.$("#table-header").show();
            }
            else {
                AJS.$("#timesheet-table").hide();
                AJS.$("#table-header").hide();

                AJS.messages.error({
                    title: 'Sorry, you do not have a timesheet!',
                    body: 'You are not authorized to view this page. Please contact the administrator! </br> ' +
                    'Probably you are not added to a team or category or both!'
                });
            }
        }
    }).responseText;
}

function projectedFinishDate(timesheetData, entryData) {
	
    var timesheet = timesheetData[0];
    var entries = entryData[0];
    var rem = timesheet.targetHours - timesheet.targetHoursCompleted + timesheet.targetHoursRemoved;
    if (rem <= 0) {
        AJS.$("#timesheet-finish-date").val(new Date().toLocaleDateString("en-US"));
        return;
        // already finished...
    }
    var sumLast30Days = 0;
    var days30Past = new Date();
    days30Past.setDate(days30Past.getDate() - 30);
    for (var i = 0; i < entries.length; i++) {
        var entry = entries[i];
        if (entry.beginDate > days30Past) {
            sumLast30Days += entry.endDate - entry.beginDate;
        }
    }
    var hoursLast30Days = sumLast30Days / (1000 * 60 * 60);
    var remDays = rem * 30 / hoursLast30Days;
    var finishDate = new Date();
    finishDate.setDate(finishDate.getDate() + remDays);
    AJS.$("#timesheet-finish-date").val(finishDate.toLocaleDateString("en-US"));
}

function setOwnerLabel(timesheet) {

	AJS.$("#timesheet-owner").empty();
	AJS.$("#timesheet-owner").append("Timesheet of: " + timesheet.displayName);
    AJS.$("#timesheet-owner-private").empty();
	AJS.$("#timesheet-owner-private").append("My Timesheet (" + timesheet.displayName + ")"); 
}

function setDownloadLink(timesheet) {
	
    AJS.$("#download-csv").attr("href", "download/timesheet");
    AJS.$("#download-json").attr("href", "download/timesheet/json");    
}

function fetchData(timesheetID) {

    clearDiagramSelections();

    var timesheetFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID,
        contentType: "application/json"
    });

    var entriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID + '/entries',
        contentType: "application/json"
    });

    var teamEntries = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheet/' + timesheetID + '/teamEntries',
        contentType: "application/json"
    });

    var categoriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'config/getCategories',
        contentType: "application/json"
    });

    var teamsFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'teams/' + timesheetID,
        contentType: "application/json"
    });

    var pairProgrammingFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getPairProgrammingUsers',
        contentType: "application/json"
    });

    AJS.$.when(timesheetFetched)
        .done(setOwnerLabel)
    	.done(setDownloadLink);

    AJS.$.when(timesheetFetched, entriesFetched)
        .done(projectedFinishDate);

    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched, pairProgrammingFetched)
        .done(assembleTimesheetData)
        .done(populateTable, prepareImportDialog)
        .done(assembleTimesheetVisData)
        .done(populateVisTable)
        .done(assignTeamAndHoursDataToPiChart)
        .done(assignCategoryDiagramData)
        .done(replaceJiraTicketLinks)

        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });

    AJS.$.when(teamEntries, categoriesFetched, teamsFetched, entriesFetched)
        .done(assembleTimesheetVisData)
        .done(computeTeamDiagramData)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });

    var interval_id = setInterval(function () {
        if(typeof timesheetData_ !== "undefined"){
            clearInterval(interval_id);
            loadMostActiveTeamForUser();
        }
    }, 200);

    stopSpin();
}

function loadMostActiveTeamForUser(){
    AJS.$.ajax({
        type : "GET",
        url : restBaseUrl + "getMostActiveTeamForUser/" + timesheetData_.userKey,
        success : function (data) {
//            console.log("Team fetched: " + data.teamName);
            setTeamVisInfoHeaders(data.teamName);
        }
    });
}


function fetchUsers() {
    var config = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'config/getConfig',
        contentType: "application/json"
    });

    var jsonUser = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/owner/' + timesheetID,
        contentType: "application/json"
    });

    var userList = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getUsers',
        contentType: "application/json"
    });
    
    AJS.$.when(config, jsonUser, userList)
        .done(initCoordinatorTimesheetSelect)

        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching user data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });

    if(isAdmin) {
        var totalUserList = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'user/getUserInformation',
            contentType: "application/json"
        });

        AJS.$.when(totalUserList)
            .done(initTimesheetAdminUserList)
            .fail(function (error) {
                AJS.messages.error({
                    title: 'There was an error while fetching user data for timesheet admin.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
            });
    }

    if(isCoordinator) {
    	
    	var currentTimesheetID;
    	var pathname = window.location.href;

    	if (pathname.includes("timesheetID=")) {
    		currentTimesheetID = pathname.split("timesheetID=")[1];
    		console.log("user/getUsersForCoordinator/  ", currentTimesheetID);
    	}   	
    	
        var userInformation = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'user/getUsersForCoordinator/' + currentTimesheetID,
            contentType: "application/json"
        });

        AJS.$.when(userInformation)
            .done(initCoordinatorUserList)
            .fail(function (error) {
                AJS.messages.error({
                    title: 'There was an error while fetching team user data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
            });
    }
}

function assembleTimesheetData(timesheetReply, categoriesReply, teamsReply, entriesReply, pairProgrammingReply) {
    timesheetData_ = timesheetReply[0];

    timesheetData_.entries = entriesReply[0];
    timesheetData_.categoryIDs = [];
    timesheetData_.teams = [];
    timesheetData_.pairProgrammingGroup = pairProgrammingReply[0];

    categoriesReply[0].map(function (category) {
        timesheetData_.categoryIDs[category.categoryID] = {
            categoryName: category.categoryName
        };
    });

    teamsReply[0].sort();
    teamsReply[0].map(function (team) {
        timesheetData_.teams[team.teamID] = {
            teamName: team.teamName,
            teamCategories: team.categoryIDs
        };
    });

    initTimesheetInformationValues(timesheetData_);
    return timesheetData_;
}

// first we have to clear the lineDiagram sections before we can repaint the lineDiagram.
function clearDiagramSelections() {
    //summary lineDiagram
    AJS.$("#piChartDiagramHoursPerCategory").empty();
    AJS.$("#piChartDiagramHoursPerTeam").empty();

    // user time visualization
    AJS.$("#lineDiagram").empty();
    AJS.$("#categoryLineDiagram").empty();

    //team visualization
    AJS.$("#teamDataDiagram").empty();
    AJS.$("#teamLineDiagram").empty();
}
