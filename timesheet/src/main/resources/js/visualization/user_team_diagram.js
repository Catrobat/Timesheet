"use strict";

function assignTeamVisData(timesheetDataReply) {
    var availableEntries = timesheetDataReply[0].entries;
    var availableTeams = timesheetDataReply[0].teams;

    var pos = 0;
    //variables for the time calculation
    var totalHours = 0;
    var totalMinutes = 0;

    //data array
    var data = {};
    data['label'] = [];
    data['year'] = [];
    data['team'] = [];

    for (var i = 0; i < availableEntries.length; i++) {
        //calculate spent time for team
        var referenceEntryDate = new Date(availableEntries[pos].beginDate);
        var compareToDate = new Date(availableEntries[i].beginDate);
        var actualTeamID = availableEntries[pos].teamID;
        var oldPos = pos;


        if ((referenceEntryDate.getFullYear() == compareToDate.getFullYear()) &&
            (referenceEntryDate.getMonth() == compareToDate.getMonth()) &&
            (availableEntries[i].teamID == actualTeamID)) {
            //add all times for the same year-month pairs
            var hours = calculateDuration(availableEntries[i].beginDate, availableEntries[i].endDate,
                availableEntries[i].pauseMinutes).getHours();
            var minutes = calculateDuration(availableEntries[i].beginDate, availableEntries[i].endDate,
                availableEntries[i].pauseMinutes).getMinutes();
            var pause = availableEntries[i].pauseMinutes;
            var calculatedTime = hours * 60 + minutes - pause;

            totalMinutes = totalMinutes + calculatedTime;

            if (totalMinutes >= 60) {
                var minutesToFullHours = Math.floor(totalMinutes / 60); //get only full hours
                totalHours = totalHours + minutesToFullHours;
                totalMinutes = totalMinutes - minutesToFullHours * 60;
            }

        } else {
            pos = i;
            i = i - 1;
        }

        if (oldPos != pos || i == availableEntries.length - 1) {
            data['year'].push(referenceEntryDate.getFullYear() + "-" + (referenceEntryDate.getMonth() + 1));
            data['team'].push(toFixed(totalHours + totalMinutes / 60), 2);
            data['label'].push(availableTeams[actualTeamID].teamName);
            totalHours = 0;
            totalMinutes = 0;
        }
    }

    var temp = []
    //build data JSON object (year is represented on the x-axis; time on the y-axis
    for (var i = 0; i < data['year'].length; i++) {
        temp.push(data['year'][i]);
        temp.push(data['label'][i]);
        temp.push(data['team'][i]);
    }

    var dataJSON = [];
    for (var i = 0; i < temp.length; i = i + 3) {
        if (i % 2 == 0)
            dataJSON.push({
                year: temp[i],
                team1: temp[i + 2],
                team2: 0
            });
        else
            dataJSON.push({
                year: temp[i],
                team1: 0,
                team2: temp[i + 2]
            });
    }

    drawTeamDiagram(dataJSON, data['label']);
}