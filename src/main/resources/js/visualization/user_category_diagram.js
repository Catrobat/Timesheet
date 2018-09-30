"use strict";

function assignTeamAndHoursDataToPiChart (timesheetData) {
	
	var teamAndHoursPerTeamArray = [];
	var availableEntries = timesheetData[0].entries;
//	console.log("timesheetData[0] ", timesheetData[0]);
//	console.log("timesheetData[0].teams ", timesheetData[0].teams);
    var teamsHavingEntries = timesheetData[0].teams;
    
    for (var key in teamsHavingEntries) {
        if (key === 'length' || !teamsHavingEntries.hasOwnProperty(key)) continue;
        var thisTeam = teamsHavingEntries[key];
//        console.log("key: ", key);
//        console.log("thisTeam: ", thisTeam);
//        console.log("thisTeamName: ", thisTeam.teamName);
        
//        for each entry, if entry teamId == key, count
        var i = availableEntries.length - 1;
        var totalHoursPerTeam = 0;
        var totalMinutesPerTeam = 0;
        var totalTimePerTeam = 0;
        
        while (i >= 0) {
        	
        	if (availableEntries[i].teamID == key) {
        		
//        		console.log("1 availableEntries[i].teamID: " + availableEntries[i].teamID);
//        		console.log("1 key: ", key);
                var hours = calculateDuration(availableEntries[i].beginDate, availableEntries[i].endDate,
            		    availableEntries[i].pauseMinutes).getHours();
        		var minutes = calculateDuration(availableEntries[i].beginDate, availableEntries[i].endDate,
        		    availableEntries[i].pauseMinutes).getMinutes();
        		var pause = availableEntries[i].pauseMinutes;
        		var calculatedTime = hours * 60 + minutes - pause;		
        		totalMinutesPerTeam = totalMinutesPerTeam + calculatedTime;
        		
        		if (totalMinutesPerTeam >= 60) {
        		    var minutesToFullHours = Math.floor(totalMinutesPerTeam / 60); //get only full hours
        		    
        		    totalHoursPerTeam = totalHoursPerTeam + minutesToFullHours;
        		    totalMinutesPerTeam = totalMinutesPerTeam - minutesToFullHours * 60;
        		}
        	}
        	i = i - 1;
        }
        
        totalTimePerTeam = totalHoursPerTeam + totalMinutesPerTeam / 60;
        
        // per team, push!
        teamAndHoursPerTeamArray.push(thisTeam.teamName);
    	teamAndHoursPerTeamArray.push(totalTimePerTeam);
//    	console.log("thisTeam.teamName: ", thisTeam.teamName);
//    	console.log("totalTimePerTeam: ", totalTimePerTeam);
    }

    appendHoursPerTeamToPiChart(teamAndHoursPerTeamArray);
}


//reverse order of the table from bottom to top
function assignCategoryDiagramData(timesheetData) {

    var availableEntries = timesheetData[0].entries;

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
            dataPoints.push(timesheetData[0].categoryIDs[availableEntries[i].categoryID].categoryName);
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

    categoryDiagram(sortedDataArray, categories.length, false);

    appendTimeToHoursPerCategoryPiChart(sortedDataArray, categories.length);
}

function categoryDiagram(sortedDataArray, numberOfCategories, isTeamDiagram) {
    var data = {};
    //create data json array dynamically
    data['label'] = [];
    data['year'] = [];
    for (var i = 0; i < numberOfCategories; i++) {
        //console.log(sortedDataArray[i]);
        //labels
        if (!containsElement(data['label'], sortedDataArray[i][0]))
            data['label'].push(sortedDataArray[i][0]);
        //years
        for (var j = 1; j < sortedDataArray[i].length - 1; j = j + 2)
            if (!containsElement(data['year'], sortedDataArray[i][j]))
                data['year'].push(sortedDataArray[i][j]);
        //values
        data['category' + i] = [];
        for (var l = 0; l < data['year'].length; l++) {
            var sum = 0;
            for (var k = 1; k < sortedDataArray[i].length; k++) {
                if (sortedDataArray[i][k] == data['year'][l])
                    sum = sum + sortedDataArray[i][k + 1];
            }
            data['category' + i].push(toFixed(sum, 2));
        }
    }

    var dataJSON = [];
    var tempData = [];
    //build data JSON object (year is represented on the x-axis; time on the y-axis
    for (var i = 0; i < data['year'].length; i++) {
        for (var key in data) {
            var obj = data[key];
            tempData.push(obj[i]);
        }
        //console.log(tempData);
        //fill JSON array
        dataJSON.push({
            year: tempData[1],
            cat1: tempData[2],
            cat2: tempData[3],
            cat3: tempData[4],
            cat4: tempData[5],
            cat5: tempData[6],
            cat6: tempData[7],
            cat7: tempData[8],
            cat8: tempData[9],
            cat9: tempData[10],
            cat10: tempData[11],
            cat11: tempData[12],
            cat12: tempData[13],
            cat13: tempData[14],
            cat14: tempData[15],
            cat15: tempData[16]
        });

        tempData = [];
    }
    if (isTeamDiagram) {
        var teamDiagram = drawTeamDiagram(dataJSON, data['label']);

        AJS.$("#table-header > ul > li > a").bind("tabSelect", function(e, o) {
            if (o.tab.attr("href") == "#tabs-team") {
                teamDiagram.redraw();
            }
        });
    } else {
        var categoryDiagram = drawCategoryDiagram(dataJSON, data['label']);

        AJS.$("#table-header > ul > li > a").bind("tabSelect", function(e, o) {
            if (o.tab.attr("href") == "#tabs-line") {
                categoryDiagram.redraw();
            }
        });
    }
}