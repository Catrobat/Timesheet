"use strict";

var errorMessageObjectTwo;
var errorMessageObjectOne;

function prepareImportDialog(timesheetDataReply) {
    var timesheetData = timesheetDataReply[0];
    var showImportDialogButton = AJS.$(".import-google-docs");
    var importDialog = AJS.$(".import-dialog");
    var importTextarea = importDialog.find(".import-text");
    var startImportButton = importDialog.find(".start-import");

    showImportDialogButton.click(function () {
        AJS.dialog2(importDialog).show();
    });

    autosize(importTextarea);

    startImportButton.click(function () {
        importGoogleDocsTable(importTextarea.val(), timesheetData, importDialog);
    });

}

function importGoogleDocsTable(table, timesheetData, importDialog) {
    var entriesAndFaultyRows = parseEntriesFromGoogleDocTimesheet(table, timesheetData);
    var entries = entriesAndFaultyRows[0];
    var faultyRows = entriesAndFaultyRows[1];

    if (faultyRows.length > 0) {
        var errorString = "Reason - The following rows are not formatted correctly: <br>";
        for (var i = 0; i < faultyRows.length; i++) {
            errorString += faultyRows[i] + "<br>";
        }
      removeErrorMessages();
      errorMessageObjectTwo = AJS.messages.error({
        title: 'There was an error during your Google Timesheet import.',
        body: '<p>' + errorString + '</p>'
      });
      
      return;
    }

    var url = restBaseUrl + "timesheets/" + timesheetID + "/entries/" + isMasterThesisTimesheet;

    if (entries.length === 0) return;

    AJS.$.ajax({
        type: "post",
        url: url,
        contentType: "application/json",
        data: JSON.stringify(entries)
    })
        .then(function (response) {
            showImportMessage(response);
            removeErrorMessages();
            AJS.dialog2(importDialog).hide();
            timesheetData.entries = response;
            appendEntriesToTable(timesheetData);
        })
        .fail(function (error) {
            var response_string = "";
            var error_object = JSON.parse(error.responseText);
            for(var i = 0; i < Object.keys(error_object).length; i++){
                var entry_key = Object.keys(error_object)[i];
                if(entry_key !== "correct"){
                    var entries = error_object[entry_key];
                    response_string += "<h2>"+ entry_key +"</h2>";

                    for(var j = 0; j < entries.length; j++){
                        var begin = new Date(entries[j].beginDate);
                        var end = new Date(entries[j].endDate);

                        response_string += "<p><strong>Begin: </strong>"+ begin.toLocaleDateString("en-US") +
                            " <strong>End: </strong>"+ end.toLocaleDateString("en-US") +"<strong> Description: </strong>"
                            + entries[j].description + "</p>";
                    }
                }
            }
            removeErrorMessages();
        	errorMessageObjectOne = AJS.messages.error({
                title: 'There was an error during your Google Timesheet import.',
                body: '<p>Reason: ' + response_string + '</p>'
            });
        	
            var parsed = JSON.parse(error.responseText);
            timesheetData.entries = parsed.correct;
            appendEntriesToTable(timesheetData);
        });
}

function removeErrorMessages() {
	if(errorMessageObjectTwo)
		errorMessageObjectTwo.closeMessage();
	if(errorMessageObjectOne)
		errorMessageObjectOne.closeMessage();
}

function showImportMessage(response) {
    var successfulEntries = response.length;

    var message = "Imported " + successfulEntries + " entries.";
    AJS.messages.success({
        title: 'Import was successful!',
        fadeout: true,
        delay: 5000,
        duration: 5000,
        body: message
    });
}

function parseEntriesFromGoogleDocTimesheet(googleDocContent, timesheetData) {
    var entries = [];
    var faultyRows = [];

    googleDocContent
        .split("\n")
        .forEach(function (row) {
            if (row.trim() === "") return;
            var entry = parseEntryFromGoogleDocRow(row, timesheetData);
            if (entry == null) {
                faultyRows.push(row);
            } else {
              entries.push(entry);
            }
        });

    return [entries, faultyRows];
}

function parseEntryFromGoogleDocRow(row, timesheetData) {
    var pieces = row.split("\t");

    //check if import entry length is valid
    if ((pieces.length < 7)) {
        return null;
    }
    //if no pause is specified 0 minutes is given
    if (pieces[4] == "") {
        pieces[4] = "00:00";
    }
    //check if any field of the import entry is empty
    var categoryID = 0;
    for (var i = 0; i <= 7; i++) {
        //Category is allowed to be empty
        if (i == 5 && pieces[i].toLowerCase() == "j") {
            pieces[i] = "Theory";
            categoryID = -1;
        }
        else if (i == 5 && pieces[i] == "") {
            pieces[i] = "GoogleDocsImport";
            categoryID = -2;
        }
        else if (pieces[i] == "") {
            return null;
        }
    }

    //check if entry values are correct
    if ((!isValidDate(new Date(pieces[0] + " " + pieces[1]))) ||
        (!isValidDate(new Date(pieces[0] + " " + pieces[2]))))
        return null;

    var firstTeamID = Object.keys(timesheetData.teams)[0];

    var beginDate = new Date(pieces[0] + " " + pieces[1]);
    var endDate = new Date(pieces[0] + " " + pieces[2]);
    if (beginDate > endDate) {
        endDate.setDate(endDate.getDate() + 1)
    }

    return {
        description: pieces[6],
        pauseMinutes: getMinutesFromTimeString(pieces[4]),
        beginDate: beginDate,
        endDate: endDate,
        teamID: firstTeamID,
        categoryID: categoryID,
        isGoogleDocImport: true,
        ticketID: "",
        partner: "",
        inactiveEndDate: new Date(pieces[0] + " " + pieces[1])
    };
}