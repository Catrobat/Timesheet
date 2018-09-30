"use strict";

function appendTimeToHoursPerCategoryPiChart(sortedDataArray, numberOfCategories) {
    var data = [];
    var catNames = [];
    for (var i = 0; i < numberOfCategories; i++) {
        if (sortedDataArray[i][0] == "Inactive" || sortedDataArray[i][0] == "Inactive & Offline") {
            continue;
        }
        var sum = 0;
        for (var k = 2; k < sortedDataArray[i].length; k+=2) {
            sum += sortedDataArray[i][k];
        }

        catNames.push(sortedDataArray[i][0]);		
		data.push(toFixed(sum, 2));
    }

    var piChart = drawHoursPerCategoryPiChart(catNames, data);
}

function appendHoursPerTeamToPiChart(teamAndHoursPerTeamArray) {
	
	// 0 Team - 1 Hours; 2 Team - 3 Hours; ...
	var data = [];
	var teamNames = [];
	
	for (var i = 1; i < teamAndHoursPerTeamArray.length; i+=2) {				
//		console.log("TEAM teamAndHoursPerTeamArray[i]: ", teamAndHoursPerTeamArray[i-1]);
//		console.log("HOURS teamAndHoursPerTeamArray[i+1]: ", teamAndHoursPerTeamArray[i]);		
		teamNames.push(teamAndHoursPerTeamArray[i-1]);		
		data.push(toFixed(teamAndHoursPerTeamArray[i], 2));
	}
	
//	console.log("LABELS(teamNames): ", teamNames);
//	console.log("DATA: ", data);
	
	var piChartHoursPerTeam = drawHoursPerTeamPiChart(teamNames, data);
}

