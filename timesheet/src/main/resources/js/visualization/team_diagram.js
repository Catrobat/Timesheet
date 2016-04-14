"use strict";

function assignTeamData(entries) {
    AJS.$("#teamDataDiagram").empty();
    var availableEntries = entries;

    var pos = availableEntries.length - 1;
    //variables for the time calculation
    var totalHours = 0;
    var totalMinutes = 0;

    //data array
    var dataPoints = [];

    for (var i = availableEntries.length - 1; i >= 0; i--) {
        //calculate spent time for team
        var referenceEntryDate = new Date(availableEntries[pos].beginDate);
        var compareToDate = new Date(availableEntries[i].beginDate);
        var oldPos = pos;

        if ((referenceEntryDate.getFullYear() == compareToDate.getFullYear()) &&
            (referenceEntryDate.getMonth() == compareToDate.getMonth())) {
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
            i = i + 1;
        }

        if (oldPos != pos || i == 0) {
            var dataX = referenceEntryDate.getFullYear() + "-" + (referenceEntryDate.getMonth() + 1);
            var dataY = toFixed((totalHours + totalMinutes) / 60, 2);
            dataPoints.push(dataX);
            dataPoints.push(dataY);

            totalHours = 0;
            totalMinutes = 0;
        }
    }

    //assign JSON data for line graph
    teamDiagram(dataPoints);
}

function teamDiagram(dataPoints) {
    var data = [];
    for (var i = 0; i < dataPoints.length; i = i + 2) {
        data.push({
            year: dataPoints[i],
            value: dataPoints[i + 1]
        });
    }
    drawSelectedTeamDiagram(data);
}