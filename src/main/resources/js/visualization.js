"use strict";

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
    assignTeamVisDiagramData(teamEntries[0]);
    assignTeamVisCategoryDiagramData(teamEntries[0]);
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

function showTeamVisSelect(){
    document.getElementById("team-vis-select-container").style.display = "block";
}

function initTeamVisOptions(teams){
    var select = document.getElementById("team-vis-select");
    select.options.length = 0;

    AJS.$("#team-vis-select").select2({
        width : "25%"
    });

    teams.forEach(function (item) {
        select[select.options.length] = new Option(item.teamName, item.teamID);
    });

    AJS.$("#display-selected-team").on("click", function () {
        console.log("displaying team with id: " + AJS.$("#team-vis-select").val());

        AJS.$.ajax({
            type : "GET",
            url : restBaseUrl + "/getTeamEntries/" + AJS.$("#team-vis-select").val(),
            success : function (entries) {
                console.log("request was successfull");
                console.log(JSON.stringify(entries));
                assignTeamVisDiagramData(entries);
                assignTeamVisCategoryDiagramData(entries);
            },
            fail : function (err) {
                console.error(err.responseText);
            }
        })
    });

    showTeamVisSelect();
}

function initTeamVisSelect(){
    AJS.$.ajax({
        type : "GET",
        url : restBaseUrl + "/config/getTeams",
        success: function (data) {
            console.log("Team request was successfull");
            initTeamVisOptions(data);
        },
        fail : function (err) {
            console.error(err.responseText);
        }
    })
}