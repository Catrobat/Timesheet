"use strict";

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

            return true;
        
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


function initSelectTimesheetButton() {
    AJS.$("#timesheet-settings").submit(function (e) {
        e.preventDefault();

        if (AJS.$("#user-select2-field").val()) {
            selectedUser = AJS.$("#user-select2-field").val().split(',');
        } else if (AJS.$("#approved-user-select2-field").val()) {
            selectedUser = AJS.$("#approved-user-select2-field").val().split(',');
        }

        if(typeof selectedUser === "undefined"){
            console.log("you have not select a user !!");

            AJS.messages.error({
                title:"Error!",
                body: "Please select a user and try again!"
            });
            return;
        }

        require('aui/flag')({
            type: 'info',
            title: 'Page will be reloaded',
            body: '<p>Page will be loaded soon. Please wait...</p>' +
            'You can <a href="javascript:window.location.reload();">quick reload</a> by pressing the F5 key.',
            close: 'auto'
        });

        var selectedUser;

        // TODO: why even 2 different fields??? simplify to 1

        if (selectedUser[0] !== "") {
            saveTimesheetIDOfUserInSession(selectedUser);
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
    AJS.$("#reset-timesheet-settings-coord").submit(function (e) {
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
    console.log("time to return: ", (totalHours + totalMinutes / 60));
    return totalHours + totalMinutes / 60;
}


function initTimesheetInformationValues(timesheetData) {
    var target_hours_rounded = toFixed(timesheetData.targetHours, 2);
    var hours_done_rounded = toFixed(timesheetData.targetHoursCompleted, 2)
        + toFixed(timesheetData.targetHoursRemoved, 2);

    setProgressBar(target_hours_rounded, hours_done_rounded);

    AJS.$("#timesheet-hours-text").val(target_hours_rounded);
    AJS.$("#timesheet-hours-ects").val(timesheetData.ects);
    AJS.$("#timesheet-hours-practical").val(toFixed(calculateTime(timesheetData), 2));

    AJS.$("#timesheet-hours-remain").val(toFixed(timesheetData.targetHours
        - AJS.$("#timesheet-hours-practical").val() - timesheetData.targetHoursRemoved, 2));

    AJS.$("#edit-total-hours").on("click", function (e) {
        AJS.$("#timesheet-hours-text").removeAttr("disabled");
        AJS.$("#submit-total-hours").css("visibility" , "visible");
    });

    AJS.$("#submit-total-hours").on("click", function (e) {
        console.log("submitting new total hours");
        var value;
        AJS.$("#timesheet-hours-text").val() === "" ? value = 0 : value = AJS.$("#timesheet-hours-text").val();

        AJS.$.ajax({
            url : restBaseUrl + "updateTotalTargetHours/" + value,
            type : "POST",
            success : function (data) {
                AJS.messages.success({
                    title : "Success",
                    body :"<br> Your data has been updated",
                    fadeout: true,
                    delay: 3000,
                    duration: 3000
                });

                updateCurrentTimesheetData(data);
                updateProgressBar();
                updateTimesheetInformationValues(timesheetData_);

                AJS.$("#timesheet-hours-text").attr("disabled", "disabled");
                AJS.$("#submit-total-hours").css("visibility" , "hidden");
            },
            fail : function (err) {
                AJS.messages.error({
                    title : "Error",
                    body : "Reason: " + err.responseText,
                    fadeout: true,
                    delay: 3000,
                    duration: 3000
                })
            }
        });
    });

    AJS.$("#lectures-container").empty();
    var lectures = timesheetData.lectures;
    var splitted = lectures.split("@/@");

    splitted.forEach(function (item, index) {
        var element;
        index === 0 ? element = "<div>" : element = "<div class='lecture-element'>";
        element += "<input class='text' type='text' name='timesheet-hours-lectures' disabled='disabled' value='" + item + "'>";
        element += "<span data-lecture='" + item + "' class='aui-icon aui-icon-small aui-iconfont-delete delete-lecture'>Delete Lecture</span>";

        if(index === 0)
            element += "<span id='add-lecture' class='aui-icon aui-icon-small aui-iconfont-add'>Add new Lecture to Account</span>";

        element += "</div>";

        AJS.$("#lectures-container").append(element)
    });

    AJS.$("#add-lecture").on("click.timesheet", function (e) {
        e.preventDefault();

        console.log("add lecture was clicked");
        showInitTimesheetReasonDialog(false);
    });

    AJS.$(".delete-lecture").on("click.timesheet", function (e) {
        e.preventDefault();
        var data = e.target.getAttribute("data-lecture");

        console.log("we want to delete a lecture");
        showLectureDeletionDialog(data);
    });

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
        AJS.$("#substractTimesheetHours").append("</fieldset>");

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
        AJS.$("#substractTimesheetHours").append("</fieldset>");

        //load values
        AJS.$("#timesheet-substract-hours-text").val(timesheetData.reason);
        AJS.$("#timesheet-hours-substract").val(toFixed(timesheetData.targetHoursRemoved, 2));
    }
}


function showLectureDeletionDialog(lecture){
    var dialog = new AJS.Dialog({
        width: 520,
        height: 255,
        id: "lecture-deletion-dialog",
        closeOnOutsideClick: true
    });

    var content = "<h3 style='text-align: center'>Do you really want to delete Lecture: " + lecture + " ?<br>"+
        "This Action cannot be undone!</h3>"+
        "<h2 style='color: red; text-align: center'><strong>Please confirm your action!</strong></h2>";

    dialog.addHeader("Delete Lecture");
    dialog.addPanel("Confirm", content, "panel-body");

    dialog.addButton("Cancel", function () {
        dialog.remove();
    });

    dialog.addButton("Delete", function () {
        console.log("about to delete Lecture");
        deleteLecture(lecture, dialog)
    });

    dialog.gotoPage(0);
    dialog.gotoPanel(0);

    dialog.show();
}

function deleteLecture(lecture, dialog){
	var hours_part = lecture.split("(")[1];
	console.log("hours_part: " + hours_part);
    var hours_string = hours_part.replace(/[^0-9\.]/g, '');
    console.log("hours_string: " + hours_string);
    var hours;

    hours = (hours_string === "" ? 0 : parseInt(hours_string));

    var data ={
        reason : lecture,
        hours : hours
    };

    AJS.$.ajax({
        url : restBaseUrl + "deleteLecture",
        type : "DELETE",
        data : JSON.stringify(data),
        contentType : "application/json",
        success : function (data) {
            AJS.messages.success({
                title : "Success!",
                body : "Your data has been updated",
                fadeout: true,
                delay: 3000,
                duration: 3000
            });

            updateCurrentTimesheetData(data);
            updateProgressBar();
            updateTimesheetInformationValues(timesheetData_);

            dialog.remove();
            console.log("that worked");
        },
        fail : function (err) {
            AJS.messages.error({
                title : "Error",
                body : "<br> Reason: " + err.responseText,
                fadeout: true,
                delay: 3000,
                duration: 3000
            })
        }
    });

    console.log(data);
}

function setProgressBar(total, done){
//    console.log("initiating progress bar: total: " + total + " done: " + done);

    if(total == 0)
        return;

    var percent = Math.round(done * 100 / total);
    var percent_string = "" + percent + "%";

    var progress_bars = document.getElementsByClassName("progress");
    var texts = document.getElementsByClassName("progress-percentage-text");

    for(var i = 0; i < progress_bars.length; i ++ ){
        progress_bars[i].style.width = percent_string;
        texts[i].innerHTML = percent_string + " done";
    }
}

function updateProgressBar(){
    console.log("UpdateProgressBar called");
    AJS.$.ajax({
        url : restBaseUrl + "timesheets/" + timesheetData_.timesheetID,
        type : "GET",
        success : function(data){
            var target_hours_rounded = toFixed(data.targetHours, 2);
            var hours_done_rounded = toFixed(data.targetHoursCompleted, 2)
                + toFixed(data.targetHoursRemoved, 2);

            setProgressBar(target_hours_rounded, hours_done_rounded);
        },
        fail : function (err) {
            alert("something went wrong here " + err.responseText);
        }
    })
}

function updateTimesheetInfoData(){
    AJS.$.ajax({
        url : restBaseUrl + "timesheets/" + timesheetData_.timesheetID,
        type : "GET",
        success : function(data){
            updateCurrentTimesheetData(data);
            AJS.$.ajax({
                url : restBaseUrl + 'timesheets/' + timesheetID + '/entries',
                type : "GET",
                success : function (data) {
                    timesheetData_.entries = data;
                    updateTimesheetInformationValues(timesheetData_);
                    updateAppendEntriesToVisTable(timesheetData_);
                }
            });
        }
    })
}

function updateAppendEntriesToVisTable(timesheetData) {
	
	AJS.$("#visualization-table-content").empty();
    AJS.$("#visualization-table-content tr:first").empty();

	console.log("UPDATE updateAppendEntriesToVisTable");
    var availableEntries = timesheetData.entries;

    var pos = 0;
    var i = 0;
    //variables for the time calculation
    var totalHours = 0;
    var totalMinutes = 0;
    var totalTimeHours = 0;
    var totalTimeMinutes = 0;
    var timeLastSixMonthHours = 0;
    var timeLastSixMonthMinutes = 0;
    //save data in an additional array
    var count = 0;
    var dataPoints = [];
    //pi chart variables

    //spent time within the last six months
    var sixMonthAgo = new Date();
    sixMonthAgo.setMonth(sixMonthAgo.getMonth() - 6);

    while (i < availableEntries.length) {
        var referenceEntryDate = new Date(availableEntries[pos].beginDate);
        var compareToDate = new Date(availableEntries[i].beginDate);
        var oldPos = pos;

        if ((referenceEntryDate.getFullYear() == compareToDate.getFullYear()) &&
            (referenceEntryDate.getMonth() == compareToDate.getMonth())) {
            //add all times for the same year-month pairs
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

            //date within the last six months
            if (compareToDate.getTime() >= sixMonthAgo.getTime()) {
            	timeLastSixMonthMinutes = timeLastSixMonthMinutes + calculatedTime;               

                if (timeLastSixMonthMinutes >= 60) {
                    var minutesToFullHours = Math.floor(timeLastSixMonthMinutes / 60); //get only full hours
                    timeLastSixMonthHours = timeLastSixMonthHours + minutesToFullHours;
                    timeLastSixMonthMinutes = timeLastSixMonthMinutes - minutesToFullHours * 60;
                }
            }
        } else {
            pos = i;
            i = i - 1;
        }

        if (oldPos != pos || i == availableEntries.length - 1) {
            //add points to line lineDiagram
            var dataX = referenceEntryDate.getFullYear() + "-" + (referenceEntryDate.getMonth() + 1);
            var dataY = toFixed(totalHours + totalMinutes / 60, 2);
            dataPoints.push(dataX);
            dataPoints.push(dataY);
            AJS.$("#visualization-table-content").append("<tr><td headers=\"basic-date\" class=\"date\">" +
                "Working Time: " + referenceEntryDate.getFullYear() + "-" + (referenceEntryDate.getMonth() + 1) + "</td>" +
                "<td headers=\"basic-time\" class=\"time\">" + totalHours + "hours " + totalMinutes + "mins" + "</td>" +
                "</tr>");

            count++;

            //overall sum of spent time
            totalTimeHours = totalTimeHours + totalHours;
            totalTimeMinutes = totalTimeMinutes + totalMinutes;

            if (totalTimeMinutes >= 60) {
                var minutesToFullHours = Math.floor(totalTimeMinutes / 60); //get only full hours
                totalTimeHours = totalTimeHours + minutesToFullHours;
                totalTimeMinutes = totalTimeMinutes - minutesToFullHours * 60;
            }
            totalHours = 0;
            totalMinutes = 0;
        }

        i = i + 1;
    }

    var totalTime = totalTimeHours * 60 + totalTimeMinutes;

    //append total time
    AJS.$("#visualization-table-content tr:first").before("<tr><td headers=\"basic-date\" class=\"total-time\"><strong>" + "Total Working Time" + "</strong></td>" +
        "<td headers=\"basic-time\" class=\"time\"><strong>" + totalTimeHours + "hours " + totalTimeMinutes + "mins" + "</strong></td>" +
        "</tr>");
    console.log("TOTAL WORKING TIME (h): ", totalTimeHours);
    console.log("TOTAL WORKING TIME (min): ", totalTimeMinutes);

    //entry for average time
    var averageMinutesPerMonth = (totalTimeHours * 60 + totalTimeMinutes) / count;
    var averageTimeHours = 0;
    var averageTimeMinutes = 0;

    if (averageMinutesPerMonth >= 60) {
        var minutesToFullHours = Math.floor(averageMinutesPerMonth / 60); //get only full hours
        averageTimeHours = minutesToFullHours;
        averageTimeMinutes = averageMinutesPerMonth - minutesToFullHours * 60;
    }

    //append avg time
    AJS.$("#visualization-table-content").append("<tr><td headers=\"basic-date\" class=\"avg-time\"><strong>" + "Time / Month" + "</strong></td>" +
        "<td headers=\"basic-time\" class=\"time\"><strong>" + averageTimeHours + "hours " + Math.floor(averageTimeMinutes) + "mins" + "</strong></td>" +
        "</tr>");

    //append time last 6 month
    AJS.$("#visualization-table-content").append("<tr><td headers=\"basic-date\" class=\"total-time\"><strong>" + "Overall Time Last 6 Months" + "</strong></td>" +
        "<td headers=\"basic-time\" class=\"time\"><strong>" + timeLastSixMonthHours + "hours " + timeLastSixMonthMinutes + "mins" + "</strong></td>" +
        "</tr>");

    AJS.$("#visualization-table-content").trigger("update");

    //assign JSON data for line graph
    lineDiagram(dataPoints);
}

function updateTimesheetInformationValues(timesheetData) {

    console.log("updating timesheetInfo");
    console.log(timesheetData);

    AJS.$("#lectures-container").empty();

    var lecutures = timesheetData.lectures;
    var splitted = lecutures.split("@/@");

    splitted.forEach(function (item, index) {
        var element;
        index === 0 ? element = "<div>" : element = "<div class='lecture-element'>";
        element += "<input class='text' type='text' name='timesheet-hours-lectures' disabled='disabled' value='" + item + "'>";
        element += "<span data-lecture='" + item + "' class='aui-icon aui-icon-small aui-iconfont-delete delete-lecture'>Delete Lecture</span>";

        if(index === 0)
            element += "<span id='add-lecture' class='aui-icon aui-icon-small aui-iconfont-add'>Add new Lecture to Account</span>";

        element += "</div>";

        AJS.$("#lectures-container").append(element)
    });

    AJS.$("#timesheet-hours-substract").val(toFixed(timesheetData.targetHoursRemoved, 2));
    AJS.$("#timesheet-substract-hours-text").val(timesheetData.reason);
    AJS.$("#timesheet-hours-text").val(toFixed(timesheetData.targetHours, 2));

    AJS.$("#timesheet-hours-practical").val(toFixed(calculateTime(timesheetData), 2));

    AJS.$("#timesheet-hours-remain").val(toFixed(timesheetData.targetHours - 
        AJS.$("#timesheet-hours-practical").val() - timesheetData.targetHoursRemoved, 2));

    AJS.$("#timesheet-hours-ects").val(timesheetData.ects);

    AJS.$("#add-lecture").off();
    AJS.$(".delete-lecture");

    AJS.$("#add-lecture").on("click.timesheet", function (e) {
        e.preventDefault();

        console.log("add lecture was clicked");
        showInitTimesheetReasonDialog(false);
    });

    AJS.$(".delete-lecture").on("click.timesheet", function (e) {
        e.preventDefault();
        var data = e.target.getAttribute("data-lecture");

        console.log("we want to delete a lecture");
        showLectureDeletionDialog(data);
    });
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

