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

    var config;
    var timesheet = [];
    var users = [];
    getConfigAndCallback(baseUrl, function (ajaxConfig) {
        config = ajaxConfig;
    });

    function populateTable(allUsers, allTimesheets) {
        users = allUsers[0];
        timesheet = allTimesheets[0];

        AJS.$(".loadingDiv").show();
        AJS.$("#user-body").empty();
        for (var i = 0; i < users.length; i++) {
            var obj = users[i];
            var username = obj['active'] ? obj['userName'] : "<del>" + obj['userName'] + "</del>";
            var state = obj['active'] ? "active" : "inactive";

            var timesheetState = "no timesheet";
            var latestEntryDate = "";
            var isEnabled = false;

            for (var j = 0; j < timesheet.length; j++) {
                if (users[i].userName.toLowerCase() == timesheet[j].userKey.toLowerCase()) {
                    timesheetState = timesheet[j].state;
                    if (new Date(timesheet[j]['latestEntryDate']).getTime() == new Date(0).getTime()) {
                        latestEntryDate = "none";
                    } else {
                        latestEntryDate = (new Date(timesheet[j]['latestEntryDate'])).toLocaleDateString("en-US");
                    }
                    isEnabled = timesheet[j]['isEnabled'];
                    break;
                }
            }

            if (obj['active']) {
                var row = "<tr><td headers=\"basic-username\" class=\"username\">" + username + "</td>" +
                    "<td headers=\"basic-email\" class=\"email\">" + obj['email'] + "</td>" +
                    "<td headers=\"basic-state\" class=\"account\">" + state + "</td>" +
                    "<td headers=\"basic-timesheet-state\" class=\"timesheet\">" + timesheetState + "</td>" +
                    "<td headers=\"basic-timesheet-latest-entry\" class=\"entry\">" + latestEntryDate + "</td>" +
                    "<td headers=\"basic-timesheet-disable\" class=\"disable\">";

                if (isEnabled) {
                    row += "<input class=\"checkbox\" type=\"checkbox\" name=\"" + username + "checkBox\" id=\"" + username + "checkBox\" checked></td></tr>";
                } else {
                    row += "<input class=\"checkbox\" type=\"checkbox\" name=\"" + username + "checkBox\" id=\"" + username + "checkBox\"></td></tr>";
                }
                AJS.$("#user-body").append(row);
            }
        }

        AJS.$("#user-table").trigger("update");
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
            AJS.$("#user-table").trigger("update");
        });
        AJS.$(".loadingDiv").hide();
    }

    function updateTimesheetStatus() {

        var data = [];
        var tmpCheckBox;

        for (var i = 0; i < users.length; i++) {
            for (var j = 0; j < timesheet.length; j++) {
                if (users[i].userName.toLowerCase() == timesheet[j].userKey.toLowerCase()) {
                    if (users[i]['active']) {
                        var tempData = {};
                        tempData.timesheetID = timesheet[j]['timesheetID'];
                        tempData.isActive = timesheet[j]['isActive'];
                        tmpCheckBox = AJS.$("#" + users[i]['userName'] + "checkBox");
                        tempData.isEnabled = tmpCheckBox.prop("checked");
                        data.push(tempData);
                    }
                }
            }
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

        var allUserFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'user/getUsers',
            contentType: "application/json"
        });

        var allTimesheetsFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'timesheets/getTimesheets',
            contentType: "application/json"
        });

        AJS.$.when(allUserFetched, allTimesheetsFetched)
            .done(populateTable)
            .fail(function (error) {
                AJS.messages.error({
                    title: 'There was an error while fetching the required data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
            });
    }

    function getConfigAndCallback(baseUrl, callback) {
        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'config/getConfig',
            dataType: "json",
            success: function (config) {
                AJS.$(".loadingDiv").hide();
                callback(config);
            },
            error: function (error) {
                AJS.messages.error({
                    title: "Error!",
                    body: "Could not load 'config'.<br />" + error.responseText
                });
                AJS.$(".loadingDiv").hide();
            }
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
