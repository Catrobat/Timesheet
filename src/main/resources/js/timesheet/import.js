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
        uploadFile(function (e) {
          AJS.$(".loadingDiv").css('visibility', 'visible');
          var timesheet = e.target.result;
          var entryRows = "";
          var start = false;
          var end = false;
          timesheet.split("\n").forEach(function (line) {
            if (line.startsWith("BEGINN")) {
              end = true;
            }
            if (start && !end) {
              entryRows += line + "\n";
            }
            if (line.startsWith("Datum")) {
              start = true;
            }
          });

          importGoogleDocsTable(entryRows, timesheetData, importDialog);
        });
    });
}

function readFile(file, callback) {
  var reader = new FileReader();

  reader.onload = callback;

  reader.readAsText(file.files[0]);
}

function uploadFile(callback) {
  var file = document.getElementById("upload-field");
  var filename = file.value;
  if (filename === "") {
    AJS.messages.error({
      title: "Error !",
      body: "Select a Timesheet file to proceed!"
    });
    closeImportDialog();
  }
  else {
    if (!filename.toLowerCase().includes(".tsv")) {
      AJS.messages.error({
        title: "Error !",
        body: "The data type of the file must be .tsv!"
      });
      closeImportDialog();
    }
    else {
      readFile(file, callback);
    }
  }
}

function importGoogleDocsTable(table, timesheetData, importDialog) {
    var entriesAndFaultyRowsAndTrimmedRows = parseEntriesFromGoogleDocTimesheet(table, timesheetData);
    var entries = entriesAndFaultyRowsAndTrimmedRows[0];
    var faultyRows = entriesAndFaultyRowsAndTrimmedRows[1];
    var trimmedRows = entriesAndFaultyRowsAndTrimmedRows[2];
    var skippedRows = entriesAndFaultyRowsAndTrimmedRows[3];

    if (faultyRows.length > 0) {
        var errorString = "Reason - The following rows are not formatted correctly: <br>";
        for (var i = 0; i < faultyRows.length; i++) {
            errorString += faultyRows[i] + "<br>";
        }
        removeErrorMessages(errorMessageObjectOne);
        removeErrorMessages(errorMessageObjectTwo);
        errorMessageObjectTwo = AJS.messages.error({
            title: 'There was an error during your Google Timesheet import.',
            body: '<p>' + errorString + '</p>'
        });
        closeImportDialog();
        return;
    }
    var warning_string = "";
    if (trimmedRows.length > 0) {
        var errorString = "The following rows have been imported correctly.<br>" +
            "HOWEVER: Their Task Descriptions have been trimmed to 255 characters! <br>";
        for (var i = 0; i < trimmedRows.length; i++) {
            errorString += trimmedRows[i] + "<br>";
        }

        warning_string += errorString;
    }

    if (skippedRows.length > 0) {
        var warning = "The following entries have been skipped: <br>";
        for (var i = 0; i < skippedRows.length; i++) {
            warning += skippedRows[i] + "<br>";
        }
        warning_string += warning;
    }

    if (warning_string.length != 0) {
        AJS.messages.warning({
            title: 'Just for your information.',
            body: '<p>' + warning_string + '</p>'
        });
    }

    var url = restBaseUrl + "timesheets/" + timesheetID + "/entries";

    if (entries.length === 0) return;

    AJS.$.ajax({
        type: "post",
        url: url,
        contentType: "application/json",
        data: JSON.stringify(entries),
        timeout: 1000 * 60 * 5 // 5 minutes
    })
        .then(function (response) {
            showImportMessage(response);
            removeErrorMessages(errorMessageObjectOne);
            removeErrorMessages(errorMessageObjectTwo);
            AJS.dialog2(importDialog).hide();
            timesheetData.entries = response;
            appendEntriesToTable(timesheetData);
        })
        .fail(function (error) {
            if (error.statusText === "timeout") {
                errorMessageObjectOne = AJS.messages.error({
                    title: 'Timesheet import timeout.',
                    body: '<p>Reason: your timesheet appears to be too big. Talk to a Timesheet Admin.</p>'
                });
            } else if (error.statusText === "Conflict") {
                var response_string = "";
                var error_object = JSON.parse(error.responseText);
                for (var i = 0; i < Object.keys(error_object).length; i++) {
                    var entry_key = Object.keys(error_object)[i];
                    if (entry_key !== "correct") {
                        var entries = error_object[entry_key];
                        response_string += "<h2>" + entry_key + "</h2>";

                        for (var j = 0; j < entries.length; j++) {
                            var begin = new Date(entries[j].beginDate);
                            var end = new Date(entries[j].endDate);

                            response_string += "<p><strong>Begin: </strong>" + begin.toLocaleDateString("en-US") +
                                " <strong>End: </strong>" + end.toLocaleDateString("en-US") + "<strong> Description: </strong>"
                                + entries[j].description + "</p>";
                        }
                    }
                }
                removeErrorMessages(errorMessageObjectOne);
                removeErrorMessages(errorMessageObjectTwo);
                errorMessageObjectOne = AJS.messages.error({
                    title: 'There was an error during your Google Timesheet import.',
                    body: '<p>Reason: ' + response_string + '</p>'
                });

                var parsed = JSON.parse(error.responseText);
                timesheetData.entries = parsed.correct;
                appendEntriesToTable(timesheetData);
            } else {
                errorMessageObjectOne = AJS.messages.error({
                    title: 'Timesheet import Error.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
            }
        })
        .always(function() {
            closeImportDialog();
        });
}

function closeImportDialog() {
    var importDialog = AJS.$(".import-dialog");
    AJS.dialog2(importDialog).hide();
}

function removeErrorMessages(messageVariable) {
	if(messageVariable)
		messageVariable.closeMessage();
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
    var trimmedTaskDescriptionRows = [];
    var skipped_rows = [];

    googleDocContent
        .split("\n")
        .forEach(function (row) {
            if (row.trim() === "") return;
            if(checkForEmptyRow(row)) {
                skipped_rows.push("<span style=\"color:#BDBDBD\">" + row + "</span>");
                //foreach continue
                return;
            }
            var entry = parseEntryFromGoogleDocRow(row, timesheetData);
            if (entry === null) {
                faultyRows.push(row);
            } else if (entry === columnsMissing) {
            	faultyRows.push(row + columnsMissing);
            } else if (entry === 0) {
            	faultyRows.push(row + "<strong> (Date is empty)</strong>");
            } else if (entry === 1) {
            	faultyRows.push(row + "<strong> (Start is empty)</strong>");
            } else if (entry === 2) {
            	faultyRows.push(row + "<strong> (End is empty)</strong>");
            } else if (entry === 3) {
            	faultyRows.push(row + "<strong> (Duration is empty)</strong>");
            } else if (entry === 6) {
            	faultyRows.push(row + "<strong> (Task Description is empty)</strong>");
            } else if (entry === wrongDateFormat) {
            	faultyRows.push(row + wrongDateFormat);
            } else if (entry === beginEndDateNotValid) {
            	faultyRows.push(row + beginEndDateNotValid);
            } else {
              entries.push(entry);
            }
            
            var pieces = row.split("\t");
            if (pieces.length === 7){
            	var taskDescription = pieces[6];
            	if (taskDescription.length > 255) {
            		trimmedTaskDescriptionRows.push("<span style=\"color:#BDBDBD\">" + row + "</span>");
            	}
            }
        });

    return [entries, faultyRows, trimmedTaskDescriptionRows, skipped_rows];
}

var columnsMissing = "<strong> (Less than 7 columns)</strong>";
var wrongDateFormat = "<strong> (Wrong Dateformat)</strong>";
var beginEndDateNotValid = "<strong> (Begin- or Enddate not valid)</strong>";
 
function parseEntryFromGoogleDocRow(row, timesheetData) {
    var pieces = row.split("\t");

    //check if import entry length is valid
    if (pieces.length < 7) {
        return columnsMissing;
    }
    //if no pause is specified 0 minutes is given
    if (pieces[4] == "") {
        pieces[4] = "00:00";
    }
    //check if any field of the import entry is empty
    var categoryID = 0;
    for (var i = 0; i <= 7; i++) {
        //Category is allowed to be empty
    	
    	if (i === 5) {
    		var categoryColumn = pieces[i];
    		switch (categoryColumn) {
    			case "j":
    				pieces[i] = "Theory";
    				categoryID = -1;
    				break;
    			case "Theory (MT)":
    				pieces[i] = "Theory (MT)";
    				categoryID = -3;
    				break;
    			case "Meeting":
    				pieces[i] = "Meeting";
    				categoryID = -4;
    				break;
    			case "Pair programming":
    				pieces[i] = "Pair Programming";
    				categoryID = -5;
    				break;
    			case "Programming":
    				pieces[i] = "Programming";
    				categoryID = -6;
    				break;
    			case "Research":
    				pieces[i] = "Research";
    				categoryID = -7;
    				break;
    			case "Planning Game":
    				pieces[i] = "Planning Game";
    				categoryID = -8;
    				break;
    			case "Refactoring":
    				pieces[i] = "Refactoring";
    				categoryID = -9;
    				break;
    			case "Refactoring (PP)":
    				pieces[i] = "Refactoring (PP)";
    				categoryID = -10;
    				break;
    			case "Code Acceptance":
    				pieces[i] = "Code Acceptance";
    				categoryID = -11;
    				break;
    			case "Organisational tasks":
    				pieces[i] = "Organisational tasks";
    				categoryID = -12;
    				break;
    			case "Discussing issues/Supporting/Consulting":
    				pieces[i] = "Discussing issues/Supporting/Consulting";
    				categoryID = -13;
    				break;
    			case "Inactive":
    				pieces[i] = "Inactive";
    				categoryID = -14;
    				break;
    			case "Other":
    				pieces[i] = "Other";
    				categoryID = -15;
    				break;
    			case "Bug fixing (PP)":
    				pieces[i] = "Bug fixing (PP)";
    				categoryID = -16;
    				break;
    			case "Bug fixing":
    				pieces[i] = "Bug fixing";
    				categoryID = -17;
    				break;
    				
    			case "":
    			default:
    				pieces[i] = "GoogleDocsImport";
    				categoryID = -2;
    		}
    	}
    	else if (pieces[i] == "") {
            return i;
        }
    	
//        if (i == 5 && pieces[i].toLowerCase() == "j") {
//            pieces[i] = "Theory";
//            categoryID = -1;
//        }
//        else if (i == 5 && pieces[i] == "") {
//            pieces[i] = "GoogleDocsImport";
//            categoryID = -2;
//        }
//        else if (pieces[i] == "") {
//            return i;
//        }
    }

    var beginDateString = pieces[0] + " " + pieces[1];
    var endDateString = pieces[0] + " " + pieces[2];

    var beginDate;
    var endDate;
    // check date format
    var german = /^\d\d\.\d\d\.\d\d\d\d \d?\d:\d\d$/;
    var iso = /^\d\d\d\d-\d\d-\d\d \d\d:\d?\d$/;
    if (german.test(beginDateString) && german.test(endDateString)) {
        beginDate = parseGermanDate(beginDateString);
        endDate = parseGermanDate(endDateString);
    } else if(iso.test(beginDateString) && iso.test(endDateString)) {
        beginDate = new Date(beginDateString);
        endDate = new Date(endDateString);
    } else {
        return wrongDateFormat;
    }

    //check if entry values are correct
    if ((!isValidDate(beginDate)) || (!isValidDate(endDate))) {
        return beginEndDateNotValid;
    }

    var firstTeamID = Object.keys(timesheetData.teams)[0];

    if (beginDate > endDate) {
        endDate.setDate(endDate.getDate() + 1)
    }
    
    var taskDescription;
    if (pieces[6].length > 255) {
    	taskDescription = pieces[6].substring(0, 254);
    	
    }
    else {
    	taskDescription = pieces[6];
    }
    	

    return {
        description: taskDescription,
        pauseMinutes: getMinutesFromTimeString(pieces[4]),
        beginDate: beginDate,
        endDate: endDate,
        teamID: firstTeamID,
        categoryID: categoryID,
        isGoogleDocImport: true,
        ticketID: "",
        partner: "",
        inactiveEndDate: beginDate
    };
}

function checkForEmptyRow(row){
    var pieces = row.split("\t");
    var i;
    var empty = true;
    for(i = 0; i < pieces.length-1; i ++){
        if(pieces[i].length != 0){
            if(pieces[i] === "\n")
                continue;
            if(i === 3) {
                if (pieces[3] === "0:00:00") {
                    continue;
                }
            }
            empty = false;
            break;
        }
    }
    return empty;
}