"use strict";

function appendEntriesToVisTable(timesheetData) {

    var availableEntries = timesheetData.entries;

    var pos = 0;
    var i = 0;
    //variables for the time calculation
    var totalHours = 0;
    var totalMinutes = 0;
    var totalTimeHours = 0;
    var totalTimeMinutes = 0;
    var timeLastSixMonthHours = 0;
    var timeLastSixMonthMinutes = 0;
    //save data in an additional array
    var count = 0;
    var dataPoints = [];
    //pi chart variables
    var theoryHours = 0;

    //spent time within the last six months
    var sixMonthAgo = new Date().getMonth() - 6;

    while (i < availableEntries.length) {
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

            //calculate theory time in minutes
            if ((timesheetData.categoryIDs[availableEntries[i].categoryID].categoryName === "Theory") ||
                (timesheetData.categoryIDs[availableEntries[i].categoryID].categoryName === "Theory (MT)") ||
                (timesheetData.categoryIDs[availableEntries[i].categoryID].categoryName === "Research"))
                theoryHours = theoryHours + calculatedTime;

            //date within the last six months
            var compareEntryMonth = compareToDate.getMonth() - 6;
            if (compareEntryMonth <= sixMonthAgo) {
                timeLastSixMonthHours = timeLastSixMonthHours + hours;
                timeLastSixMonthMinutes = timeLastSixMonthMinutes + minutes;

                if (timeLastSixMonthMinutes >= 60) {
                    var minutesToFullHours = Math.floor(timeLastSixMonthMinutes / 60); //get only full hours
                    timeLastSixMonthHours = timeLastSixMonthHours + minutesToFullHours;
                    timeLastSixMonthMinutes = timeLastSixMonthMinutes - minutesToFullHours * 60;
                }
            }
        } else {
            pos = i;
            i = i - 1;
        }

        if (oldPos != pos || i == availableEntries.length - 1) {
            //add points to line lineDiagram
            var dataX = referenceEntryDate.getFullYear() + "-" + (referenceEntryDate.getMonth() + 1);
            var dataY = toFixed(totalHours + totalMinutes / 60, 2);
            dataPoints.push(dataX);
            dataPoints.push(dataY);

            AJS.$("#visualization-table-content").append("<tr><td headers=\"basic-date\" class=\"date\">" +
                "Time Spent: " + referenceEntryDate.getFullYear() + "-" + (referenceEntryDate.getMonth() + 1) + "</td>" +
                "<td headers=\"basic-time\" class=\"time\">" + totalHours + "hours " + totalMinutes + "mins" + "</td>" +
                "</tr>");

            count++;

            //overall sum of spent time
            totalTimeHours = totalTimeHours + totalHours;
            totalTimeMinutes = totalTimeMinutes + totalMinutes;

            if (totalTimeMinutes >= 60) {
                var minutesToFullHours = Math.floor(totalTimeMinutes / 60); //get only full hours
                totalTimeHours = totalTimeHours + minutesToFullHours;
                totalTimeMinutes = totalTimeMinutes - minutesToFullHours * 60;
            }
            totalHours = 0;
            totalMinutes = 0;
        }

        i = i + 1;
    }

    var totalTime = totalTimeHours * 60 + totalTimeMinutes;

    //append total time
    AJS.$("#visualization-table-content").append("<tr><td headers=\"basic-date\" class=\"total-time\">" + "Total Time Spent" + "</td>" +
        "<td headers=\"basic-time\" class=\"time\">" + totalTimeHours + "hours " + totalTimeMinutes + "mins" + "</td>" +
        "</tr>");

    //entry for average time
    var averageMinutesPerMonth = (totalTimeHours * 60 + totalTimeMinutes) / count;
    var averageTimeHours = 0;
    var averageTimeMinutes = 0;

    if (averageMinutesPerMonth >= 60) {
        var minutesToFullHours = Math.floor(averageMinutesPerMonth / 60); //get only full hours
        averageTimeHours = minutesToFullHours;
        averageTimeMinutes = averageMinutesPerMonth - minutesToFullHours * 60;
    }

    //append avg time
    AJS.$("#visualization-table-content").append("<tr><td headers=\"basic-date\" class=\"avg-time\">" + "Time / Month" + "</td>" +
        "<td headers=\"basic-time\" class=\"time\">" + averageTimeHours + "hours " + Math.floor(averageTimeMinutes) + "mins" + "</td>" +
        "</tr>");

    //append time last 6 month
    AJS.$("#visualization-table-content").append("<tr><td headers=\"basic-date\" class=\"total-time\">" + "Overall Time Last 6 Month" + "</td>" +
        "<td headers=\"basic-time\" class=\"time\">" + timeLastSixMonthHours + "hours " + timeLastSixMonthMinutes + "mins" + "</td>" +
        "</tr>");

    appendTimeToPiChart(theoryHours, totalTime - theoryHours, totalTime);
    //assign JSON data for line graph
    lineDiagram(dataPoints);
}

