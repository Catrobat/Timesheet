"use strict";

function isUserApprovedUser(userName, config) {
    for (var i = 0; i < config.approvedUsers.length; i++) {
        var approvedUserName = config.approvedUsers[i].replace(/\s/g, '');
        if (approvedUserName.localeCompare(userName) == 0) {
            return true;
        }
    }
    return false;
}

function hideVisualizationTabs() {
    document.getElementById("tabs-line").style.display = "none";
    document.getElementById("tabs-category").style.display = "none";
    document.getElementById("tabs-team").style.display = "none";
}

function filterCategoriesPerTeam(selectedTeam, categories) {

    var categoriesPerTeam = [];

    selectedTeam.teamCategories.map(function (categoryID) {
        categoriesPerTeam.push(
            {id: categoryID, text: categories[categoryID].categoryName}
        );
    });

    return categoriesPerTeam;
}

function getselectedCategoryName(categoryID, timesheetData) {
    return timesheetData.categories[categoryID].categoryName;
}

function getCategoryID(categoryName, teamCategories, timesheetData) {
    for(var i = 0; i < teamCategories.length; i++) {
        if(categoryName == timesheetData.categories[i+1].categoryName) {
            return i+1;
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

function initAdminSaveButton(timesheetID) {
    AJS.$("#timesheet-information").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === 'Save') {
            getExistingTimesheetHours(timesheetID);
        }
    });
}

function initSelectTimesheetButton() {
    AJS.$("#timesheet-settings").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === 'Show') {
            var selectedUser = AJS.$("#user-select2-field").val().split(',');
            if (selectedUser[0] !== "") {
                getTimesheetOfUser(selectedUser, false);
                AJS.$("#timesheet-owner-information").empty();
                AJS.$("#timesheet-owner-information").append("<h3>TimePunch Timesheet Owner: " + selectedUser[0] + "</h3>");
                hideVisualizationTabs();
            }
        } else if (AJS.$(document.activeElement).val() === 'Display') {
            var selectedUser = AJS.$("#approved-user-select2-field").val().split(',');
            if (selectedUser[0] !== "") {
                getTimesheetOfUser(selectedUser, AJS.$("#requestMTSheetCheckbox")[0].checked);
                AJS.$("#timesheet-owner-information").empty();
                AJS.$("#timesheet-owner-information").append("<h3>TimePunch Timesheet Owner: " + selectedUser[0] + "</h3>");
                hideVisualizationTabs();
            }
        } else if (AJS.$(document.activeElement).val() === 'Visualize') {
            var selectedTeam = AJS.$("#select-team-select2-field").val().split(',');
            if (selectedTeam[0] !== "") {
                getDataOfTeam(selectedTeam[0]);
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

        if (timesheetData.categories[availableEntries[i].categoryID].categoryName === "Theory")
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
    AJS.$("#timesheet-hours-substract").val(toFixed(timesheetData.targetHoursRemoved,1 ));
    AJS.$("#timesheet-substract-hours-text").val(timesheetData.reason);
    AJS.$("#timesheet-hours-text").val(toFixed(timesheetData.targetHours,1 ));
    AJS.$("#timesheet-hours-theory").val(toFixed(calculateTheoryTime(timesheetData),1 ));
    AJS.$("#timesheet-hours-practical").val(toFixed(calculateTime(timesheetData) - calculateTheoryTime(timesheetData), 1));
    AJS.$("#timesheet-hours-remain").val(toFixed(AJS.$("#timesheet-hours-text").val() - AJS.$("#timesheet-hours-theory").val()
        - AJS.$("#timesheet-hours-practical").val() - (-AJS.$("#timesheet-hours-substract").val()), 1));
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
        if (isNaN(date.getTime())) {
            return false;
        }
        else {
            return true;
        }
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
