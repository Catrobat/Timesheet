"use strict";

var restBaseUrl;

AJS.toInit(function () {
    var baseUrl = AJS.params.baseURL;
    restBaseUrl = baseUrl + "/rest/timesheet/latest/";
    AJS.$("#timesheet-export-csv-link").empty();

    if(isMasterThesisTimesheet) {
        document.getElementById("tabs-timesheet-settings").style.display = "none";
        document.getElementById("tabs-team").style.display = "none";
        AJS.$("#timesheet-export-csv-link").append("<h2>Export</h2>Download 'Master Thesis Timesheet' as <a href=\"download/masterthesis\">CSV</a>.");
    } else {
        AJS.$("#timesheet-export-csv-link").append("<h2>Export</h2>Download 'Timesheet' as <a href=\"download/timesheet\">CSV</a>.");
    }

    if (isAdmin) {
        hideVisualizationTabs();
        fetchUsers();
        AJS.$("#timesheet-hours-save-button").hide();
        AJS.$("#timesheet-hours-update-button").show();
    } else {
        //init coordinator/administrator/approved user Seetings
        initUserSaveButton();
        //fetch timesheet table data
        fetchUsers();
        fetchData()
        //fetch visualization data
        fetchVisData();
        fetchTeamVisData();
        AJS.$("#timesheet-hours-save-button").show();
        AJS.$("#timesheet-hours-update-button").hide();
    }
});

function fetchUserTimesheetData(timesheetID) {

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
        url: restBaseUrl + 'teams/' + timesheetID,
        contentType: "application/json"
    });
    var usersFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getUsers',
        contentType: "application/json"
    });
    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched, usersFetched)
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

function getTimesheetOfUser(selectedUser, isMTSheet) {
    var timesheetIDFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheet/timesheetID/' + selectedUser[0] + '/' + isMTSheet,
        contentType: "application/json"
    });
    AJS.$.when(timesheetIDFetched)
        .done(fetchUserTimesheetData)
        .done(fetchUserVisData)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while getting timesheet data of another user.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function getTimesheetByUser(selectedUser, isMTSheetSelected) {
    var timesheetFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'timesheet/of/' + selectedUser + '/' + isMTSheetSelected,
        contentType: "applicatPion/json"
    });
    AJS.$.when(timesheetFetched)
        .done(updateTimesheetHours)
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching existing timesheet data.',
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
        .done(location.reload())
        .fail(function (error) {
            AJS.messages.error({
                title: 'There was an error while fetching existing timesheet data.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
            console.log(error);
        });
}

function updateTimesheetHours(existingTimesheetData) {
    var timesheetUpdateData = {
        timesheetID: existingTimesheetData.timesheetID,
        lectures: AJS.$("#timesheet-hours-lectures").val(),
        reason: AJS.$("#timesheet-substract-hours-text").val(),
        ects: AJS.$("#timesheet-hours-ects").val(),
        targetHourPractice: toFixed(AJS.$("#timesheet-hours-practical").val(), 2),
        targetHourTheory: toFixed(AJS.$("#timesheet-hours-theory").val(), 2),
        targetHours: AJS.$("#timesheet-hours-ects").val() * 30,
        targetHoursCompleted: toFixed((AJS.$("#timesheet-hours-theory").val()
        - (-AJS.$("#timesheet-hours-practical").val()) - AJS.$("#timesheet-hours-substract").val()), 2),
        targetHoursRemoved: toFixed(AJS.$("#timesheet-hours-substract").val(), 2),
        isActive: existingTimesheetData.isActive,
        isEnabled: existingTimesheetData.isEnabled,
        isMTSheet: existingTimesheetData.isMTSheet
    };

    AJS.$.ajax({
            type: 'POST',
            url: restBaseUrl + 'timesheets/update/' + existingTimesheetData.timesheetID + '/' + existingTimesheetData.isMTSheet,
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
    var usersFetched = AJS.$.ajax({
        type: 'GET',
        url: restBaseUrl + 'user/getUsers',
        contentType: "application/json"
    });
    AJS.$.when(timesheetFetched, categoriesFetched, teamsFetched, entriesFetched, usersFetched)
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

function assembleTimesheetData(timesheetReply, categoriesReply, teamsReply, entriesReply, usersReply) {
    var timesheetData = timesheetReply[0];
    timesheetData.entries = entriesReply[0];
    timesheetData.categories = [];
    timesheetData.teams = [];
    timesheetData['users'] = [];

    //fill user names
    for(var i = 0; i < usersReply[0].length; i++) {
        if(usersReply[0][i]['active'])
            timesheetData['users'].push(usersReply[0][i]['userName']);
    }

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

    updateTimesheetInformationValues(timesheetData);

    return timesheetData;
}

function populateTable(timesheetDataReply) {
    var timesheetData = timesheetDataReply[0];

    if (!isAdmin) {
        //Information at the first use
        if (timesheetData.entries.length == 0)
            AJS.messages.generic({
                title: 'Timesheet Information.',
                body: '<p> Congratulations you sucessfully created your own ' +
                'Timesheet. TimePunch provides tracking your time ' +
                'data in a comfortable way and offers several visualization opportunities ' +
                'for colleted data. Additionally an import for existing timesheet entries ' +
                'from CSV / Google Doc Timesheets is provided. The required data formarting ' +
                'steps are shown within the "Import from Google Docs - Dialog".</p>' +
                '<p> If you notice any uncommon plugin behaviour, or need support feel free to ' +
                'contact one of the project "Coordinators", or an "Administrator".</p>'
            });

        //Banner Informations for the User
        if (!timesheetData.isActive) {
            AJS.messages.warning({
                title: 'Timesheet Warning.',
                body: '<p> Your Timesheet is marked as "inactive", because its last entry ' +
                'date is older than two weeks, or your entry table is empty.</p>'
            });

            require(['aui/banner'], function (banner) {
                banner({
                    body: 'Your Timesheet is marked as <strong>inactive</strong>.'
                });
            });
        } else if (!timesheetData.isEnabled) {
            AJS.messages.warning({
                title: 'Timesheet Warning.',
                body: '<p> Your Timesheet is marked as "disabled".</p>' +
                '<p> You are not able to apply any changes until it is "disabled" again by an Administrator.</p>'
            });

            require(['aui/banner'], function (banner) {
                banner({
                    body: 'Your Timesheet is marked as <strong>disabled</strong>.'
                });
            });
        } else if ((timesheetData.targetHours - timesheetData.targetHoursCompleted) <= 80) {

            AJS.messages.warning({
                title: 'Timesheet Warning.',
                body: '<p> Congratulations you almost did it. Please contact an "Administrator", or your ' +
                '"Coordinator" for further steps.</p>'
            });

            require(['aui/banner'], function (banner) {
                banner({
                    body: 'You have <strong>less than 80 hours</strong> left, please contact ' +
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
        inactiveEndDate: "",
        pause: "00:00",
        description: "",
        duration: "",
        ticketID: "",
        isGoogleDocImport: false,
        partner: ""
    };

    var addNewEntryOptions = {
        httpMethod: "post",
        callback: addNewEntryCallback,
        ajaxUrl: restBaseUrl + "timesheets/" + timesheetData.timesheetID + '/entry/' + isMasterThesisTimesheet
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

    var inactiveDate = form.inactiveEndDateField.val();
    var validInactiveDateFormat = new Date(inactiveDate);

    if ((inactiveDate == "") || (!isValidDate(validInactiveDateFormat))) {
        inactiveDate = new Date().toJSON().slice(0, 10);
    }

    var inactiveEndDate =  new Date(inactiveDate + " " + toTimeString(beginTime));

    //change entry to 0min duration for inactive entry
    if(getselectedCategoryName(form.categorySelect.val(), timesheetData) === "Inactive") {
        endDate = beginDate;
    }

    var entry = {
        beginDate: beginDate,
        endDate: endDate,
        inactiveEndDate: inactiveEndDate,
        description: form.descriptionField.val(),
        pauseMinutes: pauseMin,
        teamID: form.teamSelect.val(),
        categoryID: form.categorySelect.val(),
        isGoogleDocImport: existingIsGoogleDocImportValue,
        partner: form.partnerSelect.val(),
        ticketID: form.ticketField.val()
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
        inactiveEndDateField: row.find('input.inactive'),
        beginTimeField: row.find('input.time.start'),
        endTimeField: row.find('input.time.end'),
        pauseTimeField: row.find('input.time.pause'),
        durationField: row.find('input.duration'),
        descriptionField: row.find('input.description'),
        ticketField: row.find('input.ticket'),
        categorySelect: row.find('span.category'),
        partnerSelect: row.find('span.partner'),
        teamSelect: row.find('select.team')
    };

    //date time columns
    form.dateField
        .datePicker(
            {overrideBrowserDefault: true, languageCode: 'en'}
        );

    form.inactiveEndDateField
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

    form.partnerSelect.auiSelect2({tags: timesheetData.users.sort(),
        width: 'resolve',
        })

    if (countDefinedElementsInArray(teams) < 2) {
        row.find(".team").hide();
    }

    return form;
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
        ajaxUrl: restBaseUrl + "entries/" + entry.entryID + '/' + isMasterThesisTimesheet
    };

    var viewRow = prepareViewRow(timesheetData, augmentedEntry);

    viewRow.find("button.edit").click(function () {
        //augmentedEntry.isGoogleDocImport = false;
        editEntryClicked(timesheetData, augmentedEntry, editEntryOptions, viewRow);
    });

    viewRow.find("button.delete").click(function () {
        deleteEntryClicked(viewRow, entry.entryID);
    });

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

    var ajaxUrl = restBaseUrl + 'entries/' + entryID + '/' + isMasterThesisTimesheet;

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
        isGoogleDocImport: entry.isGoogleDocImport,
        inactiveEndDate: toDateString(new Date(entry.inactiveEndDate)),
        ticketID: entry.ticketID,
        partner: entry.partner
    };
}

function sensoredAugmentEntry(timesheetData, entry) {

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
        isGoogleDocImport: entry.isGoogleDocImport,
        inactiveEndDate: "",
        ticketID: entry.ticketID,
        partner: entry.partner
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

    //show inactive date only if it's different to the entry date
    if(!toDateString(new Date(entry.beginDate)).localeCompare(toDateString(new Date(entry.inactiveEndDate)))) {
        augmentedEntry = sensoredAugmentEntry(timesheetData, entry);
    }

    var viewRow = AJS.$(Jira.Templates.Timesheet.timesheetEntry(
        {entry: augmentedEntry, teams: timesheetData.teams}));

    viewRow.find('span.aui-icon-wait').hide();

    return viewRow;
}
