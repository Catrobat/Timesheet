"use strict";

function appendTimeToPiChart(sortedDataArray, numberOfCategories) {
    var data = [];
    for (var i = 0; i < numberOfCategories; i++) {
        if (sortedDataArray[i][0] == "Inactive" || sortedDataArray[i][0] == "Inactive & Offline") {
            continue;
        }
        var sum = 0;
        for (var k = 2; k < sortedDataArray[i].length; k+=2) {
            sum += sortedDataArray[i][k];
        }

        data.push({
            label: sortedDataArray[i][0],
            value: toFixed(sum, 2)
        });
    }

    var piChart = drawPiChart(data);

    AJS.$("#table-header > ul > li > a").bind("tabSelect", function(e, o) {
        if (o.tab.attr("href") == "#tabs-overview") {
            piChart.redraw();
        }
    });
}

function appendHoursPerTeamToPiChart(teamAndHoursPerTeamArray) {
	
	// 0 Team - 1 Hours
	// 2 Team - 3 Hours
	// ...
	
	var data = [];
	var teamNames = [];
	
	for (var i = 1; i < teamAndHoursPerTeamArray.length; i+=2) {
				
//		console.log("TEAM teamAndHoursPerTeamArray[i]: ", teamAndHoursPerTeamArray[i-1]);
//		console.log("HOURS teamAndHoursPerTeamArray[i+1]: ", teamAndHoursPerTeamArray[i]);
		
		teamNames.push(teamAndHoursPerTeamArray[i-1]);		
		data.push(toFixed(teamAndHoursPerTeamArray[i], 2));
	}
	
	console.log("LABELS(teamNames) + DATA:");
	console.log(teamNames);
	console.log(data);
	
	var piChartHoursPerTeam = drawHoursPerTeamPiChart(teamNames, data);
	
//	AJS.$("#table-header > ul > li > a").bind("tabSelect", function(e, o) {
//        if (o.tab.attr("href") == "#tabs-line") {
//        	piChartHoursPerTeam.redraw();
//        }
//    });
}


