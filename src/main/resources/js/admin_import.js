"use strict";

AJS.toInit(function () {
    AJS.$.ajax({
        type: "GET",
        url: restBaseUrl + "config/isGoogleDocsImportEnabled",
        success: function (enabled) {
            setGoogleDocsImportToggleButtonState(enabled)
        }
    });
});

function toggleGoogleDocsImport() {
    AJS.$.ajax({
        type: "GET",
        url: restBaseUrl + "config/toggleGoogleDocsImport",
        success: function (enabled) {
            setGoogleDocsImportToggleButtonState(enabled)
        }
    });
}

function setGoogleDocsImportToggleButtonState(enabled) {
    if (enabled) {
        AJS.$("#toggle-google-docs").text("Disable GoogleDocs").css("background", "");
    } else {
        AJS.$("#toggle-google-docs").text("Enable GoogleDocs").css("background", "red");
    }
}