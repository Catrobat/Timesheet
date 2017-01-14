"use strict";

function assignTeamData(teamEntries) {
    AJS.$("#teamDataDiagram").empty();
    var availableEntries = teamEntries;

    var pos = availableEntries.length - 1;
    //variables for the time calculation
    var tmpHours = 0;
    var tmpMinutes = 0;
    var totalHours = 0;
    var totalMinutes = 0;

    //data array
    var data = {};
    data['year'] = [];
    data['dataX'] = [];
    data['dataY'] = [];

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

            tmpMinutes = tmpMinutes + calculatedTime;

            if (tmpMinutes >= 60) {
                var minutesToFullHours = Math.floor(tmpMinutes / 60); //get only full hours
                tmpHours = tmpHours + minutesToFullHours;
                tmpMinutes = tmpMinutes - minutesToFullHours * 60;
            }
        } else {
            pos = i;
            i = i + 1;
        }

        if (oldPos != pos || i == 0) {
            var dataX = referenceEntryDate.getFullYear() + "-" + (referenceEntryDate.getMonth() + 1); // time (year)
            var dataY = toFixed(tmpHours + (tmpMinutes / 60), 2); // total hours
            console.log("hours:", tmpHours);
            console.log("min", tmpMinutes);
            totalHours += tmpHours;
            totalMinutes += tmpMinutes;
            if (!containsElement(data['year'], dataX)) {
                data['year'].push(dataX);
            }

            tmpHours = 0;
            tmpMinutes = 0;
        }
    }

    console.log("dataY ", dataY);
    console.log(totalHours);
    console.log(totalMinutes);
    data['dataX'].push(dataX);
    data['dataY'].push(dataY);
    assignDataPoints(data);
}

function assignDataPoints(data) {
    var dataPoints = [];
    var sum = 0;

    for (var i = 0; i < data['year'].length; i++) {
        for (var j = 0; j < data['dataX'].length; j++) {
            if (data['dataX'][j] == data['year'][i]) {
                sum = sum + data['dataY'][j];
            }
        }
        console.log("sum:", sum);
        console.log("year: ", data['year'][i]);
        dataPoints.push(data['year'][i]);
        dataPoints.push(sum);
        sum = 0;
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
    data.forEach(function (ele) {
        console.log("value", ele.value);
    });
    drawSelectedTeamDiagram(data);
}