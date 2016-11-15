"use strict";

var selectedUser;
var isMTSheetSelected;

function isSupervisedUser(userName, config) {
    var supervisedUsers = config.supervisors.split(',');
    for (var i = 0; i < supervisedUsers.length; i++) {
        if (supervisedUsers[i].localeCompare(userName) == 0) {
            return true;
        }
    }
    return false;
}

function hideVisualizationTabs() {
    document.getElementById("tabs-line").style.display = "none";
    document.getElementById("tabs-team").style.display = "none";
}

function filterAndSortCategoriesPerTeam(selectedTeam, categories) {
    var categoriesPerTeam = [];
    selectedTeam.teamCategories.filter(function (categoryID) {
        if (!isMTSheetSelected && categories[categoryID].categoryName === "Theory") {
            return false;
        } else {
            return true;
        }
    }).map(function (categoryID) {
        categoriesPerTeam.push(
            {id: categoryID, text: categories[categoryID].categoryName}
        );
    });
    categoriesPerTeam.sort(compareNames);
    return categoriesPerTeam;
}

function getNameFromCategoryIndex(categoryID, timesheetData) {
    if (timesheetData && timesheetData.categoryIDs[categoryID]) {
        return timesheetData.categoryIDs[categoryID].categoryName;
    }
    return "";
}

function getCategoryID(categoryName, teamCategories, timesheetData) {
    for (var i = 0; i < teamCategories.length; i++) {
        var teamID = teamCategories[i];
        if (categoryName == timesheetData.categoryIDs[teamID].categoryName) {
            return teamID;
        }
    }
    return 0;
}

function initUserSaveButton() {
    AJS.$("#timesheet-information").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === 'Save') {
            getExistingTimesheetHours(timesheetID);
        }
    });
}

function initAdministratorButton() {
    AJS.$("#timesheet-hours-save-button").hide();
    AJS.$("#timesheet-hours-update-button").show();
    AJS.$("#timesheet-hours-update-button").click('click', function (e) {
        e.preventDefault();
        getTimesheetByUser(selectedUser, isMTSheetSelected);
    });
}

function initSelectTimesheetButton() {
    AJS.$("#timesheet-settings").submit(function (e) {
        e.preventDefault();

        AJS.$("#timesheet-export-csv-link").empty();
        AJS.$("#timesheet-owner-information").empty();
        AJS.messages.generic({
            title: 'Timesheet Information.',
            body: '<p>You selected a timesheet of another user. ' +
            'If you want to enable all your ' +
            'visualizations again you have to refresh the page..</p>'
        });

        if (AJS.$(document.activeElement).val() === 'Show') {
            AJS.$("#timesheet-hours-save-button").hide();
            AJS.$("#timesheet-hours-save-button").hide();

            selectedUser = AJS.$("#user-select2-field").val().split(',');

            AJS.$("#timesheet-export-csv-link").empty();
            if (selectedUser[0] !== "") {
                getTimesheetOfUser(selectedUser, false);
                hideVisualizationTabs();
            }
        } else if (AJS.$(document.activeElement).val() === 'Display') {
            AJS.$("#timesheet-hours-save-button").hide();

            selectedUser = AJS.$("#approved-user-select2-field").val().split(',');

            isMTSheetSelected = AJS.$("#requestMTSheetCheckbox")[0].checked;
            if (selectedUser[0] !== "") {
                getTimesheetOfUser(selectedUser, isMTSheetSelected);
                hideVisualizationTabs();
            }
        }
    });
}

function toFixed(value, precision) {
    var power = Math.pow(10, precision || 0);
    return Math.round(value * power) / power;
}

function calculateTime(timesheetData) {
    var totalHours = 0;
    var totalMinutes = 0;
    var availableEntries = timesheetData.entries;

    for (var i = 0; i < availableEntries.length; i++) {
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
    }
    return totalHours + totalMinutes / 60;
}

function calculateTheoryTime(timesheetData) {
    if (!isMTSheetSelected)
        return 0;

    var totalHours = 0;
    var totalMinutes = 0;
    var availableEntries = timesheetData.entries;

    for (var i = 0; i < availableEntries.length; i++) {
        var hours = calculateDuration(availableEntries[i].beginDate, availableEntries[i].endDate,
            availableEntries[i].pauseMinutes).getHours();
        var minutes = calculateDuration(availableEntries[i].beginDate, availableEntries[i].endDate,
            availableEntries[i].pauseMinutes).getMinutes();
        var pause = availableEntries[i].pauseMinutes;
        var calculatedTime = hours * 60 + minutes - pause;

        if (timesheetData.categoryIDs[availableEntries[i].categoryID].categoryName === "Theory")
            totalMinutes = totalMinutes + calculatedTime;

        if (totalMinutes >= 60) {
            var minutesToFullHours = Math.floor(totalMinutes / 60); //get only full hours
            totalHours = totalHours + minutesToFullHours;
            totalMinutes = totalMinutes - minutesToFullHours * 60;
        }
    }
    return totalHours + totalMinutes / 60;
}

function initTimesheetInformationValues(timesheetData) {
    AJS.$("#timesheet-hours-text").val(timesheetData.targetHours);
    AJS.$("#timesheet-hours-remain").val(timesheetData.targetHours - timesheetData.targetHoursCompleted
        + timesheetData.targetHoursRemoved);
    AJS.$("#timesheet-hours-theory").val(timesheetData.targetHourTheory);
    AJS.$("#timesheet-hours-practical").val(timesheetData.targetHourPractice);
    AJS.$("#timesheet-hours-ects").val(timesheetData.ects);
    AJS.$("#timesheet-hours-lectures").val(timesheetData.lectures);

    if (isAdmin) {
        AJS.$("#substractTimesheetHours").empty();
        AJS.$("#substractTimesheetHours").append("<fieldset>");
        AJS.$("#substractTimesheetHours").append("<label for=\"timesheet-hours-substract\">Substracted Timesheet Hours</label>");
        AJS.$("#substractTimesheetHours").append("<input class=\"text\" type=\"text\" id=\"timesheet-hours-substract\" name=\"timesheet-hours-substract\" title=\"timesheet-hours-substract\">");
        AJS.$("#substractTimesheetHours").append("<div class=\"description\">Shows your remaining timesheet hours.</div>");
        AJS.$("#substractTimesheetHours").append("<label for=\"timesheet-substract-hours-text\">Description Text Field</label>");
        AJS.$("#substractTimesheetHours").append("<textarea name=\"timesheet-substract-hours-text\" id=\"timesheet-substract-hours-text\" rows=\"8\" cols=\"32\" placeholder=\"You can add here what ever you want...\"></textarea>");
        AJS.$("#substractTimesheetHours").append("<div class=\"description\">Reason(s) why some hours of your timesheet <br> have been \'terminated\'.</div>");
        AJS.$("#substractTimesheetHours").append("</fieledset>");

        //load values
        AJS.$("#timesheet-substract-hours-text").val(timesheetData.reason);
        AJS.$("#timesheet-hours-substract").val(timesheetData.targetHoursRemoved);
    } else {
        AJS.$("#substractTimesheetHours").empty();
        AJS.$("#substractTimesheetHours").append("<fieldset>");
        AJS.$("#substractTimesheetHours").append("<label for=\"timesheet-hours-substract\">Substracted Timesheet Hours</label>");
        AJS.$("#substractTimesheetHours").append("<input class=\"text\" type=\"text\" id=\"timesheet-hours-substract\" name=\"timesheet-hours-substract\" title=\"timesheet-hours-substract\" readonly>");
        AJS.$("#substractTimesheetHours").append("<div class=\"description\">Shows your remaining timesheet hours.</div>");
        AJS.$("#substractTimesheetHours").append("<label for=\"timesheet-substract-hours-text\">Description Text Field</label>");
        AJS.$("#substractTimesheetHours").append("<textarea name=\"timesheet-substract-hours-text\" id=\"timesheet-substract-hours-text\" rows=\"8\" cols=\"32\" placeholder=\"You can add here what ever you want...\" readonly></textarea>");
        AJS.$("#substractTimesheetHours").append("<div class=\"description\">Reason(s) why some hours of your timesheet <br> have been \'terminated\'.</div>");
        AJS.$("#substractTimesheetHours").append("</fieledset>");

        //load values
        AJS.$("#timesheet-substract-hours-text").val(timesheetData.reason);
        AJS.$("#timesheet-hours-substract").val(timesheetData.targetHoursRemoved);
    }

}

function updateTimesheetInformationValues(timesheetData) {
    AJS.$("#timesheet-hours-substract").val(toFixed(timesheetData.targetHoursRemoved, 2));
    AJS.$("#timesheet-substract-hours-text").val(timesheetData.reason);
    AJS.$("#timesheet-hours-text").val(toFixed(timesheetData.targetHours, 2));
    AJS.$("#timesheet-hours-theory").val(toFixed(calculateTheoryTime(timesheetData), 2));
    AJS.$("#timesheet-hours-practical").val(toFixed(calculateTime(timesheetData) - calculateTheoryTime(timesheetData), 2));
    AJS.$("#timesheet-hours-remain").val(toFixed(AJS.$("#timesheet-hours-text").val() - AJS.$("#timesheet-hours-theory").val()
        - AJS.$("#timesheet-hours-practical").val() - (-AJS.$("#timesheet-hours-substract").val()), 2));
    AJS.$("#timesheet-hours-ects").val(timesheetData.ects);
    AJS.$("#timesheet-hours-lectures").val(timesheetData.lectures);
    AJS.$("#timesheet-hours-text").val(timesheetData.ects * 30);
}

function toUTCTimeString(date) {
    var h = date.getUTCHours(), m = date.getUTCMinutes();
    var string =
        ((h < 10) ? "0" : "") + h + ":" +
        ((m < 10) ? "0" : "") + m;
    return string;
}

function toTimeString(date) {
    var h = date.getHours(), m = date.getMinutes();
    var string =
        ((h < 10) ? "0" : "") + h + ":" +
        ((m < 10) ? "0" : "") + m;
    return string;
}

function toDateString(date) {
    var y = date.getFullYear(), d = date.getDate(), m = date.getMonth() + 1;
    var string = y + "-" +
        ((m < 10) ? "0" : "") + m + "-" +
        ((d < 10) ? "0" : "") + d;
    return string;
}

function calculateDuration(begin, end, pause) {
    var pauseDate = new Date(pause);
    return new Date(end - begin - (pauseDate.getHours() * 60 + pauseDate.getMinutes()) * 60 * 1000);
}

function countDefinedElementsInArray(array) {
    return array.filter(function (v) {
        return v !== undefined
    }).length;
}

/**
 * Check if date is a valid Date
 * source: http://stackoverflow.com/questions/1353684/detecting-an-invalid-date-date-instance-in-javascript
 * @param {type} date
 * @returns {boolean} true, if date is valid
 */
function isValidDate(date) {
    if (Object.prototype.toString.call(date) === "[object Date]") {
        return !isNaN(date.getTime());
    }
    else {
        return false;
    }
}

function getMinutesFromTimeString(timeString) {
    var pieces = timeString.split(":");
    if (pieces.length === 2) {
        var hours = parseInt(pieces[0]);
        var minutes = parseInt(pieces[1]);
        return hours * 60 + minutes;
    } else {
        return 0;
    }
}

function compareNames(a, b) {
    if (a.text < b.text)
        return -1;
    if (a.text > b.text)
        return 1;
    return 0;
}

function isDateMoreThanTwoMonthsAhead(inactiveDate) {
    var date = new Date(inactiveDate);
    var today = new Date();
    if ((new Date(today.getFullYear(), today.getMonth(), today.getDate() + 61)) < date) {
        return true;
    }
    return false;
}

function compareTime(time1, time2) {
    var a = new Date(time1);
    var b = new Date(time2);
    if (a > b) { // a is later
        return 1;
    } else if (b > a) { // b is later
        return -1;
    }
    return 0; // equal
}


// function printDomainAttributes() {
//     var baseUrl = AJS.params.baseURL;
//     var hostname = AJS.$('<a>').prop('href', document.URL).prop('hostname');
//
//     console.log("Base address: " + baseUrl);
//     console.log("hostname: " + hostname);
//     console.log("location.hostname: " + location.hostname);
//     console.log("document.domain: " + document.domain);
//     console.log("document.URL : " + document.URL);
//     console.log("document.location.href : " + document.location.href);
//     console.log("document.location.origin : " + document.location.origin);
//     console.log("document.location.hostname : " + document.location.hostname);
//     console.log("document.location.host : " + document.location.host);
//     console.log("document.location.pathname : " + document.location.pathname);
//     console.log("window.location.hostname : " + window.location.hostname);
// }
