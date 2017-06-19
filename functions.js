"use strict";

var isMTSheetSelected;

function isReadOnlyUser(userName, config) {
    if (config.readOnlyUsers) {
        var readOnlyUsers = config.readOnlyUsers.split(',');
        for (var i = 0; i < readOnlyUsers.length; i++) {
            if (readOnlyUsers[i].localeCompare(userName) == 0) {
                return true;
            }
        }
    }
    return false;
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

function initUserSaveButton() {
    AJS.$("#timesheet-hours-save-button").click("click", function (e) {
        e.preventDefault();
        getExistingTimesheetHours(timesheetID);
    });
}

function initAdministratorButton() {
    AJS.$("#timesheet-hours-save-button").hide();
    AJS.$("#timesheet-hours-update-button").show();
    AJS.$("#timesheet-hours-update-button").click('click', function (e) {
        e.preventDefault();
        if (timesheetIDOfUser) {
            getExistingTimesheetHours(timesheetIDOfUser);
        } else {
            getExistingTimesheetHours(timesheetID);
        }
    });
}

function initSelectTimesheetButton() {
    AJS.$("#timesheet-settings").submit(function (e) {
        e.preventDefault();

        require('aui/flag')({
            type: 'info',
            title: 'Page will be reloaded',
            body: '<p>Page will be loaded soon. Please wait...</p>' +
            'You can <a href="javascript:window.location.reload();">quick reload</a> by pressing the F5 key.',
            close: 'auto'
        });

        var selectedUser;
        var isMTSheetSelected = AJS.$("#requestMTSheetCheckbox")[0].checked;

        // TODO: why even 2 different fields??? simplify to 1
        if (AJS.$("#user-select2-field").val()) {
            selectedUser = AJS.$("#user-select2-field").val().split(',');
        } else if (AJS.$("#approved-user-select2-field").val()) {
            selectedUser = AJS.$("#approved-user-select2-field").val().split(',');
        }

        if (selectedUser[0] !== "") {
            saveTimesheetIDOfUserInSession(selectedUser, isMTSheetSelected);
        }

        //browser reload
        window.setTimeout(function () {
            location.reload()
        }, 4000);
    });
    AJS.$("#reset-timesheet-settings").submit(function (e) {
        e.preventDefault();

        sessionStorage.removeItem('timesheetID');
        location.reload();
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
    AJS.$("#timesheet-hours-text").val(toFixed(timesheetData.targetHours, 2));
    AJS.$("#timesheet-hours-remain").val(toFixed(timesheetData.targetHours, 2) - toFixed(timesheetData.targetHoursCompleted, 2)
        + toFixed(timesheetData.targetHoursRemoved, 2));
    AJS.$("#timesheet-target-hours-theory").val(toFixed(timesheetData.targetHourTheory, 2));
    AJS.$("#timesheet-hours-ects").val(timesheetData.ects);
    AJS.$("#timesheet-hours-lectures").val(timesheetData.lectures);

    if (isAdmin) {
        AJS.$("#substractTimesheetHours").empty();
        AJS.$("#substractTimesheetHours").append("<fieldset>");
        AJS.$("#substractTimesheetHours").append("<label for=\"timesheet-hours-substract\">Substracted Timesheet Hours</label>");
        AJS.$("#substractTimesheetHours").append("<input class=\"text\" type=\"text\" id=\"timesheet-hours-substract\" name=\"timesheet-hours-substract\" title=\"timesheet-hours-substract\">");
        AJS.$("#substractTimesheetHours").append("<div class=\"description\">Shows your subtracted timesheet hours " +
        		"(only integers are supported)." +
        		"<br>The Remaining Timesheet Hours are increased by the value entered above.</div>");
        AJS.$("#substractTimesheetHours").append("<label for=\"timesheet-substract-hours-text\">Description Text Field</label>");
        AJS.$("#substractTimesheetHours").append("<textarea name=\"timesheet-substract-hours-text\" id=\"timesheet-substract-hours-text\" rows=\"8\" cols=\"32\" placeholder=\"No timesheet hours have been subtracted yet.\"></textarea>");
        AJS.$("#substractTimesheetHours").append("<div class=\"description\">Reason(s) why some hours of your timesheet <br> have been \'terminated\'.</div>");
        AJS.$("#substractTimesheetHours").append("</fieledset>");

        //load values
        AJS.$("#timesheet-substract-hours-text").val(timesheetData.reason);
        AJS.$("#timesheet-hours-substract").val(toFixed(timesheetData.targetHoursRemoved, 2));
    } else {
        AJS.$("#substractTimesheetHours").empty();
        AJS.$("#substractTimesheetHours").append("<fieldset>");
        AJS.$("#substractTimesheetHours").append("<label for=\"timesheet-hours-substract\">Substracted Timesheet Hours</label>");
        AJS.$("#substractTimesheetHours").append("<input disabled=\"disabled\" class=\"text\" type=\"text\" id=\"timesheet-hours-substract\" name=\"timesheet-hours-substract\" title=\"timesheet-hours-substract\" readonly>");
        AJS.$("#substractTimesheetHours").append("<div class=\"description\">Shows your subtracted timesheet hours " +
        		"(only integers are supported)." +
        		"<br>The Remaining Timesheet Hours are increased by the value entered above.</div>");
        AJS.$("#substractTimesheetHours").append("<label for=\"timesheet-substract-hours-text\">Description Text Field</label>");
        AJS.$("#substractTimesheetHours").append("<textarea disabled=\"disabled\" name=\"timesheet-substract-hours-text\" id=\"timesheet-substract-hours-text\" rows=\"8\" cols=\"32\" placeholder=\"No timesheet hours have been subtracted yet.\" readonly></textarea>");
        AJS.$("#substractTimesheetHours").append("<div class=\"description\">Reason(s) why some hours of your timesheet <br> have been \'terminated\'.</div>");
        AJS.$("#substractTimesheetHours").append("</fieledset>");

        //load values
        AJS.$("#timesheet-substract-hours-text").val(timesheetData.reason);
        AJS.$("#timesheet-hours-substract").val(toFixed(timesheetData.targetHoursRemoved, 2));
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

function validateHhMm(time) {
    var isValid = /^([0-1]?[0-9]|2[0-3]):([0-5][0-9])$/.test(time);

    if (isValid) {
        return true;
    }
    else {
        return false;
    }
}

function parseGermanDate(input) {
    var parts = input.match(/(\d+)/g);
    return new Date(parts[2], parts[1]-1, parts[0], parts[3], parts[4]);
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

function isPairProgrammingCategorySelected(timesheetData, form) {
    var categoryIndex = form.categorySelect.val();
    var categoryName = getNameFromCategoryIndex(categoryIndex, timesheetData).toLowerCase();
    return ((categoryName.includes("(pp)") || categoryName.includes("pair")) && !form.partnerSelect.val());
}

function getLengthOfArray(array) {
    var length = 0;
    for (var k in array) {
        length++;
    }
    return length;
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
