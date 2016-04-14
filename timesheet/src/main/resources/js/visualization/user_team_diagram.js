"use strict";

//reverse order of the table from bottom to top
function assignTeamVisData(timesheetData) {

    var availableEntries = timesheetData;

    var pos = availableEntries.length - 1;
    var i = availableEntries.length - 1;
    //variables for the time calculation
    var totalHours = 0;
    var totalMinutes = 0;
    var totalTimeHours = 0;
    var totalTimeMinutes = 0;
    //save data in an additional array
    var dataPoints = [];

    while (i >= 0) {
        var referenceEntryDate = new Date(availableEntries[pos].beginDate);
        var compareToDate = new Date(availableEntries[i].beginDate);
        var oldPos = pos;

        if ((referenceEntryDate.getFullYear() === compareToDate.getFullYear()) &&
            (referenceEntryDate.getMonth() === compareToDate.getMonth())) {
            totalHours = 0;
            totalMinutes = 0;
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

            //add points
            var dataX = referenceEntryDate.getFullYear() + "-" + (referenceEntryDate.getMonth() + 1);
            var dataY = totalHours + totalMinutes / 60;
            dataPoints.push(dataX);
            dataPoints.push(dataY);
            dataPoints.push(timesheetData.categories[availableEntries[i].categoryID].categoryName);
        } else {
            pos = i;
            i = i + 1;
        }

        if (oldPos != pos || i === 0) {
            if (totalTimeMinutes >= 60) {
                var minutesToFullHours = Math.floor(totalTimeMinutes / 60); //get only full hours
                totalTimeHours = totalTimeHours + minutesToFullHours;
                totalTimeMinutes = totalTimeMinutes - minutesToFullHours * 60;
            }
        }

        i = i - 1;
    }

    var categories = [];
    //filter all category names
    for (var i = 0; i < dataPoints.length; i++)
        //read category name at position 3
        if (i % 3 === 2)
            if (!containsElement(categories, dataPoints[i]))
                categories.push(dataPoints[i]);


    var sortedDataArray = [];
    var tempArray = [];

    for (var k = 0; k < categories.length; k++) {
        for (var i = 0; i < dataPoints.length; i++) {
            //fill in category name at first pos of subarray
            if (i === 0) {
                tempArray.push(categories[k]);
            }

            //read category name at position 3
            if ((i % 3 === 2) && (dataPoints[i] === categories[k])) {
                tempArray.push(dataPoints[i - 2]);
                tempArray.push(dataPoints[i - 1]);
            }

            //add subarray to array and pick next category
            if (i === dataPoints.length - 1) {
                sortedDataArray.push(tempArray);
                tempArray = [];
            }
        }
    }
    
    categoryDiagram(sortedDataArray, categories.length, true);
}