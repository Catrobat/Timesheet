"use strict";

function appendTimeToPiChart(theoryTime, practicalTime, totalTime) {
    var piChartDataPoints = [];

    //practice hours
    piChartDataPoints.push("Practice");
    piChartDataPoints.push(((practicalTime * 100) / totalTime).toString().slice(0, 5));
    //theory hours
    piChartDataPoints.push("Theory");
    piChartDataPoints.push(((theoryTime * 100) / totalTime).toString().slice(0, 5));

    var data = [];
    for (var i = 0; i < piChartDataPoints.length; i = i + 2) {
        data.push({
            label: piChartDataPoints[i],
            value: piChartDataPoints[i + 1]
        });
    }
    drawPiChart(data);
}