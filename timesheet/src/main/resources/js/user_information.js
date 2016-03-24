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

var tableSkeleton = "<h3>Teams</h3>" +
    "<table class=\"aui\">\n" +
    "<thead>\n" +
    "<tr>\n" +
    "<th id=\"basic-team\">Team</th>\n" +
    "<th id=\"basic-coordinator\">Coordinator</th>\n" +
    "<th id=\"basic-senior\">Senior</th>\n" +
    "<th id=\"basic-developer\">Developer</th>\n" +
    "<th id=\"basic-none\">None</th>\n" +
    "</tr>\n" +
    "</thead>\n" +
    "<tbody id=\"team-body\">\n" +
    "</tbody>\n" +
    "</table>" +
    "<h3>Individual Resources</h3>" +
    "<fieldset id=\"individual-resources\" class=\"group\"></fieldset>";

AJS.toInit(function () {
    //AJS.$(document).ajaxStart(function () {
    //    AJS.$(".loadingDiv").show();
    //});
    //AJS.$(document).ajaxStop(function () {
    //    AJS.$(".loadingDiv").hide();
    //});

    //var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");
    var baseUrl = AJS.params.baseURL;
    restBaseUrl = baseUrl + "/rest/timesheet/latest/";
    //restBaseUrl = "/rest/timesheet/latest/";

    var config;
    var timesheet = [];
    var users = []
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
            var timesheetState = timesheet[i]['isActive'] ? "active" : "inactive";

            if (!obj['active']) {
                AJS.$("#user-body").append("<tr><td headers=\"basic-username\" class=\"username\">" + username + "</td>" +
                    "<td headers=\"basic-email\" class=\"email\"><del>" + obj['email'] + "</td>" +
                    "<td headers=\"basic-state\" class=\"account\">" + state + "</td>" +
                    "<td headers=\"basic-timesheet-state\" class=\"timesheet\"><del>" + "active" + "</td>" +
                    "<td headers=\"basic-timesheet-latest-entry\" class=\"entry\">" + "Not Available" + "</td>" +
                    "<td headers=\"basic-timesheet-disable\" class=\"disable\"></td></tr>");
            } else {
                if (timesheet[i]['isEnabled']) {
                    AJS.$("#user-body").append("<tr><td headers=\"basic-username\" class=\"username\">" + username + "</td>" +
                        "<td headers=\"basic-email\" class=\"email\">" + obj['email'] + "</td>" +
                        "<td headers=\"basic-state\" class=\"account\">" + state + "</td>" +
                        "<td headers=\"basic-timesheet-state\" class=\"timesheet\">" + timesheetState + "</td>" +
                        "<td headers=\"basic-timesheet-latest-entry\" class=\"entry\">" + timesheet[i]['latestEntryDate'] + "</td>" +
                        "<td headers=\"basic-timesheet-disable\" class=\"disable\"><input class=\"checkbox\" type=\"checkbox\" name=\"" + username + "checkBox\" id=\"" + username + "checkBox\" checked></td></tr>");
                } else {
                    AJS.$("#user-body").append("<tr><td headers=\"basic-username\" class=\"username\">" + username + "</td>" +
                        "<td headers=\"basic-email\" class=\"email\">" + obj['email'] + "</td>" +
                        "<td headers=\"basic-state\" class=\"account\">" + state + "</td>" +
                        "<td headers=\"basic-timesheet-state\" class=\"timesheet\">" + timesheetState + "</td>" +
                        "<td headers=\"basic-timesheet-latest-entry\" class=\"entry\">" + timesheet[i]['latestEntryDate'] + "</td>" +
                        "<td headers=\"basic-timesheet-disable\" class=\"disable\"><input class=\"checkbox\" type=\"checkbox\" name=\"" + username + "checkBox\" id=\"" + username + "checkBox\"></td></tr>");
                }
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

        for (var i = 0; i < timesheet.length; i++) {
            if (users[i]['active']) {
                var tempData = {};
                tempData.timesheetID = timesheet[i]['timesheetID'];
                tempData.isActive = timesheet[i]['isActive'];
                tempData.isEnabled = AJS.$("#" + users[i]['userName'] + "checkBox")[0].checked;
                data.push(tempData);
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

    fetchData();

    AJS.$("#update-timesheet-status").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === 'Refresh List') {
            updateTimesheetStatus();
        }
    });
});
