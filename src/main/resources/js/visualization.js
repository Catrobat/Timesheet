"use strict";
var orig;

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
    AJS.$("#visualization-table-content").empty();

    AJS.$("#piChartDiagramTag").empty();
    AJS.$("#piChartDiagram").empty();

    // user time visualization
    AJS.$("#lineDiagram").html("");
    AJS.$("#categoryLineDiagram").html("");

    var timesheetData = timesheetDataReply[0];
    appendEntriesToVisTable(timesheetData);
}

function computeTeamDiagramData(teamEntries) {
    assignTeamVisData(teamEntries[0]);
    assignTeamData(teamEntries[0]);
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
