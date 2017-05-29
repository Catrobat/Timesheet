"use strict";

//global field
var timesheetEntry;

// variables to handle how error messages and flags appear/disappear
var deletingError;
var savingError;

var ppFlag;
var dateRangeFlag;
var dateFlag;
var descrFlag;

function populateTable(timesheetDataReply) {
    var timesheetData = timesheetDataReply[0];

    if (!isAdmin) {
        //activate html banner
        AJS.$("#inactive-header").show();
        //Information at the first use
        if (timesheetData.entries.length == 0) {
            AJS.messages.generic({
                title: 'Timesheet Information.',
                closeable: true,
                body: '<p> Congratulations you sucessfully created your own ' +
                'Timesheet. The Timesheet add-on provides tracking your time ' +
                'data in a comfortable way and offers several visualizations ' +
                'for your data. An Import-function for existing timesheet entries ' +
                'from CSV / Google Doc Timesheets is provided in addition. ' +
                'The required import steps are shown within the ' +
                '"Import from Google Docs - Dialog".</p>' +
                '<p> If you notice any uncommon plugin behaviour, or need support feel free to ' +
                'contact one of the project "Coordinators", or an "Administrator".</p>'
            });
        } else if (timesheetData.state === "AUTO_INACTIVE") {
            require(['aui/banner'], function (banner) {
                banner({
                    body: 'Your Timesheet is marked as <strong>' + timesheetData.state + '</strong>.'
                });
            });
        } else if (timesheetData.state === "DISABLED") {
            AJS.messages.warning({
                title: 'Timesheet Warning.',
                closeable: true,
                body: '<p> Your Timesheet is marked as <em>disabled</em>.</p>' +
                '<p> You are not able to apply any changes until it is "enabled" again by an Administrator.</p>'
            });

            require(['aui/banner'], function (banner) {
                banner({
                    body: 'Your Timesheet is marked as <strong>disabled</strong>.'
                });
            });
        } else if (timesheetData.state !== "ACTIVE") {
          AJS.messages.warning({
            title: 'Timesheet Warning.',
            closeable: true,
            body: '<p>Your Timesheet is marked as <em>' + timesheetData.state + '</em>.</p>'
          });
        } else if ((timesheetData.targetHours - timesheetData.targetHoursCompleted) <= 80) {

            // FIXME: is this banner needed?

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
        ajaxUrl: restBaseUrl + "timesheets/" + timesheetData.timesheetID + '/entry'
    };

    var emptyForm = renderFormRow(timesheetData, emptyEntry, addNewEntryOptions, false);
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

    replaceJiraTicketLinks();

    var indexOfInactive = getIDFromCategoryName("inactive", timesheetData);
    var indexOfDeactivated = getIDFromCategoryName("inactive & offline", timesheetData);
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
    var categoryName = getNameFromCategoryIndex(categoryIndex, timesheetData);
    if (!(categoryName.includes("(pp)") || categoryName.includes("pair"))) {
        AJS.$(".partner").hide();
    }
}

/**
 * Callback after editing an entry
 * @param {Object} entry
 * @param {Object} timesheetData
 * @param {jQuery} form
 */
function editEntryCallback(entry, timesheetData, form) {
    var augmentedEntry = augmentEntry(timesheetData, entry);
    if (augmentedEntry == null)
        return;

    var newViewRow = prepareViewRow(timesheetData, augmentedEntry);
    var oldViewRow = form.row.prev();

    newViewRow.find("button.edit").click(function () {
        newViewRow.hide();
        form.row.show();
    });

    newViewRow.find("button.delete").click(function () {
      showEntryDeletionDialog(newViewRow, entry.entryID);
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
function renderFormRow(timesheetData, entry, saveOptions, isModified) {

    if (entry.pause === "")
        entry.pause = "00:00";

    var form = prepareForm(entry, timesheetData, isModified);

    form.saveButton.click(function (event) {
        event.preventDefault();
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
function prepareForm(entry, timesheetData, isModified) {

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

    form.ticketSelect.show();
    //date time columns
    form.dateField
        .datePicker(
            {overrideBrowserDefault: true, languageCode: 'en'}
        );

    form.inactiveEndDateField
        .datePicker(
            {overrideBrowserDefault: true, languageCode: 'en'}
        );

    form.categorySelect.change(function () {
        form.saveButton.prop('disabled', false);

        var indexOfInactive = getIDFromCategoryName("inactive", timesheetData);
        var indexOfDeactivated = getIDFromCategoryName("inactive & offline", timesheetData);
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

            var date = form.inactiveEndDateField.val();
            checkIfDateIsInRange(date, form);
            form.descriptionField.attr("placeholder", "Reason for your inactivity");
            AJS.$(".inactive").fadeIn(2000);
        }
        else {
            form.descriptionField.attr("placeholder", "a short task description");

            form.inactiveEndDateField.val(""); // clear inactive field input

            AJS.$(".inactive").fadeOut(2000);

            form.pauseTimeField.show();
            form.durationField.show();

            AJS.$(".start").fadeIn(2000);
            AJS.$(".end").fadeIn(2000);
            AJS.$(".pause").fadeIn(2000);
            AJS.$(".duration").fadeIn(2000);
            AJS.$(".ticket").fadeIn(2000);

            // define special behaviour
            setTimeout(function () { //little hack we have to do
                if (getLengthOfArray(timesheetData.teams) > 1) {
                    AJS.$(".team").show();
                } else {
                    AJS.$(".team").hide();
                }
            }, 100);

            if (categoryIndex === indexOfPP) {
                setTimeout(function () { //little hack we have to do
                    AJS.$(".partner").show();
                    form.partnerSelect.show();
                }, 100);
            }
            else {
                setTimeout(function () {
                    AJS.$(".partner").hide();
                }, 100);
                form.partnerSelect.select2("val", "");
            }
        }
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
            updateCategorySelect(form.categorySelect, selectedTeamID, entry, timesheetData, isModified, form);
        })
        .auiSelect2("val", initTeamID)
        .trigger("change");

    form.partnerSelect.auiSelect2({
        tags: timesheetData.pairProgrammingGroup.sort(),
        width: 'resolve'
    });

    var baseUrl = AJS.params.baseURL; // we have to reassign it otherwise it would be undefined
    var tickets = [];
    var queryString = "/rest/api/2/issue/picker";
    var text="0 issues";

    AJS.$.ajax({
        type: 'GET',
        url: baseUrl + queryString,
        dataType: 'json',
        success: function (data) {
            data.sections[0].issues.forEach(function (issue) {
                tickets.push(issue.key + " : " + issue.summary);
            });
            var reg = /\d+/g;
            var number = data.sections[0].sub;
            if (number) {
                text = number.match(reg)[0] + " issues";
            }
        },
        error: function (jqXHR) {
            console.log("error message: " + jqXHR.responseText);
        },
        async: false
    });

    form.ticketSelect.auiSelect2({
        tags: tickets,
        width: 'resolve',
        placeholder: text,
        tooltiptext: "hekkkkk",
        placement: "auto"
    });

    return form;
}


function checkIfDateIsInRange(date, form) {
    var today = new Date();
    if (isDateMoreThanTwoMonthsAhead(date)) {
    	if (dateRangeFlag)
    		dateRangeFlag.close();
    	dateRangeFlag = AJS.flag({
            type: 'error',
            title: 'Wrong date range',
            body: 'The date is more than 2 months in advance. This is not allowed.',
            close: 'auto'
        });
        form.inactiveEndDateField.val("");
    }

    if (compareTime(date, today) == -1) {
    	if (dateFlag)
    		dateFlag.close();
    	dateFlag = AJS.flag({
            type: 'error',
            title: 'Invalid date',
            body: 'The date is behind or equal the current date.',
            close: 'auto'
        });
        form.inactiveEndDateField.val("");
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
function updateCategorySelect(categorySelect, selectedTeamID, entry, timesheetData, isModified, form) {

    var selectedTeam = timesheetData.teams[selectedTeamID];
    if (selectedTeam == null || selectedTeam.teamCategories == null) {
        return;
    }
    var categoriesPerTeam = filterAndSortCategoriesPerTeam(selectedTeam, timesheetData.categoryIDs);

    categorySelect.auiSelect2({data: categoriesPerTeam});

    if (isModified) {
        var cat_id = getIDFromCategoryName(entry.category);
        categorySelect.val(cat_id).trigger("change");
        form.partnerSelect.val(entry.partner);
    }
    else {
        var suitableIndex = getSuitableCatIndex(categoriesPerTeam);
        categorySelect.val(suitableIndex).trigger("change");
    }

}

function getSuitableCatIndex(categoriesPerTeam) {
    for (var k in categoriesPerTeam) {
        if (categoriesPerTeam.hasOwnProperty(k)) {
            var name = categoriesPerTeam[k].text.toLowerCase();
            if (name !== "inactive" && name !== "inactive & offline") {
                return categoriesPerTeam[k].id;
            }
        }
    }
}

function updateTimeField(form) {
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
    if (augmentedEntry == null)
        return;

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
      showEntryDeletionDialog(viewRow, entry.entryID);
    });

    return viewRow;
}

function editEntryClicked(timesheetData, augmentedEntry, editEntryOptions, viewRow) {
    var formRow = getFormRow(viewRow);

    if (formRow === undefined) {
        formRow = renderFormRow(timesheetData, augmentedEntry, editEntryOptions, true);
        viewRow.after(formRow);
    }

    //AJS.$("#entry-table").hide();
    //AJS.$(".entry-form").hide();
    viewRow.hide();
    formRow.show();
}

function showEntryDeletionDialog(viewRow, entryID) {
  var dialog = new AJS.Dialog({
    width: 520,
    height: 390,
    id: "timesheets-deletion-dialog",
    closeOnOutsideClick: true
  });

    var content = "<h1>Do you really want to delete the timesheet entry?</h1> <br>" +
      "<h2>This action cannot be undone.</h2>" +
      "<h2 style='color: red'><center><strong>Please confirm your action!</strong></center></h2>";

  dialog.addHeader("Timesheet Entry Deletion");
  dialog.addPanel("Confirm", content, "panel-body");

  dialog.addButton("Cancel", function () {
    dialog.remove();
  });

  dialog.addButton("OK", function () {
    deleteEntryClicked(viewRow, entryID);
    dialog.remove();
  });

  dialog.gotoPage(0);
  dialog.gotoPanel(0);

  dialog.show();
}

function deleteEntryClicked(viewRow, entryID) {
    var ajaxUrl = restBaseUrl + 'entries/' + entryID;

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
        	removeSavingAndDeletingErrorMessages();
            deletingError = AJS.messages.error({
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

    var teamName = "Default";
    if (timesheetData.teams[entry.teamID] != null) {
        teamName = timesheetData.teams[entry.teamID].teamName;
    }

    // TODO: remove after GoogleDocsImport is Disabled
    var categoryName;
    var catID = entry.categoryID;
    switch (catID) {
    case -1:
    	categoryName = "Theory";
    	break;
    case -2:
    	categoryName = "GoogleDocsImport";
    	break;
    case -3:
    	categoryName = "Theory (MT)";
		break;
	case -4:
		categoryName = "Meeting";
		break;
	case -5:
		categoryName = "Pair programming";
		break;
	case -6:
		categoryName = "Programming";
		break;
	case -7:
		categoryName = "Research";
		break;
	case -8:
		categoryName = "Planning Game";
		break;
	case -9:
		categoryName = "Refactoring";
		break;
	case -10:
		categoryName = "Refactoring (PP)";
		break;
	case -11:
		categoryName = "Code Acceptance";
		break;
	case -12:
		categoryName = "Organisational tasks";
		break;
	case -13:
		categoryName = "Discussing issues/Supporting/Consulting";
		break;
	case -14:
		categoryName = "Inactive";
		break;
	case -15:
		categoryName = "Other";
		break;
	case -16:
		categoryName = "Bug fixing (PP)";
		break;
	case -17:
		categoryName = "Bug fixing";
		break;
    default:
    	categoryName = timesheetData.categoryIDs[entry.categoryID].categoryName;
    }
//    if (entry.categoryID === -1) {
//        categoryName = "Theory";
//    } else if (entry.categoryID === -2) {
//        categoryName = "GoogleDocsImport";
//    } else {
//        categoryName = timesheetData.categoryIDs[entry.categoryID].categoryName;
//    }

    var pauseDate = new Date(entry.pauseMinutes * 1000 * 60);

    return {
        date: toDateString(new Date(entry.beginDate)),
        begin: toTimeString(new Date(entry.beginDate)),
        end: toTimeString(new Date(entry.endDate)),
        pause: (entry.pauseMinutes > 0) ? toUTCTimeString(pauseDate) : "",
        duration: toTimeString(calculateDuration(entry.beginDate, entry.endDate, pauseDate)),
        category: categoryName,
        team: teamName,
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

/**
 * Creates the viewrow
 * @param {Object} timesheetData
 * @param {Object} entry
 */
function prepareViewRow(timesheetData, entry) {

    var viewRow = AJS.$(Jira.Templates.Timesheet.timesheetEntry(
        {entry: entry, teams: timesheetData.teams}));

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
    if (beginDate > endDate) {
        endDate.setDate(endDate.getDate() + 1)
    }
    var pauseMin = pauseTime.getHours() * 60 + pauseTime.getMinutes();

    var inactiveEndDate = form.inactiveEndDateField.val();
    var validInactiveDateFormat = new Date(inactiveEndDate);

    if ((inactiveEndDate == "") || (!isValidDate(validInactiveDateFormat))) {
        inactiveEndDate = beginDate;
    }
    else {
        inactiveEndDate = inactiveEndDate.replace(/-/g, "/");
        inactiveEndDate = new Date(inactiveEndDate + " " + toTimeString(beginTime));
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

    if (categoryIndex == getIDFromCategoryName("inactive & offline", timesheetData) && form.inactiveEndDateField.val() == "") {

        require('aui/flag')({
            type: 'info',
            title: 'Attention: No inactive end date ',
            body: 'You may have forgotten to set the end date.',
            close: 'auto'
        });
        return;
    }

    if (isPairProgrammingCategorySelected(timesheetData, form)) {

    	if (ppFlag)
    		ppFlag.close();
    	ppFlag = AJS.flag({
            type: 'error',
            title: 'Pair Programming Partner is missing',
            body: 'Please select an partner',
            close: 'auto'
        });
    }

    if (!form.descriptionField.val()) {
    	if (descrFlag)
    		descrFlag.close();
    	descrFlag = AJS.flag({
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
        data: JSON.stringify(timesheetEntry),
        success: function (entryData) {
        	removeSavingAndDeletingErrorMessages();
            var augmentedEntry = augmentEntry(timesheetData, entryData);
            saveOptions.callback(augmentedEntry, timesheetData, form);
            AJS.$(".description_").val("");
        },
        error: function (error) {
            console.log(error);
            removeSavingAndDeletingErrorMessages();
            savingError = AJS.messages.error({
                title: 'There was an error while saving.',
                body: '<p>Reason: ' + error.responseText + '</p>'
            });
        }
    }).always(function () {
        form.loadingSpinner.hide();
        form.saveButton.prop('disabled', false);
    });
}

function removeSavingAndDeletingErrorMessages() {
	if(savingError)
		savingError.closeMessage();
	if(deletingError)
		deletingError.closeMessage();
}
