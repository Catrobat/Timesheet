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
    window.onbeforeunload = null;
    var baseUrl = AJS.params.baseURL;

    restBaseUrl = baseUrl + "/rest/timesheet/latest/";

    var errorMessage;
    var fetchingErrorMessage;

    function dynamicSort(property) {
        var sortOrder = 1;
        if(property[0] === "-") {
            sortOrder = -1;
            property = property.substr(1);
        }
        return function (a,b) {
            var result = (a[property] < b[property]) ? -1 : (a[property] > b[property]) ? 1 : 0;
            return result * sortOrder;
        }
    }

    function populateTable(userInformation) {
        //sort by username
        userInformation.sort(dynamicSort("userName"));

        for (var i = 0; i < userInformation.length; i++) {

            var firstEntryDate;
            if (new Date(userInformation[i].firstEntryDate).getTime() === new Date(0).getTime()) {
                firstEntryDate = "none";
            } else {
                firstEntryDate = (new Date(userInformation[i].firstEntryDate)).toLocaleDateString("en-US");
            }

            var latestEntryDate;
            if (new Date(userInformation[i].latestEntryDate).getTime() === new Date(0).getTime()) {
                latestEntryDate = "none";
            } else {
                latestEntryDate = (new Date(userInformation[i].latestEntryDate)).toLocaleDateString("en-US");
            }
            var inactiveEndDate;
            if (userInformation[i].inactiveEndDate == null || new Date(userInformation[i].inactiveEndDate).getTime() === new Date(0).getTime()) {
                inactiveEndDate = "";
            } else {
                inactiveEndDate = (new Date(userInformation[i].inactiveEndDate)).toLocaleDateString("en-US");
            }

            var enabled = userInformation[i].state !== "DISABLED";

            var timesheetId = userInformation[i].timesheetID;
            var dropdown =  "<aui-button id='button" + timesheetId + "' class='aui-button aui-dropdown2-trigger' aria-controls='active-timesheet" + timesheetId + "'>Actions</aui-button>";
            dropdown += "<aui-dropdown-menu id='active-timesheet" + timesheetId + "' >";
            dropdown += "<aui-item-link href='#' class='actions-open' value='SHOW'>Open Timesheet</aui-item-link>";
            dropdown += "<aui-item-link href='#' class='actions-enable' value='ACTIVE'>Enable Timesheet</aui-item-link>";
            dropdown += "<aui-item-link href='#' class='actions-disable' value='DISABLED'>Disable Timesheet</aui-item-link>";
            dropdown += "</aui-dropdown-menu>";

            var current_state = userInformation[i].state;
            var current_state_color = "black";
            var table = "";

            switch(current_state){
                case "ACTIVE":
                    current_state_color = "green";
                    table = "#active-user-table-content";
                    break;
                case "DISABLED":
                    current_state_color = "red";
                    table = "#disabled-user-table-content";
                    break;
                case "INACTIVE" :
                    current_state_color = "goldenRod";
                    table = "#inactive-user-table-content";
                    break;
                case "AUTO_INACTIVE":
                    current_state_color = "goldenRod";
                    table = "#autoinactive-user-table-content";
                    break;
                case "DONE":
                    current_state_color = "grey";
                    table = "#done-user-table-content";
                    break;
                case "INACTIVE_OFFLINE":
                    current_state_color = "goldenRod";
                    table = "#inactiveoffline-user-table-content";
                    break;
            }

            var enabledColumn = "</td><td headers='ti-actions'>" + dropdown;
            var row = "<tr id='tr" + timesheetId + "'>" +
                "<td headers='ti-users' class='users'>" +
                "<a href='#' class='view-profile-link' data-user-name='" + userInformation[i].userName + "'>" + userInformation[i].userName + "</a> "+
                "</td><td headers='ti-team' class='team'>" + userInformation[i].teams +
                "</td><td headers='ti-state' class='state' id='state"+ timesheetId + "' style='color:" + current_state_color + "';>" + userInformation[i].state +
                "</td><td headers='ti-inactive-end-date' class='inactive-end'>" + inactiveEndDate +
                "</td><td headers='ti-remaining-hours' class='remaining-hours'>" + userInformation[i].remainingHours +
                "</td><td headers='ti-target-total-hours' class='ti-target-total-hours'>" + userInformation[i].targetTotalHours +
                "</td><td headers='ti-total-hours' class='total-hours'>" + userInformation[i].totalHours +
                "</td><td headers='ti-hours-per-half-year' class='hours-half-year'>" + userInformation[i].hoursPerHalfYear +
                "</td><td headers='ti-hours-per-monitoring-period' class='hours-half-year'>" + userInformation[i].hoursPerMonitoringPeriod +
                "</td><td headers='ti-first-entry-date' class='latest-date'>" + firstEntryDate +
                "</td><td headers='ti-latest-entry-date' class='latest-date'>" + latestEntryDate +
                "</td><td headers='ti-latest-entry-description' class='latest-description'>" + userInformation[i].latestEntryDescription +
                enabledColumn +  "</td></tr>";

            AJS.$(table).append(row);

            setupDropdownButton(userInformation[i].timesheetID, enabled);
        }

        AJS.$(".view-timesheet-button").on("click", function (e) {
            var timesheet_id = e.target.getAttribute("data-timesheet-id");

            window.open(AJS.params.baseURL + "/plugins/servlet/timesheet?timesheetID=" + timesheet_id, "_blank");
        });

        AJS.$(".view-profile-link").on("click", function (e) {
            var user_name = e.target.getAttribute("data-user-name");

            window.open(AJS.params.baseURL + "/secure/ViewProfile.jspa?name=" + user_name, "_blank");
        });
        AJS.$(".loadingDiv").hide();
    }

    function setupDropdownButton(timesheetID, enabled) {
        var dropdown = document.getElementById("active-timesheet" + timesheetID);
        var buttonOpen = AJS.$(dropdown).find(".actions-open");
        var buttonDisable = AJS.$(dropdown).find(".actions-disable");
        var buttonEnable = AJS.$(dropdown).find(".actions-enable");

        buttonOpen.on("click", function() {
            window.open(AJS.params.baseURL + "/plugins/servlet/timesheet?timesheetID=" + timesheetID, "_blank");
        });

        buttonDisable.on("click", function() {
            setTimesheetState(timesheetID, false);
        });

        buttonEnable.on("click", function() {
            setTimesheetState(timesheetID, true);
        });

        if (enabled)  {
            buttonDisable.show();
            buttonEnable.hide();
        }  else  {
            buttonDisable.hide();
            buttonEnable.show();
        }
    }

    function setTimesheetState(timesheetID, enabled) {
        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'timesheets/' + timesheetID + '/updateEnableState/' + enabled,
            type: "POST",
            contentType: "application/json",
            processData: false,
            success: function () {
                if (fetchingErrorMessage)
                    fetchingErrorMessage.closeMessage();
                if (errorMessage)
                    errorMessage.closeMessage();
                AJS.messages.success({
                    title: "Success!",
                    body: "Timesheet " + timesheetID + " 'enabled state' updated.",
                    fadeout: true,
                    delay: 5000,
                    duration: 5000
                });

                setupDropdownButton(timesheetID, enabled);

                AJS.$("#tr" + timesheetID).remove();

                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
                if (errorMessage)
                    errorMessage.closeMessage();
                errorMessage = AJS.messages.error({
                    title: "Error!",
                    body: error.responseText
                });
                AJS.$(".loadingDiv").hide();
            }
        });
    }

    function populateTableWrapper(state) {
        AJS.$(".loadingDiv").show();
        var userInformationFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'user/getUserInformation/' + state,
            contentType: "application/json"
        });
        AJS.$.when(userInformationFetched)
            .done(populateTable)
            .fail(function (error) {
                if(fetchingErrorMessage)
                    fetchingErrorMessage.closeMessage();
                fetchingErrorMessage = AJS.messages.error({
                    title: 'There was an error while fetching the required data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
            });
    }

    function populateStatsWrapper() {
        AJS.$(".loadingDiv").show();
        var stateAmounts = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'user/getUserInformationStats',
            contentType: "text/plain"
        });

        AJS.$.when(stateAmounts)
            .done(populateStats)
            .fail(function (error) {
                if(fetchingErrorMessage)
                    fetchingErrorMessage.closeMessage();
                fetchingErrorMessage = AJS.messages.error({
                    title: 'There was an error while fetching the required data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
            });
    }

    function populateStats(amounts) {
        var sum = 0;
        var style = " style=\"text-align:right\"";
        var stat_list = ["Active", "Inactive", "Auto-Inactive",
            "Inactive-Offline", "Disabled", "Done", "Total Number of Timesheets"]

        for (var i = 0; i < amounts.length; i++) {
            console.log(amounts[i]);
            var row_stat = "<tr><td>" + stat_list[i] + "</td><td" + style + ">" + amounts[i] +  "</td></tr>";
            AJS.$("#timesheet-user-statistics").append(row_stat);
            sum += amounts[i];
        }

        AJS.$("#timesheet-user-statistics-total").append("<tr><th>" + stat_list[stat_list.length - 1] + "</th><th" + style + ">" + sum +  "</th></tr>");
        AJS.$(".loadingDiv").hide();
    }

    function fetchMonitoring() {
        var monitoringFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'monitoring/getMonitoring',
            contentType: "application/json"
        });

        AJS.$.when(monitoringFetched)
            .fail(function (error) {
                if(fetchingErrorMessage)
                    fetchingErrorMessage.closeMessage();
                fetchingErrorMessage = AJS.messages.error({
                    title: 'There was an error while fetching the required data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
            });
    }

    function setupClickListeners() {
        AJS.$(".loadingDiv").hide();

        AJS.$("#li-stats-tab").click(function(){
            AJS.$("#timesheet-user-statistics").empty()
            AJS.$("#timesheet-user-statistics-total").empty()
            populateStatsWrapper();
        })

        AJS.$("#li-active-tab").click(function(){
            AJS.$("#active-user-table-content").empty()
            populateTableWrapper("ACTIVE");
        })

        AJS.$("#li-inactive-tab").click(function(){
            AJS.$("#inactive-user-table-content").empty()
            populateTableWrapper("INACTIVE");
        })

        AJS.$("#li-autoinactive-tab").click(function(){
            AJS.$("#autoinactive-user-table-content").empty()
            populateTableWrapper("AUTO_INACTIVE");
        })

        AJS.$("#li-inactiveoffline-tab").click(function(){
            AJS.$("#inactiveoffline-user-table-content").empty()
            populateTableWrapper("INACTIVE_OFFLINE");
        })

        AJS.$("#li-disabled-tab").click(function(){
            AJS.$("#disabled-user-table-content").empty()
            populateTableWrapper("DISABLED");
        })

        AJS.$("#li-done-tab").click(function(){
            AJS.$("#done-user-table-content").empty()
            populateTableWrapper("DONE");
        })
    }

    setupClickListeners();
    populateStatsWrapper();
    fetchMonitoring();

});
