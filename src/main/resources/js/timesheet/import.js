"use strict";

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
    var entries = parseEntriesFromGoogleDocTimesheet(table, timesheetData);
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
            AJS.dialog2(importDialog).hide();
            timesheetData.entries = response.entries;
            appendEntriesToTable(timesheetData);
        })
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error during your Google Timesheet import.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
        });
}

function showImportMessage(response) {
    var successfulEntries = response.entries.length;
    var errorEntries = response.errorMessages.length;

    if (errorEntries > 0) {

        var begin = (successfulEntries === 0)
            ? "Entries could not be imported"
            : "Some entries could not be imported";

        var message = begin + ". Reason: <br /> "
            + "<ul><li>"
            + response.errorMessages.join("</li><li>")
            + "</li></ul>"
            + "Successfully imported entries: " + successfulEntries + "<br />"
            + "Failed imports: " + errorEntries + "<br />";

        if (successfulEntries === 0)
            AJS.messages.error({title: 'Import Error', body: message});
        else
            AJS.messages.warning({title: 'Import Error', body: message});

    } else {
        var message = "Imported " + successfulEntries + " entries.";
        AJS.messages.success({
            title: 'Import was successful!',
            body: message
        });
    }
}

function parseEntriesFromGoogleDocTimesheet(googleDocContent, timesheetData) {
    var entries = [];

    googleDocContent
        .split("\n")
        .forEach(function (row) {
            if (row.trim() === "") return;
            var entry = parseEntryFromGoogleDocRow(row, timesheetData);
            entries.push(entry);
        });

    return entries;
}

function parseEntryFromGoogleDocRow(row, timesheetData) {
    var isTheory;
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
    for (var i = 0; i <= 7; i++) {
        //Category is allowed to be empty
        if (i == 5 && pieces[i].toLowerCase() == "j") {
            isTheory = true;
            pieces[i] = "Theory";
        }
        else if (i == 5 && pieces[i] == "") {
            pieces[i] = "GoogleDocsImport";
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
    var firstTeam = timesheetData.teams[firstTeamID];
    var firstCategoryIDOfFirstTeam = firstTeam.teamCategories[0];

    //import category ID from Google Doc
    var categoryID = getCategoryID(pieces[5], firstTeam.teamCategories, timesheetData);

    if (categoryID == 0) {
        categoryID = firstCategoryIDOfFirstTeam;
    }

    return {
        description: pieces[6],
        pauseMinutes: getMinutesFromTimeString(pieces[4]),
        beginDate: new Date(pieces[0] + " " + pieces[1]),
        endDate: new Date(pieces[0] + " " + pieces[2]),
        teamID: firstTeamID,
        categoryID: categoryID,
        isGoogleDocImport: true,
        ticketID: "None",
        partner: "",
        inactiveEndDate: new Date(pieces[0] + " " + pieces[1]),
        deactivateEndDate: new Date(),
        isTheory: isTheory
    };
}