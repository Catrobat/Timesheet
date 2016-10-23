"use strict";

function fetchVisData() {
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
        url: restBaseUrl + 'categoryIDs',
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
        .done(computeCategoryDiagramData)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function fetchTeamVisData() {
    //Block 1 bis 3 sinnlos, da bereits gefetched
    var entriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID + '/entries',
        contentType: "application/json"
    });

    var categoriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'categoryIDs',
        contentType: "application/json"
    });

    var teamsFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'teams',
        contentType: "application/json"
    });

    //Block 4 relevant, aber mapping ist seltsam allEntries -> timesheetData[0]
    var allTeamMemberEntriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheet/' + timesheetID + '/teamEntries',
        contentType: "application/json"
    });

    AJS.$.when(allTeamMemberEntriesFetched, categoriesFetched, teamsFetched, entriesFetched)
        .done(assembleTimesheetVisData) //TODO: macht keinen Sinn: Überprüfung notwendig!
        .done(computeTeamDiagramData) // TODO: Überprüfung notwendig! allEntries -> timesheetData[0] wrong mapping!!
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}


function assembleTimesheetVisData(timesheetReply, categoriesReply, teamsReply, entriesReply) {
    var timesheetData = timesheetReply[0];

    timesheetData.entries = entriesReply[0];
    timesheetData.categoryIDs = [];
    timesheetData.teams = [];
    timesheetData.categoryNames = [];

    categoriesReply[0].map(function (category) {
        timesheetData.categoryIDs[category.categoryID] = {
            categoryName: category.categoryName
        };
    });

    categoriesReply[0].forEach(function (category) {
        timesheetData.categoryNames[category.categoryName] = category.categoryID;
    });


    /*  timesheetData.categoryIDs.forEach(function (value, index) {
     console.log(value, index);
     });*/

    teamsReply[0].map(function (team) {
        timesheetData.teams[team.teamID] = {
            teamName: team.teamName,
            teamCategories: team.categoryIDs
        };
    });

    return timesheetData;
}


function populateVisTable(timesheetDataReply) {
    var timesheetData = timesheetDataReply[0];
    AJS.$("#visualization-table-content").empty();
    AJS.$("#piChartDiagramTag").empty();
    AJS.$("#piChartDiagram").empty();
    appendEntriesToVisTable(timesheetData);
}

// ToDO: 2. Parameter: unused ?!?
// eine funktion soll mehr als nur eine weitere Funktion beinhalten, kann gelöscht werden
function computeCategoryDiagramData(timesheetDataReply) {
    assignCategoryDiagramData(timesheetDataReply[0], false);
}

// ToDO:
function computeTeamDiagramData(timesheetDataReply) {
    assignTeamVisData(timesheetDataReply[0], true); // 1.Parameter: timesheetData
    assignTeamData(timesheetDataReply[0]); // aber hier 1. Parameter: entries - what the hell ?!?!
}

function calculateDuration(begin, end, pause) {
    var pauseDate = new Date(pause);
    return new Date(end - begin - (pauseDate.getHours() * 60 + pauseDate.getMinutes()) * 60 * 1000);
}

function containsElement(array, element) {
    for (var p in array)
        if (array[p] === element)
            return true;
    return false;
}
