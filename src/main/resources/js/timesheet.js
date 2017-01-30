"use strict";

var restBaseUrl;
var hostname = location.hostname;
var baseUrl;
var timesheetData_;
var categoryData_;
var teamData_;
var entryData_;
var userData_;

var timesheetIDOfUser;

AJS.toInit(function () {
    if (hostname.includes("catrob.at")) {
        baseUrl = "https://" + hostname;
    }
    else {
        baseUrl = AJS.params.baseURL;
    }

    restBaseUrl = baseUrl + "/rest/timesheet/latest/";

    var isFirefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
    if (isFirefox) {
        AJS.messages.error({
            title: 'Sorry, Firefox is not supported at the moment!',
            body: 'Firefox has an critical bug so would you be so kind and take a different browser. We suggest Chrome as perfect search engine.'
        });
        return;
    }

    // printDomainAttributes();

    AJS.$("#timesheet-table").hide();
    AJS.$("#table-header").hide();
    checkConstrains();

    if (isCoordinator || isSupervisor) {
        fetchUsers();
        AJS.$("#coord_private").show();
        AJS.$("#coord_private_table").show();
    } else {
        AJS.$("#coord_private").hide();
        AJS.$("#coord_private_table").hide();
    }

    if (isMasterThesisTimesheet) {
        document.getElementById("tabs-timesheet-settings").style.display = "none";
        document.getElementById("tabs-team").style.display = "none";
        AJS.$("#download-csv").attr("href", "download/timesheet/masterthesis");
        AJS.$("#download-json").attr("href", "download/timesheet/json/masterthesis");

        AJS.$("#theory-hour-key-data").show();
    } else {
        AJS.$("#download-csv").attr("href", "download/timesheet");
        AJS.$("#download-json").attr("href", "download/timesheet/json");

        AJS.$("#theory-hour-key-data").hide();
    }

    if (isAdmin) {
        AJS.$("#timesheet-hours-save-button").hide();
        AJS.$("#timesheet-hours-update-button").show();
    } else {
        initUserSaveButton();
        AJS.$("#timesheet-hours-save-button").show();
        AJS.$("#timesheet-hours-update-button").hide();
    }

    timesheetIDOfUser = sessionStorage.getItem('timesheetID');
    if (timesheetIDOfUser) {
        fetchData(timesheetIDOfUser);
        var param = "?id=" + timesheetIDOfUser;
        var url_json = AJS.$("#download-json").attr("href");
        var url_csv = AJS.$("#download-csv").attr("href");
        AJS.$("#download-json").attr("href", url_json + param);
        AJS.$("#download-csv").attr("href", url_csv + param);

        //defining behaviour: after double refresh original data are shown
        sessionStorage.removeItem('selectedUser');
        sessionStorage.removeItem('isMTSheetSelected');
        sessionStorage.removeItem('timesheetID');
    }
    else {
        fetchData(timesheetID);
    }
});

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
                    'Probably you are not added to a team or category!'
                });
            }
        }
    }).responseText;
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
        url: restBaseUrl + 'categoryIDs',
        contentType: "application/json"
    });

    var teamsFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'teams/' + timesheetID,
        contentType: "application/json"
    });

    var usersFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getUsers',
        contentType: "application/json"
    });

    var pairProgrammingFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getPairProgrammingUsers',
        contentType: "application/json"
    });


    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched, usersFetched, pairProgrammingFetched)
        .done(assembleTimesheetData)
        .done(populateTable, prepareImportDialog)
        .done(assembleTimesheetVisData)
        .done(populateVisTable)
        .done(assignCategoryDiagramData)

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

}

function saveTimesheetIDOfUserInSession(selectedUser, isMTSheet) {
    AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheet/timesheetID/' + selectedUser[0] + '/' + isMTSheet,
        contentType: "application/json",
        success: function (timesheetID) {
            sessionStorage.setItem('timesheetID', timesheetID); // defining the session variable
        },
        error: function (error) {
            AJS.messages.error({
                title: 'There was an error while getting timesheet data of another user.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        }
    });
}

function getExistingTimesheetHours(timesheetID) {
    var timesheetFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID,
        contentType: "application/json"
    });
    AJS.$.when(timesheetFetched)
        .done(updateTimesheetHours)
        .done(location.reload())
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching existing timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function updateTimesheetHours(existingTimesheetData) {
    var timesheetUpdateData = {
        timesheetID: existingTimesheetData.timesheetID,
        lectures: AJS.$("#timesheet-hours-lectures").val(),
        reason: AJS.$("#timesheet-substract-hours-text").val(),
        ects: AJS.$("#timesheet-hours-ects").val(),
        targetHourPractice: toFixed(AJS.$("#timesheet-hours-practical").val(), 2),
        targetHourTheory: toFixed(AJS.$("#timesheet-target-hours-theory").val(), 2),
        targetHours: AJS.$("#timesheet-hours-text").val(),
        targetHoursCompleted: toFixed((AJS.$("#timesheet-hours-theory").val()
        - (-AJS.$("#timesheet-hours-practical").val()) - AJS.$("#timesheet-hours-substract").val()), 2),
        targetHoursRemoved: toFixed(AJS.$("#timesheet-hours-substract").val(), 2),
        isActive: existingTimesheetData.isActive,
        isAutoInactive: existingTimesheetData.isAutoInactive,
        isOffline: existingTimesheetData.isOffline,
        isAutoOffline: existingTimesheetData.isAutoOffline,
        isEnabled: existingTimesheetData.isEnabled,
        isMTSheet: existingTimesheetData.isMTSheet,
        state: existingTimesheetData.state
    };

    AJS.$.ajax({
        type: 'POST',
        url: restBaseUrl + 'timesheets/update/' + existingTimesheetData.timesheetID + '/' + existingTimesheetData.isMTSheet,
        contentType: "application/json",
        data: JSON.stringify(timesheetUpdateData)
    })
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while updating the timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
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
        .done(initTimesheetAdminTimesheetSelect)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching user data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });

    var userInformation = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getUsersForCoordinator',
        contentType: "application/json"
    });

    AJS.$.when(userInformation)
        .done(initCoordinatorUserList)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while team user data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function assembleTimesheetData(timesheetReply, categoriesReply, teamsReply, entriesReply, usersReply, pairProgrammingReply) {
    timesheetData_ = timesheetReply[0];
    categoryData_ = categoriesReply[0];
    teamData_ = teamsReply[0];
    entryData_ = entriesReply[0];
    userData_ = usersReply[0];

    timesheetData_.entries = entriesReply[0];
    timesheetData_.categoryIDs = [];
    timesheetData_.teams = [];
    timesheetData_['users'] = [];
    timesheetData_.pairProgrammingGroup = pairProgrammingReply[0];

    //fill user names
    for (var i = 0; i < usersReply[0].length; i++) {
        if (usersReply[0][i]['active'])
            timesheetData_['users'].push(usersReply[0][i]['userName']);
    }

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
    updateTimesheetInformationValues(timesheetData_);

    return timesheetData_;
}

// first we have to clear the lineDiagram sections before we can repaint the lineDiagram.
function clearDiagramSelections() {

    //summary lineDiagram
    AJS.$("#piChartDiagram").empty();

    // user time visualization
    AJS.$("#lineDiagram").empty();
    AJS.$("#categoryLineDiagram").empty();

    //team visualization
    AJS.$("#teamDataDiagram").empty();
    AJS.$("#teamLineDiagram").empty();
}
