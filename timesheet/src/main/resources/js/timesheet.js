"use strict";

var restBaseUrl;

AJS.toInit(function () {
    var baseUrl = AJS.params.baseURL;
    restBaseUrl = baseUrl + "/rest/timesheet/latest/";

    if (isAdmin) {
        document.getElementById("tabs-line").style.display = "none";
        document.getElementById("tabs-category").style.display = "none";
        document.getElementById("tabs-team").style.display = "none";
        fetchUsers();
    } else {
        //init coordinator/administrator/approved user Seetings
        initUserSaveButton();
        //fetch timesheet table data
        fetchUsers();
        fetchData()
        //fetch visualization data
        fetchVisData();
        fetchTeamVisData();
    }
});

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
            if (selectedUser[0] !== "")
                getTimesheetOfUser(selectedUser);
        } else if (AJS.$(document.activeElement).val() === 'Display') {
            var selectedUser = AJS.$("#approved-user-select2-field").val().split(',');
            if (selectedUser[0] !== "")
                getTimesheetOfUser(selectedUser);
        } else if (AJS.$(document.activeElement).val() === 'Visualize') {
            var selectedTeam = AJS.$("#select-team-select2-field").val().split(',');
            if (selectedTeam[0] !== "")
                getDataOfTeam(selectedTeam[0]);
        }
    });
}

function initCoordinatorTimesheetSelect(jsonConfig, jsonUser) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isTeamCoordinator = false;
    var listOfUsers = [];

    AJS.$("#coordinatorTimesheetSelect").append("<field-group>");
    AJS.$("#coordinatorTimesheetSelect").append("<h3>Coordinator Space</h3>");
    AJS.$("#coordinatorTimesheetSelect").append("<div class=\"field-group\"><label for=\"permission\">Timesheet Of</label><input class=\"text selectTimesheetOfUserField\" type=\"text\" id=\"user-select2-field\"></div>");
    AJS.$("#coordinatorTimesheetSelect").append("<div class=\"field-group\"><input type=\"submit\" value=\"Show\" class=\"aui-button aui-button-primary\"></field-group>");
    AJS.$("#coordinatorTimesheetSelect").append("</field-group>");
    for (var i = 0; i < config.teams.length; i++) {
        var team = config.teams[i];
        //check if user is coordinator of a team
        for (var j = 0; j < team['coordinatorGroups'].length; j++) {
            if (team['coordinatorGroups'][j].localeCompare(userName) == 0) {
                //add users of that team to the select2
                for (var j = 0; j < team['developerGroups'].length; j++) {
                    if (!listOfUsers.contains(team['developerGroups'][j]))
                        listOfUsers.push(team['developerGroups'][j]);
                }
                isTeamCoordinator = true;
            }
        }
    }
    AJS.$(".selectTimesheetOfUserField").auiSelect2({
        placeholder: "Select User",
        tags: listOfUsers.sort(),
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });

    if (isTeamCoordinator) {
        initSelectTimesheetButton();
        AJS.$("#coordinatorTimesheetSelect").show();
        AJS.$("#approvedUserTimesheetSelect").hide();
    } else {
        AJS.$("#coordinatorTimesheetSelect").hide();
    }
}

function initApprovedUserTimesheetSelect(jsonConfig, jsonUser, userList) {
    var config = jsonConfig[0];
    var userName = jsonUser[0]['userName'];
    var isApprovedUser = false;
    var listOfUsers = [];

    //Select User
    AJS.$("#approvedUserTimesheetSelect").append("<field-group>");
    AJS.$("#approvedUserTimesheetSelect").append("<h3>Approved User Space</h3>");
    AJS.$("#approvedUserTimesheetSelect").append("<div class=\"field-group\"><label for=\"approvedUserSelect\">Timesheet Of</label><input class=\"text approvedUserSelectTimesheetOfUserField\" type=\"text\" id=\"approved-user-select2-field\"></div>");
    AJS.$("#approvedUserTimesheetSelect").append("<div class=\"field-group\"><input type=\"submit\" value=\"Display\" class=\"aui-button aui-button-primary\"></field-group>");
    AJS.$("#approvedUserTimesheetSelect").append("</field-group>");
    //check if user is approved
    for (var i = 0; i < config.approvedUsers.length; i++) {
        var approvedUserName = config.approvedUsers[i].replace(/\s/g, '');
        if (approvedUserName.localeCompare(userName) == 0) {
            isApprovedUser = true;
            for (var j = 0; j < userList[0].length; j++)
                listOfUsers.push(userList[0][j]['userName']);
        }
    }
    if (isAdmin)
        for (var j = 0; j < userList[0].length; j++)
            listOfUsers.push(userList[0][j]['userName']);

    AJS.$(".approvedUserSelectTimesheetOfUserField").auiSelect2({
        placeholder: "Select User",
        tags: listOfUsers.sort(),
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });

    //Team Visualization
    var teamNameList = [];
    for (var i = 0; i < config.teams.length; i++)
        teamNameList.push(config.teams[i]['teamName']);

    AJS.$("#visualizationTeamSelect").append("<field-group>");
    AJS.$("#visualizationTeamSelect").append("<h3>Team</h3>");
    AJS.$("#visualizationTeamSelect").append("<div class=\"field-group\"><label for=\"teamSelect\">Visualize Team Data</label><input class=\"text teamSelectField\" type=\"text\" id=\"select-team-select2-field\"></div>");
    AJS.$("#visualizationTeamSelect").append("<div class=\"field-group\"><input type=\"submit\" value=\"Visualize\" class=\"aui-button aui-button-primary\"></field-group>");
    AJS.$("#visualizationTeamSelect").append("</field-group>");

    AJS.$(".teamSelectField").auiSelect2({
        placeholder: "Select Team",
        tags: teamNameList.sort(),
        tokenSeparators: [",", " "],
        maximumSelectionSize: 1
    });

    if (isApprovedUser || isAdmin) {
        initSelectTimesheetButton();
        AJS.$("#approvedUserTimesheetSelect").show();
        AJS.$("#coordinatorTimesheetSelect").hide();
    } else {
        AJS.$("#approvedUserTimesheetSelect").hide();
    }
}

function fetchUserTimesheetData(timesheetID) {

    var timesheetFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'coordinator/' + timesheetID,
        contentType: "application/json"
    });

    var entriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'coordinator/' + timesheetID + '/entries',
        contentType: "application/json"
    });

    var categoriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'categories',
        contentType: "application/json"
    });

    var teamsFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'teams/' + timesheetID,
        contentType: "application/json"
    });

    console.log("fetchUserTimesheetData");
    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched)
        .done(assembleTimesheetData)
        .done(populateTable, prepareImportDialog)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function fetchUserVisData(timesheetID) {
    var timesheetFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID,
        contentType: "application/json"
    });

    var entriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID + '/entries',
        contentType: "application/json"
    });

    var categoriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'categories',
        contentType: "application/json"
    });

    var teamsFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'teams',
        contentType: "application/json"
    });

    console.log("fetchUserVisData");
    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched)
        .done(assembleTimesheetVisData)
        .done(populateVisTable)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function getDataOfTeam(teamName) {
    var teamData = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheet/' + teamName + '/entries',
        contentType: "application/json"
    });

    AJS.$.when(teamData)
        .done(assignTeamData)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching team data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function getTimesheetOfUser(selectedUser) {
    var timesheetIDFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheetID/fromUser/' + selectedUser[0],
        contentType: "application/json"
    });

    AJS.$.when(timesheetIDFetched)
        .done(fetchUserTimesheetData)
        .done(fetchUserVisData)
        .done(initAdminSaveButton)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while getting timesheet data of another user.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function getExistingTimesheetHours(timesheetID) {
    var timesheetFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID,
        contentType: "applicatPion/json"
    });
    AJS.$.when(timesheetFetched)
        .done(updateTimesheetHours)
        //.done(location.reload())
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching existing timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function toFixed(value, precision) {
    var power = Math.pow(10, precision || 0);
    return Math.round(value * power) / power;
}

function updateTimesheetHours(existingTimesheetData) {
    var timesheetUpdateData = {
        timesheetID: existingTimesheetData.timesheetID,
        lectures: AJS.$("#timesheet-hours-lectures").val(),
        reason: AJS.$("#timesheet-substract-hours-text").val(),
        ects: AJS.$("#timesheet-hours-ects").val(),
        targetHourPractice: toFixed(AJS.$("#timesheet-hours-practical").val(), 1),
        targetHourTheory: toFixed(AJS.$("#timesheet-hours-theory").val(), 1),
        targetHours: AJS.$("#timesheet-hours-text").val(),
        targetHoursCompleted: toFixed((AJS.$("#timesheet-hours-theory").val()
        - (-AJS.$("#timesheet-hours-practical").val()) - AJS.$("#timesheet-hours-substract").val()), 1),
        targetHoursRemoved: toFixed(AJS.$("#timesheet-hours-substract").val(), 1),
        isActive: existingTimesheetData.isActive,
        isEnabled: existingTimesheetData.isEnabled
    };

    AJS.$.ajax({
            type: "post",
            url: restBaseUrl + 'timesheets/update/' + existingTimesheetData.timesheetID,
            contentType: "application/json",
            data: JSON.stringify(timesheetUpdateData)
        })
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while updating the timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
        });
}

function fetchUsers() {
    var config = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'config/getConfig',
        contentType: "application/json"
    });

    var jsonUser = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/owner/' + timesheetID,
        contentType: "application/json"
    });

    var userList = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getUsers',
        contentType: "application/json"
    });

    AJS.$.when(config, jsonUser, userList)
        .done(initCoordinatorTimesheetSelect)
        .done(initApprovedUserTimesheetSelect)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching user data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function fetchData() {
    var timesheetFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID,
        contentType: "application/json"
    });

    var entriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheets/' + timesheetID + '/entries',
        contentType: "application/json"
    });

    var categoriesFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'categories',
        contentType: "application/json"
    });

    var teamsFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'teams',
        contentType: "application/json"
    });

    console.log("fetchData");
    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched)
        .done(assembleTimesheetData)
        .done(populateTable, prepareImportDialog)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
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
    AJS.$("#timesheet-hours-substract").val(timesheetData.targetHoursRemoved);
    AJS.$("#timesheet-substract-hours-text").val(timesheetData.reason);
    AJS.$("#timesheet-hours-text").val(timesheetData.targetHours);
    AJS.$("#timesheet-hours-theory").val(calculateTheoryTime(timesheetData));
    AJS.$("#timesheet-hours-practical").val(calculateTime(timesheetData) - calculateTheoryTime(timesheetData));
    AJS.$("#timesheet-hours-remain").val(AJS.$("#timesheet-hours-text").val() - AJS.$("#timesheet-hours-theory").val()
        - AJS.$("#timesheet-hours-practical").val() - (-AJS.$("#timesheet-hours-substract").val()));
    AJS.$("#timesheet-hours-ects").val(timesheetData.ects);
    AJS.$("#timesheet-hours-lectures").val(timesheetData.lectures);
}

function assembleTimesheetData(timesheetReply, categoriesReply, teamsReply, entriesReply) {
    var timesheetData = timesheetReply[0];
    timesheetData.entries = entriesReply[0];
    timesheetData.categories = [];
    timesheetData.teams = [];

    console.log(timesheetData);

    categoriesReply[0].map(function (category) {
        timesheetData.categories[category.categoryID] = {
            categoryName: category.categoryName
        };
    });

    teamsReply[0].map(function (team) {
        timesheetData.teams[team.teamID] = {
            teamName: team.teamName,
            teamCategories: team.teamCategories
        };
    });

    initTimesheetInformationValues(timesheetData);

    return timesheetData;
}

function populateTable(timesheetDataReply) {
    var timesheetData = timesheetDataReply[0];

    if (!isAdmin) {
        if (!timesheetData.isActive) {
            require(['aui/banner'], function (banner) {
                banner({
                    body: 'Your TimePunch - Timesheet is marked as <strong>inactive</strong>.'
                });
            });
        } else if (!timesheetData.isEnabled) {
            require(['aui/banner'], function (banner) {
                banner({
                    body: 'Your TimePunch - Timesheet has been <strong>disabled</strong> by an Administrator.'
                });
            });
        } else if ((timesheetData.targetHours - timesheetData.targetHoursCompleted) <= 80) {
            require(['aui/banner'], function (banner) {
                banner({
                    body: 'TimePunch Timesheet - You have <strong>less than 80 hours</strong> left, please contact ' +
                    'your Team - Coordinator and/or an Administrator.'
                });
            });
        }
    }

    var timesheetTable = AJS.$("#timesheet-table");
    timesheetTable.empty();

    timesheetTable.append(Jira.Templates.Timesheet.timesheetHeader(
        {teams: timesheetData.teams}
    ));

    var actualDate = new Date();

    var emptyEntry = {
        entryID: "new-id",
        date: toDateString(actualDate),
        begin: actualDate.getHours() + ":" + actualDate.getMinutes(),
        end: (actualDate.getHours() + 1) + ":" + actualDate.getMinutes(),
        pause: "00:00",
        description: "",
        duration: "",
        isGoogleDocImport: false
    };

    var addNewEntryOptions = {
        httpMethod: "post",
        callback: addNewEntryCallback,
        ajaxUrl: restBaseUrl + "timesheets/" + timesheetData.timesheetID + "/entry/"
    };

    var emptyForm = renderFormRow(timesheetData, emptyEntry, addNewEntryOptions);
    timesheetTable.append(emptyForm);

    appendEntriesToTable(timesheetData);
}

function appendEntriesToTable(timesheetData) {
    //append timesheet entries to table
    var timesheetTable = AJS.$("#timesheet-table");

    timesheetData.entries.map(function (entry) {
        var viewRow = renderViewRow(timesheetData, entry);
        timesheetTable.append(viewRow);
    });
}

function prepareImportDialog(timesheetDataReply) {
    var timesheetData = timesheetDataReply[0];
    var showImportDialogButton = AJS.$(".import-google-docs");
    var importDialog = AJS.$(".import-dialog");
    var importTextarea = importDialog.find(".import-text");
    var startImportButton = importDialog.find(".start-import");

    showImportDialogButton.click(function () {
        AJS.dialog2(importDialog).show();
    });

    autosize(importTextarea);

    startImportButton.click(function () {
        importGoogleDocsTable(importTextarea.val(), timesheetData, importDialog);
    });
}

function importGoogleDocsTable(table, timesheetData, importDialog) {
    var entries = parseEntriesFromGoogleDocTimesheet(table, timesheetData);
    var url = restBaseUrl + "timesheets/" + timesheetID + "/entries";

    if (entries.length === 0) return;

    AJS.$.ajax({
            type: "post",
            url: url,
            contentType: "application/json",
            data: JSON.stringify(entries)
        })
        .then(function (response) {
            showImportMessage(response);
            AJS.dialog2(importDialog).hide();
            timesheetData.entries = response.entries;
            appendEntriesToTable(timesheetData);
        })
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error during your Google Timesheet import.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
        });
}

function showImportMessage(response) {
    var successfulEntries = response.entries.length;
    var errorEntries = response.errorMessages.length;

    if (errorEntries > 0) {

        var begin = (successfulEntries === 0)
            ? "Entries could not be imported"
            : "Some entries could not be imported";

        var message = begin + ". Reason: <br /> "
            + "<ul><li>"
            + response.errorMessages.join("</li><li>")
            + "</li></ul>"
            + "Successfully imported entries: " + successfulEntries + "<br />"
            + "Failed imports: " + errorEntries + "<br />";

        if (successfulEntries === 0)
            AJS.messages.error({title: 'Import Error', body: message});
        else
            AJS.messages.warning({title: 'Import Error', body: message});

    } else {
        var message = "Imported " + successfulEntries + " entries.";
        AJS.messages.success({
            title: 'Import was successful!',
            body: message
        });
    }
}

/**
 * Callback after creating new Entry
 * @param {Object} entry
 * @param {Object} timesheetData
 * @param {jQuery} form
 */
function addNewEntryCallback(entry, timesheetData, form) {
    var viewRow = renderViewRow(timesheetData, entry);
    var beginTime = form.beginTimeField.timepicker('getTime');
    var endTime = form.endTimeField.timepicker('getTime');

    form.row.after(viewRow);
    form.beginTimeField.timepicker('setTime', endTime);
    form.endTimeField.timepicker('setTime', new Date(2 * endTime - beginTime));
    form.pauseTimeField.val("00:00").trigger('change');
}

/**
 * Callback after editing an entry
 * @param {Object} entry
 * @param {Object} timesheetData
 * @param {jQuery} form
 */
function editEntryCallback(entry, timesheetData, form) {
    var newViewRow = prepareViewRow(timesheetData, entry); //todo check if entry is augmented
    var oldViewRow = form.row.prev();

    newViewRow.find("button.edit").click(function () {
        newViewRow.hide();
        form.row.show();
    });

    newViewRow.find("button.delete").click(function () {
        deleteEntryClicked(newViewRow, entry.entryID);
    });

    oldViewRow.after(newViewRow);
    oldViewRow.remove();

    form.row.hide();
}

/**
 * Handles saving an entry
 * @param {Object} timesheetData
 * @param {Object} saveOptions
 *           callback   : Function(entry, timesheetData, form)
 *           ajaxUrl    : String
 *           httpMethod : String
 * @param {jQuery} form
 * @returns {undefined}
 */
function saveEntryClicked(timesheetData, saveOptions, form, existingEntryID,
                          existingIsGoogleDocImportValue) {
    form.saveButton.prop('disabled', true);

    var date = form.dateField.val();
    var validDateFormat = new Date(date);

    if ((date == "") || (!isValidDate(validDateFormat))) {
        date = new Date().toJSON().slice(0, 10);
    }

    var beginTime = form.beginTimeField.timepicker('getTime');

    if (beginTime === null) {
        beginTime = new Date();
    }

    var endTime = form.endTimeField.timepicker('getTime');

    if (endTime === null) {
        endTime = new Date();
    }

    var pauseTime = form.pauseTimeField.timepicker('getTime');
    var beginDate = new Date(date + " " + toTimeString(beginTime));
    var endDate = new Date(date + " " + toTimeString(endTime));
    var pauseMin = pauseTime.getHours() * 60 + pauseTime.getMinutes();

    var entry = {
        beginDate: beginDate,
        endDate: endDate,
        description: form.descriptionField.val(),
        pauseMinutes: pauseMin,
        teamID: form.teamSelect.val(),
        categoryID: form.categorySelect.val(),
        isGoogleDocImport: existingIsGoogleDocImportValue
    };

    if (existingEntryID !== "new-id") {
        entry.entryID = existingEntryID;
    }

    form.loadingSpinner.show();

    AJS.$.ajax({
            type: saveOptions.httpMethod,
            url: saveOptions.ajaxUrl,
            contentType: "application/json",
            data: JSON.stringify(entry) //causes error in FIREFOX
        })
        .then(function (entry) {
            var augmentedEntry = augmentEntry(timesheetData, entry);
            saveOptions.callback(augmentedEntry, timesheetData, form);
        })
        .fail(function (error) {
            console.log(error);
            AJS.messages.error({
                title: 'There was an error while saving.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
        })
        .always(function () {
            form.loadingSpinner.hide();
            form.saveButton.prop('disabled', false);
        });
}

/**
 * creates a form with working ui components and instrumented buttons
 * @param {Object} timesheetData
 * @param {Object} entry
 * @param {Object} saveOptions
 *           callback   : Function(entry, timesheetData, form)
 *           ajaxUrl    : String
 *           httpMethod : String
 * @returns {jquery} form
 */
function renderFormRow(timesheetData, entry, saveOptions) {

    if (entry.pause === "")
        entry.pause = "00:00";

    var form = prepareForm(entry, timesheetData);

    form.saveButton.click(function () {
        saveEntryClicked(timesheetData, saveOptions, form, entry.entryID,
            entry.isGoogleDocImport);
    });

    return form.row;
}

/**
 * Create form for editing a entry & instrument ui components
 * @param {object} entry
 * @param {object} timesheetData
 * @returns {object of jquery objects}
 */
function prepareForm(entry, timesheetData) {

    var teams = timesheetData.teams;
    var row = AJS.$(Jira.Templates.Timesheet.timesheetEntryForm(
        {entry: entry, teams: teams})
    );

    var form = {
        row: row,
        loadingSpinner: row.find('span.aui-icon-wait').hide(),
        saveButton: row.find('button.save'),
        dateField: row.find('input.date'),
        beginTimeField: row.find('input.time.start'),
        endTimeField: row.find('input.time.end'),
        pauseTimeField: row.find('input.time.pause'),
        durationField: row.find('input.duration'),
        descriptionField: row.find('input.description'),
        categorySelect: row.find('span.category'),
        teamSelect: row.find('select.team')
    };

    //date time columns
    form.dateField
        .datePicker(
            {overrideBrowserDefault: true, languageCode: 'en'}
        );

    row.find('input.time.start, input.time.end')
        .timepicker({
            showDuration: false,
            timeFormat: 'H:i',
            scrollDefault: 'now',
            step: 15
        });

    form.pauseTimeField.timepicker({timeFormat: 'H:i', step: 15})
        .change(changePauseTimeField)
        .on('timeFormatError', function () {
            this.value = '00:00';
        });

    new Datepair(row.find(".time-picker")[0]);

    row.find('input.time')
        .change(function () {
            updateTimeField(form);
        });

    var initTeamID = (entry.teamID !== undefined)
        ? entry.teamID : Object.keys(teams)[0];

    form.teamSelect.auiSelect2()
        .change(function () {
            var selectedTeamID = this.value;
            updateCategorySelect(form.categorySelect, selectedTeamID, entry, timesheetData);
        })
        .auiSelect2("val", initTeamID)
        .trigger("change");

    if (countDefinedElementsInArray(teams) < 2) {
        row.find(".team").hide();
    }

    return form;
}

function parseEntriesFromGoogleDocTimesheet(googleDocContent, timesheetData) {
    var entries = [];

    googleDocContent
        .split("\n")
        .forEach(function (row) {
            if (row.trim() === "") return;
            var entry = parseEntryFromGoogleDocRow(row, timesheetData);
            entries.push(entry);
        });

    return entries;
}

function parseEntryFromGoogleDocRow(row, timesheetData) {
    var pieces = row.split("\t");

    var firstTeamID = Object.keys(timesheetData.teams)[0];
    var firstTeam = timesheetData.teams[firstTeamID];
    var firstCategoryIDOfFirstTeam = firstTeam.teamCategories[0];

    return {
        description: pieces[6],
        pauseMinutes: getMinutesFromTimeString(pieces[4]),
        beginDate: new Date(pieces[0] + " " + pieces[1]),
        endDate: new Date(pieces[0] + " " + pieces[2]),
        teamID: firstTeamID,
        categoryID: firstCategoryIDOfFirstTeam,
        isGoogleDocImport: true
    };
}

/**
 * Updates the Category Seletion Box depending on the selected team
 * @param {jQuery} categorySelect
 * @param {int} selectedTeamID
 * @param {Object} entry
 * @param {Object} timesheetData
 */
function updateCategorySelect(categorySelect, selectedTeamID, entry, timesheetData) {

    var selectedTeam = timesheetData.teams[selectedTeamID];
    var categoryPerTeam = filterCategoriesPerTeam(selectedTeam, timesheetData.categories);

    categorySelect.auiSelect2({data: categoryPerTeam});

    var selectedCategoryID = (entry.categoryID === undefined || selectedTeamID != entry.teamID)
        ? selectedTeam.teamCategories[0]
        : entry.categoryID;

    categorySelect.val(selectedCategoryID).trigger("change");
}

/**
 * Creates an array with the categories of seletedTeam
 * @param {Object} selectedTeam
 * @param {Object} categories
 * @returns {Array of Objects}
 */
function filterCategoriesPerTeam(selectedTeam, categories) {

    var categoriesPerTeam = [];

    selectedTeam.teamCategories.map(function (categoryID) {
        categoriesPerTeam.push(
            {id: categoryID, text: categories[categoryID].categoryName}
        );
    });

    return categoriesPerTeam;
}

function updateTimeField(form) {
    //todo: fix duration update without setTimeout
    setTimeout(function () {
        var duration = calculateDuration(
            form.beginTimeField.timepicker('getTime'),
            form.endTimeField.timepicker('getTime'),
            form.pauseTimeField.timepicker('getTime'));

        if (duration < 0) {
            duration = new Date(0);
        }

        form.durationField.val(toUTCTimeString(duration));
    }, 10);
}

function changePauseTimeField() {
    if (this.value === '') {
        this.value = '00:00';
    }
}

/**
 * creates a view row with working ui components
 * @param {Object} timesheetData
 * @param {Object} entry
 * @returns {viewrow : jquery, formrow : jquery}
 */
function renderViewRow(timesheetData, entry) {

    var augmentedEntry = augmentEntry(timesheetData, entry);

    var editEntryOptions = {
        httpMethod: "put",
        callback: editEntryCallback,
        ajaxUrl: restBaseUrl + "entries/" + entry.entryID
    };

    var viewRow = prepareViewRow(timesheetData, augmentedEntry);

    viewRow.find("button.edit").click(function () {
        //augmentedEntry.isGoogleDocImport = false;
        editEntryClicked(timesheetData, augmentedEntry, editEntryOptions, viewRow);
    });

    viewRow.find("button.delete").click(function () {
        deleteEntryClicked(viewRow, entry.entryID);
    });

    updateTimesheetInformationValues(timesheetData);

    return viewRow;
}

function editEntryClicked(timesheetData, augmentedEntry, editEntryOptions, viewRow) {
    var formRow = getFormRow(viewRow);

    if (formRow === undefined) {
        formRow = renderFormRow(timesheetData, augmentedEntry, editEntryOptions);
        viewRow.after(formRow);
    }

    viewRow.hide();
    formRow.show();
}

function deleteEntryClicked(viewRow, entryID) {

    var ajaxUrl = restBaseUrl + "entries/" + entryID;

    var spinner = viewRow.find('span.aui-icon-wait');
    spinner.show();

    AJS.$.ajax({
            type: 'DELETE',
            url: ajaxUrl,
            contentType: "application/json"
        })
        .then(function () {
            var formRow = getFormRow(viewRow);
            if (formRow !== undefined) formRow.remove();
            viewRow.remove();
        })
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while deleting.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
            spinner.hide();
        });
}

/**
 * Finds and returns the form row that belongs to a view row
 * @param {jQuery} viewRow
 * @returns {jQuery} formRow or undefined if not found
 */
function getFormRow(viewRow) {
    var formRow = viewRow.next(".entry-form");
    if (formRow.data("id") === viewRow.data("id")) {
        return formRow;
    }
}

/**
 * Augments an entry object wth a few attributes by deriving them from its
 * original attributes
 * @param {Object} timesheetData
 * @param {Object} entry
 * @returns {Object} augmented entry
 */
function augmentEntry(timesheetData, entry) {

    var pauseDate = new Date(entry.pauseMinutes * 1000 * 60);

    return {
        date: toDateString(new Date(entry.beginDate)),
        begin: toTimeString(new Date(entry.beginDate)),
        end: toTimeString(new Date(entry.endDate)),
        pause: (entry.pauseMinutes > 0) ? toUTCTimeString(pauseDate) : "",
        duration: toTimeString(calculateDuration(entry.beginDate, entry.endDate, pauseDate)),
        category: timesheetData.categories[entry.categoryID].categoryName,
        team: timesheetData.teams[entry.teamID].teamName,
        entryID: entry.entryID,
        beginDate: entry.beginDate,
        endDate: entry.endDate,
        description: entry.description,
        pauseMinutes: entry.pauseMinutes,
        teamID: entry.teamID,
        categoryID: entry.categoryID,
        isGoogleDocImport: entry.isGoogleDocImport
    };
}

/**
 * Creates the viewrow
 * @param {Object} timesheetData
 * @param {Object} entry
 */
function prepareViewRow(timesheetData, entry) {

    //todo: dont augment entry twice.
    var augmentedEntry = augmentEntry(timesheetData, entry);

    var viewRow = AJS.$(Jira.Templates.Timesheet.timesheetEntry(
        {entry: augmentedEntry, teams: timesheetData.teams}));

    viewRow.find('span.aui-icon-wait').hide();

    return viewRow;
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

Array.prototype.contains = function (k) {
    for (var p in this)
        if (this[p] === k)
            return true;
    return false;
}
