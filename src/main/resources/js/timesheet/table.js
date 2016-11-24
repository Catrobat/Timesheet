"use strict";

//global field
var timesheetEntry;

function populateTable(timesheetDataReply) {
    var timesheetData = timesheetDataReply[0];

    if (!isAdmin) {
        //activate html banner
        AJS.$("#inactive-header").show();
        //Information at the first use
        if (timesheetData.entries.length == 0)
            AJS.messages.generic({
                title: 'Timesheet Information.',
                closeable: true,
                body: '<p> Congratulations you sucessfully created your own ' +
                'Timesheet. The Timesheet add-on provides tracking your time ' +
                'data in a comfortable way and offers several visualizations' +
                'for your data. An Import-function for existing timesheet entries ' +
                'from CSV / Google Doc Timesheets is provided in addition. ' +
                'The required import steps are shown within the ' +
                '"Import from Google Docs - Dialog".</p>' +
                '<p> If you notice any uncommon plugin behaviour, or need support feel free to ' +
                'contact one of the project "Coordinators", or an "Administrator".</p>'
            });
        else if (timesheetData.isOffline) {
            AJS.messages.warning({
                title: 'Timesheet Warning.',
                closeable: true,
                body: '<p> Your Timesheet is marked as <em>offline</em>, because you weren\'t active for a very long time.</p>'
            });

            require(['aui/banner'], function (banner) {
                banner({
                    body: 'Your Timesheet is marked as <strong>offline</strong>.'
                });
            });
        }
        else if (timesheetData.isAutoInactive) {
            AJS.messages.warning({
                title: 'Timesheet Warning.',
                closeable: true,
                body: '<p> Your Timesheet was marked as <em>inactive</em> by the system, ' +
                'because you wasn\'t active for a longer period in the past.</p>'
            });

            require(['aui/banner'], function (banner) {
                banner({
                    body: 'Your Timesheet is marked as <strong>inactive</strong>.'
                });
            });
        }
        //Banner Informations for the User
        else if (!timesheetData.isActive) {
            AJS.messages.warning({
                title: 'Timesheet Warning.',
                closeable: true,
                body: '<p> Your Timesheet is marked as <em>inactive</em>, because you made an inactive entry.</p>'
            });

            require(['aui/banner'], function (banner) {
                banner({
                    body: 'Your Timesheet is marked as <strong>inactive</strong>.'
                });
            });
        }
        else if (!timesheetData.isEnabled) {
            AJS.messages.warning({
                title: 'Timesheet Warning.',
                closeable: true,
                body: '<p> Your Timesheet is marked as <em>disabled</em>.</p>' +
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
                closeable: true,
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
        else {
            AJS.$("#inactive-header").hide();
        }
    }

    var timesheetTable = AJS.$("#timesheet-table");
    timesheetTable.empty();

    timesheetTable.append(Jira.Templates.Timesheet.timesheetHeader(
        {teams: timesheetData.teams}
    ));

    var actualDate = new Date();
    var minutes = (actualDate.getMinutes() < 10 ? '0' : '') + actualDate.getMinutes();
    var hours = (actualDate.getHours() < 10 ? '0' : '') + actualDate.getHours();

    var emptyEntry = {
        entryID: "new-id",
        date: toDateString(actualDate),
        begin: hours + ":" + minutes,
        end: (parseInt(hours) + 1) + ":" + minutes,
        inactiveEndDate: "",
        deactivateEndDate: "",
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

    form.categorySelect.trigger("change"); // this is needed for the sparkling effect

    var indexOfInactive = getIDFromCategoryName("inactive", timesheetData);
    var indexOfDeactivated = getIDFromCategoryName("deactivated", timesheetData);
    var categoryIndex = form.categorySelect.val();

    if (indexOfInactive == categoryIndex || indexOfDeactivated == categoryIndex) {
        AJS.$(".ticket").hide();
        AJS.$(".partner").hide();
        AJS.$(".team").hide();
        AJS.$(".duration").hide();
        AJS.$(".pause").hide();
        AJS.$(".end").hide();
        AJS.$(".start").hide();
    }
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
        submit(timesheetData, saveOptions, form, entry.entryID,
            entry.isGoogleDocImport);
        //AJS.$(".entry-form").show();
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
        inactiveEndDateField: row.find('input.inactive_'),
        deactivateEndDateField: row.find('input.deactivate_'),
        beginTimeField: row.find('input.time.start'),
        endTimeField: row.find('input.time.end'),
        pauseTimeField: row.find('input.time.pause'),
        durationField: row.find('input.duration'),
        descriptionField: row.find('input.description_'),
        ticketSelect: row.find('input.ticket_'),
        categorySelect: row.find('span.category_'),
        partnerSelect: row.find('span.partner_'),
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

    form.deactivateEndDateField
        .datePicker(
            {overrideBrowserDefault: true, languageCode: 'en'}
        );

    form.inactiveEndDateField.change(function () {
        // Info: you can also write AJS.$("input.description").val("hello boy");
        var index = getIDFromCategoryName("inactive", timesheetData);
        form.categorySelect.auiSelect2("val", index);
        form.categorySelect.trigger("change");
    });

    form.deactivateEndDateField.change(function () {
        AJS.$("input.description_").attr("placeholder", "Reason(s) for your deactivation");
        var index = getIDFromCategoryName("deactivated", timesheetData);
        form.categorySelect.auiSelect2("val", index);
        form.categorySelect.trigger("change");
    });

    form.categorySelect.change(function () {
        form.saveButton.prop('disabled', false);

        var indexOfInactive = getIDFromCategoryName("inactive", timesheetData);
        var indexOfDeactivated = getIDFromCategoryName("deactivated", timesheetData);
        var indexOfPP = -1;
        var categoryIndex = form.categorySelect.val();

        var categoryName = getNameFromCategoryIndex(categoryIndex, timesheetData).toLowerCase();
        if (categoryName.includes("(pp)") || categoryName.includes("pair")) {
            categoryIndex = indexOfPP;
        }

        if (categoryIndex == indexOfInactive || categoryIndex == indexOfDeactivated) {
            form.beginTimeField.val('00:00');
            form.endTimeField.val('00:00');
            form.pauseTimeField.val('00:00');
            form.durationField.val('00:00');
            form.ticketSelect.select2("val", "");
            form.partnerSelect.select2("val", "");

            AJS.$(".ticket").fadeOut(2000);
            AJS.$(".partner").fadeOut(2000);
            AJS.$(".team").fadeOut(2000);
            AJS.$(".duration").fadeOut(2000);
            AJS.$(".pause").fadeOut(2000);
            AJS.$(".end").fadeOut(2000);
            AJS.$(".start").fadeOut(2000);

            if (categoryIndex == indexOfInactive) {
                var date = form.inactiveEndDateField.val();
                checkIfDateIsInRange(date, form);
                AJS.$("input.description_").attr("placeholder", "Reason for your inactivity");
                form.deactivateEndDateField.val("");
                AJS.$(".inactive").fadeIn(2000);
                AJS.$(".deactivate").fadeOut(2000);
            }
            else {
                var date = form.deactivateEndDateField.val();
                checkIfDateIsInRange(date, form);
                AJS.$("input.description_").attr("placeholder", "Reason(s) for your deactivation");
                form.inactiveEndDateField.val("");
                AJS.$(".deactivate").fadeIn(2000);
                AJS.$(".inactive").fadeOut(2000);
            }
        }
        else {
            AJS.$("input.description_").attr("placeholder", "a short task description");

            form.inactiveEndDateField.val(""); // clear inactive field input
            form.deactivateEndDateField.val(""); // clear deactivated field input

            AJS.$(".inactive").fadeOut(2000);
            AJS.$(".deactivate").fadeOut(2000);

            form.pauseTimeField.show();
            form.durationField.show();

            AJS.$(".start").fadeIn(2000);
            AJS.$(".end").fadeIn(2000);
            AJS.$(".pause").fadeIn(2000);
            AJS.$(".duration").fadeIn(2000);
            AJS.$(".team").fadeIn(2000);
            AJS.$(".ticket").fadeIn(2000);
            AJS.$(".partner").fadeIn(2000);

            if (categoryIndex === indexOfPP) {
                AJS.$(".partner").show();
            }
            else {
                AJS.$(".partner").hide();
            }
        }
    });

    form.partnerSelect.change(function () {
        var index = getIDFromCategoryName("Pair programming", timesheetData);
        form.categorySelect.auiSelect2("val", index);
    });

    form.descriptionField.change(function () {
        form.saveButton.prop('disabled', false);
        validation(form.descriptionField);
    });

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

    form.partnerSelect.auiSelect2({
        tags: timesheetData.users.sort(),
        width: 'resolve'
    });

    var baseUrl = AJS.params.baseURL; // we have to reassign it otherwise it would be undefined
    var queryString = "/rest/api/2/project?recent=20";
    var projectKeys = [];

    AJS.$.ajax({
        type: 'GET',
        url: baseUrl + queryString,
        dataType: 'json',
        success: function (data) {
            for (var i = 0; i < data.length; i++) {
                var key = data[i].key;
                projectKeys[i] = key;
            }
        },
        error: function (jqXHR) {
            console.log("error message: " + jqXHR.responseText);
        },
        async: false
    });

    var tickets = [];
    AJS.$.each(projectKeys, function (index, value) {
        if (value === undefined) {
            value = null; // null is a accepted value, but undefined is going worse
        }
        var queryString = "/rest/api/latest/search?jql=project%20in%20(" + value +
            ")%20AND%20(status%20not%20in%20(closed))";

        AJS.$.ajax({
            type: 'GET',
            url: baseUrl + queryString,
            dataType: 'json',
            success: function (data) {
                for (var i = 0; i < data.total; i++) {
                    if (data.issues[i] && data.issues[i].key) {
                        var key = data.issues[i].key;
                        var summary = data.issues[i].fields.summary;
                        tickets.push(key + " : " + summary);
                    }
                }
                console.log("Amount of fetched issues: " + i);

            },
            error: function (jqXHR) {
                console.log("error message: " + jqXHR.responseText);
            },
            async: false
        });
    });

    form.ticketSelect.auiSelect2({
        tags: tickets,
        width: 'resolve'
    });

    if (countDefinedElementsInArray(teams) < 2) {
        row.find(".team").hide();
        AJS.$(".team").hide();
    }

    return form;
}

function checkIfDateIsInRange(date, form) {
    var today = new Date();
    if (isDateMoreThanTwoMonthsAhead(date)) {
        require('aui/flag')({
            type: 'error',
            title: 'Wrong date range',
            body: 'The date is more than 2 months in advance. This is not allowed.'
        });
        form.inactiveEndDateField.val("");
        form.deactivateEndDateField.val("");
    }

    if (compareTime(date, today) == -1) {
        require('aui/flag')({
            type: 'error',
            title: 'Invalid date',
            body: 'The date is behind or equal the current date.'
        });
        form.inactiveEndDateField.val("");
        form.deactivateEndDateField.val("");
    }
}

function getIDFromCategoryName(categoryName) {
    var orig_array = timesheetData_.categoryNames;
    var dup_array = [];
    for (var key in orig_array) {
        dup_array[key.toLowerCase()] = orig_array[key];
    }

    /* Info: iterate through key, value pair
     for (var k in dup_array){
     if (dup_array.hasOwnProperty(k)) {
     console.log("Key is " + k + ", value is" + dup_array[k]);
     }
     }*/
    return dup_array[categoryName.toLowerCase()];
}

// input validation
function validation(form) {
    var str = form.val().toLowerCase();
    if (str.indexOf("<script>") !== -1) { // this increases the performance, because regex validation is high computationally
        var isMatching = str.match(/.*<script>.*<\/script>/i);
        if (isMatching) {
            alert("Don't try to hack this, you nasty bastard! You have no change! ;)");
            form.val("");
        }
    }
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
    if (selectedTeam == null || selectedTeam.teamCategories == null) {
        return;
    }
    var categoriesPerTeam = filterAndSortCategoriesPerTeam(selectedTeam, timesheetData.categoryIDs);

    categorySelect.auiSelect2({data: categoriesPerTeam});

    var suitableIndex = getSuitableCatIndex(categoriesPerTeam);
    categorySelect.val(suitableIndex).trigger("change");
}

function getSuitableCatIndex(categoriesPerTeam) {
    for (var k in categoriesPerTeam) {
        if (categoriesPerTeam.hasOwnProperty(k)) {
            var name = categoriesPerTeam[k].text.toLowerCase();
            if (name !== "inactive" && name !== "deactivated") {
                return categoriesPerTeam[k].id;
            }
        }
    }
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

    //AJS.$("#entry-table").hide();
    //AJS.$(".entry-form").hide();
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
        category: timesheetData.categoryIDs[entry.categoryID].categoryName,
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
        deactivateEndDate: toDateString(new Date(entry.deactivateEndDate)),
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

    var augmentedEntry = augmentEntry(timesheetData, entry);

    var viewRow = AJS.$(Jira.Templates.Timesheet.timesheetEntry(
        {entry: augmentedEntry, teams: timesheetData.teams}));

    viewRow.find('span.aui-icon-wait').hide();

    return viewRow;
}

/**
 * Handles saving an timesheetEntry
 * @param {Object} timesheetData
 * @param {Object} saveOptions
 *           callback   : Function(timesheetEntry, timesheetData, form)
 *           ajaxUrl    : String
 *           httpMethod : String
 * @param {jQuery} form
 * @returns {undefined}
 */
function submit(timesheetData, saveOptions, form, existingEntryID,
                existingIsGoogleDocImportValue) {
    form.saveButton.prop('disabled', true);

    var date = form.dateField.val();
    var validDateFormat = new Date(date);
    var categoryIndex = form.categorySelect.val();

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

    date = date.replace(/-/g, "/");
    var pauseTime = form.pauseTimeField.timepicker('getTime');
    var beginDate = new Date(date + " " + toTimeString(beginTime));
    var endDate = new Date(date + " " + toTimeString(endTime));
    var pauseMin = pauseTime.getHours() * 60 + pauseTime.getMinutes();

    var inactiveEndDate = form.inactiveEndDateField.val();
    var validInactiveDateFormat = new Date(inactiveEndDate);

    var deactivateEndDate = form.deactivateEndDateField.val();
    var validDeactivateDateFormat = new Date(deactivateEndDate);

    if ((inactiveEndDate == "") || (!isValidDate(validInactiveDateFormat))) {
        inactiveEndDate = beginDate;
    }
    else {
        inactiveEndDate = inactiveEndDate.replace(/-/g, "/");
        inactiveEndDate = new Date(inactiveEndDate + " " + toTimeString(beginTime));
    }

    if ((deactivateEndDate == "") || (!isValidDate(validDeactivateDateFormat))) {
        deactivateEndDate = beginDate;
    }
    else {
        deactivateEndDate = deactivateEndDate.replace(/-/g, "/");
        deactivateEndDate = new Date(deactivateEndDate + " " + toTimeString(beginTime));
    }

    if (categoryIndex == getIDFromCategoryName("inactive", timesheetData) && form.inactiveEndDateField.val() == "") {

        require('aui/flag')({
            type: 'error',
            title: 'Attention: No inactive end date ',
            body: 'You may have forgotten to set the end date.',
            close: 'auto'
        });
        return;
    }

    if (categoryIndex == getIDFromCategoryName("deactivated", timesheetData) && form.deactivateEndDateField.val() == "") {

        require('aui/flag')({
            type: 'info',
            title: 'Attention: No deactivate end date ',
            body: 'You may have forgotten to set the end date.',
            close: 'auto'
        });
        return;
    }

    var categoryName = getNameFromCategoryIndex(categoryIndex, timesheetData).toLowerCase();
    if ((categoryName.includes("(pp)") || categoryName.includes("pair")) && !form.partnerSelect.val()) {

        require('aui/flag')({
            type: 'error',
            title: 'Pair Programming Partner is missing',
            body: 'Please select an partner',
            close: 'auto'
        });
    }

    if (!form.descriptionField.val()) {
        require('aui/flag')({
            type: 'warning',
            title: 'Description message is missing',
            body: 'You need to write a short summary about what you have done so far.',
            close: 'auto'
        });

        form.descriptionField.css({
            "border-color": "red"
        });
        return;
    }
    else {

        form.descriptionField.css({
            "border-color": "#DCDCDC"
        });

    }

    timesheetEntry = {
        beginDate: beginDate,
        endDate: endDate,
        inactiveEndDate: inactiveEndDate,
        deactivateEndDate: deactivateEndDate,
        description: form.descriptionField.val(),
        pauseMinutes: pauseMin,
        teamID: form.teamSelect.val(),
        categoryID: form.categorySelect.val(),
        isGoogleDocImport: existingIsGoogleDocImportValue,
        partner: form.partnerSelect.val(),
        ticketID: form.ticketSelect.val()
    };

    if (existingEntryID !== "new-id") {
        timesheetEntry.entryID = existingEntryID;
    }

    form.loadingSpinner.show();

    AJS.$.ajax({
        type: saveOptions.httpMethod,
        url: saveOptions.ajaxUrl,
        contentType: "application/json",
        data: JSON.stringify(timesheetEntry) //causes error in FIREFOX
    })
        .then(function (entryData) {
            var augmentedEntry = augmentEntry(timesheetData, entryData);
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
