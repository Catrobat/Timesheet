/*
 * Copyright 2014 Stephan Fellhofer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

"use strict";

var restBaseUrl;

AJS.toInit(function () {
    var baseUrl = AJS.params.baseURL;
    restBaseUrl = baseUrl + "/rest/timesheet/latest/";

    var timesheet = [];
    var users = [];

    function populateTable(userInformation) {

        users = userInformation;

        AJS.$(".loadingDiv").show();
        AJS.$("#user-information-table-content").empty();
        for (var i = 0; i < userInformation.length; i++) {
            var latestEntryDate;
            if (new Date(userInformation[i].latestEntryDate).getTime() == new Date(0).getTime()) {
                latestEntryDate = "none";
            } else {
                latestEntryDate = (new Date(userInformation[i].latestEntryDate)).toLocaleDateString("en-US");
            }
            var inactiveEndDate;
            if (userInformation[i].inactiveEndDate == null || new Date(userInformation[i].inactiveEndDate).getTime() == new Date(0).getTime()) {
                inactiveEndDate = "";
            } else {
                inactiveEndDate = (new Date(userInformation[i].inactiveEndDate)).toLocaleDateString("en-US");
            }

            var enabledColumn = "</td><td headers='ti-enabled'><input class=\"checkbox\" type=\"checkbox\" id='checkBox"+ userInformation[i].timesheetID + "'>";
            if (userInformation[i].isEnabled) {
                enabledColumn = "</td><td headers='ti-enabled'><input class=\"checkbox\" type=\"checkbox\" id='checkBox"+ userInformation[i].timesheetID + "' checked>";
            }
            var row = "<tr>" +
                "<td headers='ti-users'>" + userInformation[i].userName +
                "</td><td headers='ti-email'>" + userInformation[i].email + // TODO:
                "</td><td headers='ti-team'>" + userInformation[i].teams + // TODO:
                "</td><td headers='ti-state'>" + userInformation[i].state +
                "</td><td headers='ti-inactive-end-date'>" + inactiveEndDate +

                "</td><td headers='ti-total-practice-hours'>" + userInformation[i].totalPracticeHours +
                "</td><td headers='ti-hours-per-half-year'>" + userInformation[i].hoursPerHalfYear +
                "</td><td headers='ti-hours-per-month'>" + userInformation[i].hoursPerMonth +

                "</td><td headers='ti-latest-entry-date'>" + latestEntryDate +
                "</td><td headers='ti-latest-entry-hours'>" + userInformation[i].latestEntryHours +
                "</td><td headers='ti-latest-entry-description'>" + userInformation[i].latestEntryDescription +
                enabledColumn +
                "</td></tr>";
            AJS.$("#user-information-table-content").append(row);
        }

        AJS.$("#user-information-table").trigger("update");
        var userList = new List("modify-user", {
            page: Number.MAX_VALUE,
            valueNames: ["username", "email", "account", "timesheet", "entry"]
        });

        userList.on('updated', function () {
            if (AJS.$("#search-filter-overview").val() === "") {
                AJS.$("#update-timesheet-button").show();
            } else {
                AJS.$("#update-timesheet-button").hide();
            }
            AJS.$("#user-information-table").trigger("update");
        });
        AJS.$(".loadingDiv").hide();
    }

    function updateTimesheetStatus() {

        var data = [];

        for (var i = 0; i < users.length; i++) {
            var tempData = {};
            tempData.timesheetID = users[i].timesheetID;
            var tmpCheckBox = AJS.$("#checkBox" + users[i].timesheetID);
            tempData.isEnabled = tmpCheckBox.prop("checked");
            console.log(tempData);
            data.push(tempData);
        }

        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'timesheets/updateEnableStates',
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(data),
            processData: false,
            success: function () {
                AJS.messages.success({
                    title: "Success!",
                    body: "Timesheet 'enabled states' updated."
                });
                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
                AJS.messages.error({
                    title: "Error!",
                    body: error.responseText
                });
                AJS.$(".loadingDiv").hide();
            }
        });
    }

    function fetchData() {
        var userInformationFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'user/getUserInformation',
            contentType: "application/json"
        });

        AJS.$.when(userInformationFetched)
            .done(populateTable)
            .fail(function (error) {
                AJS.messages.error({
                    title: 'There was an error while fetching the required data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
            });
    }

    fetchData();

    AJS.$("#update-timesheet-status").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === "Save User Information") {
            updateTimesheetStatus();
        }
    });
});
