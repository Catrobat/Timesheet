"use strict";

var timesheetID;

AJS.toInit(function () {
    AJS.$.ajax({
        type: "GET",
        url: restBaseUrl + "config/isGoogleDocsImportEnabled",
        success: function (enabled) {
            setGoogleDocsImportToggleButtonState(enabled)
        }
    });

    var userList = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getUsers',
        contentType: "application/json"
    });

    AJS.$.when(userList).done(fillUserSelect);
});

function fillUserSelect(allUsers) {
    var userNameList = [];
    for (var i = 0; i < allUsers.length; i++) {
        userNameList.push(allUsers[i]['userName']);
    }
    AJS.$("#select-for-import").auiSelect2({
        placeholder: "Select User",
        tags: userNameList,
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });
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

    return timesheetData_;
}

function appendEntriesToTable() {
    // empty function since importer expects this
}

function prepareData(timesheetID) {
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

    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched, pairProgrammingFetched)
        .done(assembleTimesheetData)
        .done(prepareImportDialog);
}

function getTimesheetIDOfUser() {
    var importButton = AJS.$(".import-google-docs");
    var userName = AJS.$("#select-for-import").val();
    var isMTSheet = AJS.$("#is-mt-sheet").is(":checked");
    AJS.$.ajax({
        type: "GET",
        // url: restBaseUrl + "timesheet/of/" + userName + "/" + isMTSheet,
        url: restBaseUrl + "timesheet/timesheetID/" + userName + "/" + isMTSheet,
        contentType: "application/json",
        success: function (timesheetIDlocal) {
            timesheetID = timesheetIDlocal;
            importButton.show();
            importButton.text("Import for User: " + userName);
            importButton.prop("disabled", false);
            prepareData(timesheetID);
            AJS.messages.success({
                title: 'User correctly loaded.',
                body: '<p>Reason: Import can now be started.</p>'
            });
        },
        error: function(error) {
            AJS.messages.error({
                title: 'Error while getting the ID for the user. ',
                body: '<p>Reason: User does not seem to have a timesheet.</p>'
            });
        }
    });
}

function toggleGoogleDocsImport() {
    AJS.$.ajax({
        type: "GET",
        url: restBaseUrl + "config/toggleGoogleDocsImport",
        success: function (enabled) {
            setGoogleDocsImportToggleButtonState(enabled)
            AJS.messages.success({
                title: 'Google Docs Import state updated.',
                body: '<p>Reason: The import state has been toggled.</p>'
            });
        }
    });
}

function setGoogleDocsImportToggleButtonState(enabled) {
    if (enabled) {
        AJS.$("#toggle-google-docs").text("Disable GoogleDocs").css("background", "");
    } else {
        AJS.$("#toggle-google-docs").text("Enable GoogleDocs").css("background", "red");
    }
}