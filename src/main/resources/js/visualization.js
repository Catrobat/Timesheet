"use strict";

function assembleTimesheetVisData(timesheetReply, categoriesReply, teamsReply, entriesReply) {
    var timesheetData = timesheetReply[0];

    if (typeof entriesReply != "undefined")
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

function setTeamVisInfoHeaders(team_name){
    document.getElementById("teamDiagramTag").innerHTML = "<strong>" + team_name + " Time Visualization</strong>";
    document.getElementById("teamLineDiagramTag").innerHTML = "<strong>" + team_name + " Category Visualization</strong>";
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
    document.getElementById("team-vis-select-container").style.display = "flex";
}

function spin(){
    document.getElementById("team-vis-spinner").style.visibility="visible";
}

function stopSpin(){
    document.getElementById("team-vis-spinner").style.visibility="hidden";
}

function initTeamVisOptions(teams){
    var select = document.getElementById("team-vis-select");
    select.options.length = 0;

    showTeamVisSelect();

    AJS.$("#team-vis-select").select2({
        placeholder: "Select gulasch",
        width : "25%"
    });

    teams.forEach(function (item) {
        select[select.options.length] = new Option(item.teamName, item.teamID);
    });

    setTeamVisListeners();
}

function setTeamVisListeners(){
    //clear the listeners first, at multible calls function will be called multible times and listeners will be appended
    AJS.$("#display-selected-team").off();

    AJS.$("#display-selected-team").on("click", function () {
        console.log("displaying team with id: " + AJS.$("#team-vis-select").val());
        var selected_team = AJS.$("#team-vis-select").select2("data");

        if(selected_team == null){
            AJS.messages.error({
                title : "Error: Invalid Selection",
                body : "Please select a Team and click Display"
            });
            return;
        }

        spin();

        var teamEntries = AJS.$.ajax({
            type: 'GET',
            url:  restBaseUrl + "/getTeamEntries/" + selected_team.id,
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

        AJS.$.when(teamEntries, categoriesFetched, teamsFetched)
            .done(assembleTimesheetVisData)
            .done(computeTeamDiagramData)
            .done(function () {
                setTeamVisInfoHeaders(selected_team.text);
                stopSpin();
            })
            .fail(function (error) {
                AJS.messages.error({
                    title: 'There was an error while fetching Team data for: ' + AJS.$("#team-vis-select").select2("data").text,
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
                stopSpin();
            });
    });

    AJS.$("#reset-teamvis-selection").on("click", function (e) {
        fetchData(timesheetData_.timesheetID);
    })
}

function initTeamVisSelect(userType){
    if(userType === "admin") {
        AJS.$.ajax({
            type: "GET",
            url: restBaseUrl + "/config/getTeams",
            success: function (data) {
                console.log("Team request was successfull");
                initTeamVisOptions(data);
            },
            fail: function (err) {
                console.error(err.responseText);
            }
        })
    }else{
        AJS.$.ajax({
            type: "GET",
            url: restBaseUrl + "/getTeamsOfUser",
            success: function (data) {
                console.log("Team request was successfull");
                if(data.length > 1) {
                    initTeamVisOptions(data);
                }else{
                    console.log("We got no options except one team so no dropdown needed");
                }
            },
            fail: function (err) {
                console.error(err.responseText);
            }
        })
    }
}