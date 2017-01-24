"use strict";

function lineDiagram(dataPoints) {
    var data = [];
    for (var i = 0; i < dataPoints.length; i = i + 2) {
        data.push({
            year: dataPoints[i],
            value: dataPoints[i + 1]
        });
    }
    var lineDiagram = drawLineDiagram(data);

    AJS.$("#table-header > ul > li > a").bind("tabSelect", function(e, o) {
        if (o.tab.attr("href") == "#tabs-line") {
            lineDiagram.redraw();
        }
    });
}