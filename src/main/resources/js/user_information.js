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

    var timesheet = [];
    var errorMessage;
    var fetchingErrorMessage;

    function populateTable(userInformation) {
        AJS.$(".loadingDiv").show();
        AJS.$("#user-information-table-content").empty();
        AJS.$("#user-information-table-master-content").empty();
        
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

            var enabled = userInformation[i].state !== "DISABLED";

            var enableButton = "<button class='aui-button' id='button"+ userInformation[i].timesheetID + "'>Enable Timesheet</button>";
            
            
            if (userInformation[i].isMasterTimesheet === true) {
            	var enabledColumn = "</td><td headers='ti-enabled-m'>" + enableButton;
            	var rowm = "<tr>" +
                "<td headers='ti-users-m' class='musers'>" + userInformation[i].userName +
                "</td><td headers='ti-email-m' class='memail'>" + userInformation[i].email +
                "</td><td headers='ti-team-m' class='mteam'>" + userInformation[i].teams +
                "</td><td headers='ti-state-m' class='mstate' id='state"+ userInformation[i].timesheetID + "'>" + userInformation[i].state +
                "</td><td headers='ti-inactive-end-date-m' class='minactive-end'>" + inactiveEndDate +
                "</td><td headers='ti-remaining-hours-m' class='mremaining-hours'>" + userInformation[i].remainingHours +
                "</td><td headers='ti-target-total-hours-m' class='mtarget-total-hours'>" + userInformation[i].targetTotalHours +
                "</td><td headers='ti-total-practice-hours-m' class='mtotal-practice'>" + userInformation[i].totalPracticeHours +
                "</td><td headers='ti-hours-per-half-year-m' class='mhours-half-year'>" + userInformation[i].hoursPerHalfYear +
                "</td><td headers='ti-hours-per-month-m' class='mhours-month'>" + userInformation[i].hoursPerMonth +
                "</td><td headers='ti-latest-entry-date-m' class='mlatest-date'>" + latestEntryDate +
                "</td><td headers='ti-latest-entry-hours-m' class='mlatest-hours'>" + userInformation[i].latestEntryHours +
                "</td><td headers='ti-latest-entry-description-m' class='mlatest-description'>" + userInformation[i].latestEntryDescription +
                enabledColumn +
                "</td></tr>";
            	
            	AJS.$("#user-information-table-master-content").append(rowm);
            }
            else {
            	var enabledColumn = "</td><td headers='ti-enabled'>" + enableButton;
            	var row = "<tr>" +
                "<td headers='ti-users' class='users'>" + userInformation[i].userName +
                "</td><td headers='ti-email' class='email'>" + userInformation[i].email +
                "</td><td headers='ti-team' class='team'>" + userInformation[i].teams +
                "</td><td headers='ti-state' class='state' id='state"+ userInformation[i].timesheetID + "'>" + userInformation[i].state +
                "</td><td headers='ti-inactive-end-date' class='inactive-end'>" + inactiveEndDate +
                "</td><td headers='ti-remaining-hours' class='remaining-hours'>" + userInformation[i].remainingHours +
                "</td><td headers='ti-target-total-hours' class='ti-target-total-hours'>" + userInformation[i].targetTotalHours +
                "</td><td headers='ti-total-practice-hours' class='total-practice'>" + userInformation[i].totalPracticeHours +
                "</td><td headers='ti-hours-per-half-year' class='hours-half-year'>" + userInformation[i].hoursPerHalfYear +
                "</td><td headers='ti-hours-per-month' class='hours-month'>" + userInformation[i].hoursPerMonth +
                "</td><td headers='ti-latest-entry-date' class='latest-date'>" + latestEntryDate +
                "</td><td headers='ti-latest-entry-hours' class='latest-hours'>" + userInformation[i].latestEntryHours +
                "</td><td headers='ti-latest-entry-description' class='latest-description'>" + userInformation[i].latestEntryDescription +
                enabledColumn +
                "</td></tr>";
            	
            	AJS.$("#user-information-table-content").append(row);
            }
            	
            var timesheetID = userInformation[i].timesheetID;
            setEnableButton(timesheetID, enabled);
        }
        AJS.$("#user-information-table").trigger("update");
        AJS.$("#user-information-table-master").trigger("update");


        AJS.$("#timesheet-user-statistics").empty();
        var numberTotal = 0;
        var numberActive = 0;
        var numberInActive = 0;
        var numberAutoInActive = 0;
        var numberInActiveOffline = 0;
        var numberDisabled = 0;
        var numberDone = 0;
        var numberMasterTimesheets = 0;
        
        for (var i = 0; i < userInformation.length; i++) {
        	
        	numberTotal++;
        	
        	if (userInformation[i].state === "ACTIVE")
        		numberActive++;
        	else if (userInformation[i].state === "INACTIVE")
        		numberInActive++;
        	else if (userInformation[i].state === "AUTO_INACTIVE")
        		numberAutoInActive++;
        	else if (userInformation[i].state === "INACTIVE_OFFLINE")
        		numberInActiveOffline++;
        	else if (userInformation[i].state === "DISABLED")
        		numberDisabled++;
        	else if (userInformation[i].state === "DONE")
        		numberDone++;
        	
        	if (userInformation[i].isMasterTimesheet === true)
        		numberMasterTimesheets++;
        }
        
        var row = "<tr><td>" + "Total Number of Timesheets: " + numberTotal + "</td>" +
        				"<td>" + "Active Timesheets: " + numberActive + "</td>" +
                  		"<td>" + "Auto Inactive Timesheets: " + numberAutoInActive + "</td>" +
                        "<td>" + "Master Timesheets: " + numberMasterTimesheets + "</td>" +
                  "</tr>" +

                  "<tr><td>" + "Disabled Timesheets: " + numberDisabled + "</td>" +
                  		"<td>" + "Inactive Timesheets: " + numberInActive + "</td>" +
                  		"<td>" + "InactiveOffline Timesheets: " + numberInActiveOffline + "</td>" +
                  		"<td>" + "Done Timesheets: " + numberDone + "</td>" +
                  "</tr>";

        AJS.$("#timesheet-user-statistics").append(row);

        AJS.$(".loadingDiv").hide();
    }

    function setEnableButton(timesheetID, enabled) {
        var button = AJS.$("#button" + timesheetID);
        button.prop("onclick", null).off("click");
        if (enabled) {
            button.text("Disable Timesheet");
            button.click(function () {
                setTimesheetState(timesheetID, false)
            });
            button.css("background", "");
        } else {
            button.text("Enable Timesheet");
            button.click(function () {
                setTimesheetState(timesheetID, true)
            });
            button.css("background", "red");
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
                setEnableButton(timesheetID, enabled);
                if (enabled) {
                    AJS.$("#state" + timesheetID).text("ACTIVE");
                } else {
                    AJS.$("#state" + timesheetID).text("DISABLED");
                }
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
            	if(fetchingErrorMessage)
            		fetchingErrorMessage.closeMessage();
            	if(errorMessage)
            		errorMessage.closeMessage();
                AJS.messages.success({
                    title: "Success!",
                    body: "Timesheet 'enabled states' updated.",
                    fadeout: true,
                    delay: 5000,
                    duration: 5000
                });
                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
            	if(errorMessage)
            		errorMessage.closeMessage();
                errorMessage = AJS.messages.error({
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
            	if(fetchingErrorMessage)
            		fetchingErrorMessage.closeMessage();
                fetchingErrorMessage = AJS.messages.error({
                    title: 'There was an error while fetching the required data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
            });
    }

    fetchData();
});
