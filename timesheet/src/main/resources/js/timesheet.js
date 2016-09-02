"use strict";

var restBaseUrl;
var hostname = location.hostname;
var baseUrl;

AJS.toInit(function () {
    if (hostname.includes("catrob.at")) {
        baseUrl = "https://" + hostname;
    }
    else {
        baseUrl = AJS.params.baseURL;
    }

    restBaseUrl = baseUrl + "/rest/timesheet/latest/";

    console.log("restBaseUrl: " + restBaseUrl);

    AJS.$("#timesheet-export-csv-link").empty();

    // printDomainAttributes();

    AJS.$("#timesheet-table").hide();
    AJS.$("#table-header").hide();

    var isValid = checkConstrains();

    if (isValid == "true") {
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
        return;
    }

    if (!isCoordinator) {
        AJS.$("#coord_private").hide();
    }

    if (isMasterThesisTimesheet) {
        document.getElementById("tabs-timesheet-settings").style.display = "none";
        document.getElementById("tabs-team").style.display = "none";
        AJS.$("#timesheet-export-csv-link").append("<h2>Export</h2>Download 'Master Thesis Timesheet' as <a href=\"download/masterthesis\">CSV</a>.");
    } else {
        AJS.$("#timesheet-export-csv-link").append("<h2>Export</h2>Download 'Timesheet' as <a href=\"download/timesheet\">CSV</a>.");
    }

    AJS.$("#tabs-team").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === 'Visualize') {
            var selectedTeam = AJS.$("#select-team-select2-field").val().split(',');
            if (selectedTeam[0] !== "") {
                getDataOfTeam(selectedTeam[0]);
            }
        }
    });

    if (isAdmin) {
        hideVisualizationTabs();
        fetchUsers();
        AJS.$("#timesheet-hours-save-button").hide();
        AJS.$("#timesheet-hours-update-button").show();
    } else {
        //init coordinator/administrator/approved user Seetings
        initUserSaveButton();
        //fetch timesheet table data
        fetchUsers();
        fetchData();
        //fetch visualization data
        fetchVisData();
        fetchTeamVisData();
        AJS.$("#timesheet-hours-save-button").show();
        AJS.$("#timesheet-hours-update-button").hide();
    }

    initHiddenDialog();
});

function checkConstrains() {
    return AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'checkConstrains',
        async: false
    }).responseText;
}

function fetchUserTimesheetData(timesheetID) {

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

    var categoriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'categories',
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
    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched, usersFetched)
        .done(assembleTimesheetData)
        .done(populateTable, prepareImportDialog)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function fetchUserVisData(timesheetID) {
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

    var categoriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'categories',
        contentType: "application/json"
    });

    var teamsFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'teams',
        contentType: "application/json"
    });
    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched)
        .done(assembleTimesheetVisData)
        .done(populateVisTable)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function getDataOfTeam(teamName) {
    var teamData = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheet/' + teamName + '/entries',
        contentType: "application/json"
    });
    AJS.$.when(teamData)
        .done(assignTeamData)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching team data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function getTimesheetOfUser(selectedUser, isMTSheet) {
    var timesheetIDFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheet/timesheetID/' + selectedUser[0] + '/' + isMTSheet,
        contentType: "application/json"
    });
    AJS.$.when(timesheetIDFetched)
        .done(fetchUserTimesheetData)
        .done(fetchUserVisData)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while getting timesheet data of another user.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function getTimesheetByUser(selectedUser, isMTSheetSelected) {
    var timesheetFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheet/of/' + selectedUser + '/' + isMTSheetSelected,
        contentType: "applicatPion/json"
    });
    AJS.$.when(timesheetFetched)
        .done(updateTimesheetHours)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching existing timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function getExistingTimesheetHours(timesheetID) {
    var timesheetFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID,
        contentType: "applicatPion/json"
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
        targetHourTheory: toFixed(AJS.$("#timesheet-hours-theory").val(), 2),
        targetHours: AJS.$("#timesheet-hours-ects").val() * 30,
        targetHoursCompleted: toFixed((AJS.$("#timesheet-hours-theory").val()
        - (-AJS.$("#timesheet-hours-practical").val()) - AJS.$("#timesheet-hours-substract").val()), 2),
        targetHoursRemoved: toFixed(AJS.$("#timesheet-hours-substract").val(), 2),
        isActive: existingTimesheetData.isActive,
        isAutoInactive: existingTimesheetData.isAutoInactive,
        isOffline : existingTimesheetData.isOffline,
        isAutoOffline : existingTimesheetData.isAutoOffline,
        isEnabled: existingTimesheetData.isEnabled,
        isMTSheet: existingTimesheetData.isMTSheet
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
        .done(initApprovedUserTimesheetSelect)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching user data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function fetchData() {
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

    var categoriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'categories',
        contentType: "application/json"
    });

    var teamsFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'teams',
        contentType: "application/json"
    });
    var usersFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getUsers',
        contentType: "application/json"
    });
    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched, usersFetched)
        .done(assembleTimesheetData)
        .done(populateTable, prepareImportDialog)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

/*
function printDomainAttributes() {
    var baseUrl = AJS.params.baseURL;
    var hostname = AJS.$('<a>').prop('href', document.URL).prop('hostname');

    console.log("Base address: " + baseUrl);
    console.log("hostname: " + hostname);
    console.log("location.hostname: " + location.hostname);
    console.log("document.domain: " + document.domain);
    console.log("document.URL : " + document.URL);
    console.log("document.location.href : " + document.location.href);
    console.log("document.location.origin : " + document.location.origin);
    console.log("document.location.hostname : " + document.location.hostname);
    console.log("document.location.host : " + document.location.host);
    console.log("document.location.pathname : " + document.location.pathname);
    console.log("window.location.hostname : " + window.location.hostname);
}
*/


//neue funktionen .....
AJS.$(document).keydown(function (e) {
    var keyCode = e.keyCode || e.which;
    if (e.ctrlKey && e.altKey && e.shiftKey) {
        if (keyCode == 84) { // keycode == 't'
            AJS.dialog2("#hidden-dialog").show();
        } else {
            console.log("CTRL + ALT + SHIFT pressed");
        }
    }
    //console.log(event.keyCode);
});

function initHiddenDialog() {

    //global val
    var inactiveUsers = new Array();
    inactiveUsers.push("Markus Hobisch");

// Hides the dialog
    AJS.$("#dialog-submit-button").click(function (e) {
        e.preventDefault();
        AJS.dialog2("#hidden-dialog").hide();
    });

// Show event - this is triggered when the dialog is shown
    AJS.dialog2("#hidden-dialog").on("show", function () {
        AJS.$(".aui").focus();
        AJS.log("hidden-dialog was shown");
        AJS.$(".aui-dialog2-footer-hint").html("Created on " + new Date().toDateString());

        var content = "";
        for (var i = 0; i < inactiveUsers.length; i++) {
            content += inactiveUsers[i] + "<br/>";
        }
        AJS.$(".aui-dialog2-content").html(content);
    });

    AJS.$("#search-field").keyup(function (e) {
        var keyCode = e.keyCode || e.which;
        if (keyCode === 13) {
            e.preventDefault();
            return false;
        }
        var searchText = AJS.$("#search-field").val().toLowerCase();
        var content = "";
        for (var i = 0; i < inactiveUsers.length; i++) {
            if (inactiveUsers[i].toLowerCase().includes(searchText)) {
                content += inactiveUsers[i] + "<br/>";
            }
        }
        AJS.$(".aui-dialog2-content").html(content);
    });

//     // Hide event - this is triggered when the dialog is hidden
//     AJS.dialog2("#hidden-dialog").on("hide", function () {
//         AJS.log("hidden-dialog was hidden");
//     });
//
// // Global show event - this is triggered when any dialog is show
//     AJS.dialog2.on("show", function () {
//         AJS.log("a dialog was shown");
//     });
//
// // Global hide event - this is triggered when any dialog is hidden
//     AJS.dialog2.on("hide", function () {
//         AJS.log("a dialog was hidden");
//     });
}
