/*
 * Copyright 2016 Adrian Schnedlitz
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
var inactiveUsers = [];

AJS.toInit(function () {
    var baseUrl = AJS.params.baseURL;
    restBaseUrl = baseUrl + "/rest/timesheet/latest/";

    var teams = [];
    var editCategoryNameDialog, editTeamNameDialog;

    function scrollToAnchor(aid) {
        var aTag = AJS.$("a[name='" + aid + "']");
        AJS.$('html,body').animate({scrollTop: aTag.offset().top}, 'slow');
    }

    initDialog();

    function fetchData() {

        var allUsersFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'user/getUsers',
            contentType: "application/json"
        });

        var categoriesFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'config/getCategories',
            contentType: "application/json"
        });

        var groupsFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'user/getGroups',
            contentType: "application/json"
        });

        var schedulingFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'scheduling/getScheduling',
            contentType: "application/json"
        });
        AJS.$.when(schedulingFetched)
            .done(fillSchedulingData);

        AJS.$.when(allUsersFetched, categoriesFetched, groupsFetched)
            .done(populateForm)
            .fail(function (error) {
                AJS.messages.error({
                    title: 'There was an error while fetching user data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
                AJS.$(".loadingDiv").hide();
            });
    }

    function fillSchedulingData(scheduling) {
        AJS.$("#scheduling-inactive-time").val(scheduling.inactiveTime);
        AJS.$("#scheduling-offline-time").val(scheduling.offlineTime);
        AJS.$("#scheduling-remaining-time").val(scheduling.remainingTime);
        AJS.$("#scheduling-out-of-time").val(scheduling.outOfTime);
    }

    function populateForm(allUsers, allCategories, allGroups) {
        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'config/getConfig',
            dataType: "json",
            success: function (config) {
                //manage mail
                if (config.mailFromName)
                    AJS.$("#mail-from-name").val(config.mailFromName);
                if (config.mailFrom)
                    AJS.$("#mail-from").val(config.mailFrom);

                //manage mail subjects
                if (config.mailSubjectTime)
                    AJS.$("#mail-subject-out-of-time").val(config.mailSubjectTime);
                if (config.mailSubjectInactive)
                    AJS.$("#mail-subject-inactive").val(config.mailSubjectInactive);
                if (config.mailSubjectOffline)
                    AJS.$("#mail-subject-offline").val(config.mailSubjectOffline);
                if (config.mailSubjectActive)
                    AJS.$("#mail-subject-active").val(config.mailSubjectActive);
                if (config.mailSubjectEntry)
                    AJS.$("#mail-subject-entry-change").val(config.mailSubjectEntry);

                //manage mail bodies
                if (config.mailBodyTime)
                    AJS.$("#mail-body-out-of-time").val(config.mailBodyTime);
                if (config.mailBodyInactive)
                    AJS.$("#mail-body-inactive").val(config.mailBodyInactive);
                if (config.mailBodyOffline)
                    AJS.$("#mail-body-offline").val(config.mailBodyOffline);
                if (config.mailBodyActive)
                    AJS.$("#mail-body-active").val(config.mailBodyActive);
                if (config.mailBodyEntry)
                    AJS.$("#mail-body-entry-change").val(config.mailBodyEntry);
                if (config.readOnlyUsers)
                    AJS.$("#plugin-permission").val(config.readOnlyUsers);

                //build team list
                teams = [];
                AJS.$("#teams").empty();
                for (var i = 0; i < config.teams.length; i++) {
                    var team = config.teams[i];
                    teams.push(team['teamName']);

                    var tempTeamName = team['teamName'].replace(/\W/g, '-');
                    AJS.$("#teams").append("<h3>" + team['teamName'] +
                        "<button class=\"aui-button aui-button-subtle\" value=\"" + team['teamName'] + "\">" +
                        "<span class=\"aui-icon aui-icon-small aui-iconfont-edit\">Editing</span> Edit Team Name </button></h3><fieldset>");
                    AJS.$("#teams").append("<div class=\"field-group\"><label for=\"" + tempTeamName + "-coordinator\">Coordinator</label><input class=\"text coordinator\" type=\"text\" id=\"" + tempTeamName + "-coordinator\" value=\"" + team['coordinatorGroups'] + "\"></div>");
                    AJS.$("#teams").append("<div class=\"field-group\"><label for=\"" + tempTeamName + "-developer\">User</label><input class=\"text user\" type=\"text\" id=\"" + tempTeamName + "-developer\" value=\"" + team['developerGroups'] + "\"></div>");
                    AJS.$("#teams").append("<div class=\"field-group\"><label for=\"" + tempTeamName + "-category\">Category</label><input class=\"text category\" type=\"text\" id=\"" + tempTeamName + "-category\" value=\"" + team['teamCategoryNames'] + "\"></div>");
                    AJS.$("#teams").append("</fieldset>");
                }

                //Timesheet - plugin administrator picker
                AJS.$("#plugin-administration").auiSelect2({
                    placeholder: "Search for users and groups",
                    minimumInputLength: 0,
                    tags: true,
                    tokenSeparators: [",", " "],
                    ajax: {
                        url: baseUrl + "/rest/api/2/groupuserpicker",
                        dataType: "json",
                        data: function (term, page) {
                            return {query: term};
                        },
                        results: function (data, page) {
                            var select2data = [];
                            for (var i = 0; i < data.groups.groups.length; i++) {
                                select2data.push({
                                    id: "groups-" + data.groups.groups[i].name,
                                    text: data.groups.groups[i].name
                                });
                            }
                            for (var i = 0; i < data.users.users.length; i++) {
                                select2data.push({
                                    id: "users-" + data.users.users[i].name,
                                    text: data.users.users[i].name
                                });
                            }
                            return {results: select2data};
                        }
                    },
                    initSelection: function (elements, callback) {
                        var data = [];
                        var array = elements.val().split(",");
                        for (var i = 0; i < array.length; i++) {
                            data.push({id: array[i], text: array[i].replace(/^users-/i, "").replace(/^groups-/i, "")});
                        }
                        callback(data);
                    }
                });

                var approved = [];
                if (config.timesheetAdminGroups) {
                    for (var i = 0; i < config.timesheetAdminGroups.length; i++) {
                        approved.push({id: "groups-" + config.timesheetAdminGroups[i], text: config.timesheetAdminGroups[i]});
                    }
                }

                if (config.timesheetAdmins) {
                    for (var i = 0; i < config.timesheetAdmins.length; i++) {
                        approved.push({id: "users-" + config.timesheetAdmins[i], text: config.timesheetAdmins[i]});
                    }
                }

                // pair programming group
                if (config.pairProgrammingGroup) {
                    AJS.$("#pp-ldap").val(config.pairProgrammingGroup);
                }

                AJS.$("#plugin-administration").auiSelect2("data", approved);

                //list of all available LDAP users
                var userNameList = [];
                for (var i = 0; i < allUsers[0].length; i++) {
                    userNameList.push(allUsers[0][i]['userName']);
                }

                //list of all available categories
                var categoryList = [];
                for (var i = 0; i < allCategories[0].length; i++) {
                    categoryList.push(allCategories[0][i]['categoryName']);
                }

                //build category list
                AJS.$("#categories").empty();
                for (var i = 0; i < categoryList.length; i++) {
                    if (categoryList[i] === "Inactive" || categoryList[i] === "Inactive & Offline"
                        || categoryList[i] === "GoogleDocsImport" || categoryList[i] === "Theory"
                        || categoryList[i] === "Default Category (original cateogry got deleted)") {
                        AJS.$("#categories").append("<h3>" + categoryList[i] +
                            "<button class=\"aui-button aui-button-subtle\" disabled>" +
                            "<span>This category cannot be renamed or deleted</span></button></h3><fieldset>");
                        AJS.$("#categories").append("</fieldset>");
                    } else {
                        AJS.$("#categories").append("<h3>" + categoryList[i] +
                            "<button class=\"aui-button aui-button-subtle\" value=\"C-" + categoryList[i] + "\">" +
                            "<span class=\"aui-icon aui-icon-small aui-iconfont-edit\">Editing</span> Edit Category Name </button></h3><fieldset>");
                        AJS.$("#categories").append("</fieldset>");
                    }
                }

                //Pair Programmign - ldap group
                AJS.$("#pp-ldap").auiSelect2({
                    placeholder: "Select Pair Programming Group",
                    tags: allGroups[0].sort(),
                    tokenSeparators: [",", " "]
                });

                //Timesheet - supervisor picker
                AJS.$("#plugin-permission").auiSelect2({
                    placeholder: "Select read only users",
                    tags: userNameList.sort(),
                    tokenSeparators: [",", " "]
                });
                //Timesheet - team coordinator picker
                AJS.$(".coordinator").auiSelect2({
                    placeholder: "Select coordinators",
                    tags: userNameList.sort(),
                    tokenSeparators: [",", " "]
                });
                //Timesheet - team user picker
                AJS.$(".user").auiSelect2({
                    placeholder: "Select users",
                    tags: userNameList.sort(),
                    tokenSeparators: [",", " "]
                });
                //Timesheet - team category picker
                AJS.$(".category").auiSelect2({
                    placeholder: "Select categories",
                    tags: categoryList.sort(),
                    tokenSeparators: [",", " "]
                });

                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
                AJS.messages.error({
                    title: "Error!",
                    body: "Could not load 'Config'."
                });

                AJS.$(".loadingDiv").hide();
            }
        });
    }

    function updateConfig() {
        var config = {};

        config.mailFromName = AJS.$("#mail-from-name").val();
        config.mailFrom = AJS.$("#mail-from").val();

        config.mailSubjectTime = AJS.$("#mail-subject-out-of-time").val();
        config.mailSubjectInactive = AJS.$("#mail-subject-inactive").val();
        config.mailSubjectOffline = AJS.$("#mail-subject-offline").val();
        config.mailSubjectActive = AJS.$("#mail-subject-active").val();
        config.mailSubjectEntry = AJS.$("#mail-subject-entry-change").val();

        config.mailBodyTime = AJS.$("#mail-body-out-of-time").val();
        config.mailBodyInactive = AJS.$("#mail-body-inactive").val();
        config.mailBodyOffline = AJS.$("#mail-body-offline").val();
        config.mailBodyActive = AJS.$("#mail-body-active").val();
        config.mailBodyEntry = AJS.$("#mail-body-entry-change").val();

        config.readOnlyUsers = AJS.$("#plugin-permission").val();
        config.pairProgrammingGroup = AJS.$("#pp-ldap").val();

        var usersAndGroups = AJS.$("#plugin-administration").auiSelect2("val");
        var timesheetAdmins = [];
        var timesheetAdminGroups = [];
        for (var i = 0; i < usersAndGroups.length; i++) {
            if (usersAndGroups[i].match("^users-")) {
                timesheetAdmins.push(usersAndGroups[i].split("users-")[1]);
            } else if (usersAndGroups[i].match("^groups-")) {
                timesheetAdminGroups.push(usersAndGroups[i].split("groups-")[1]);
            }
        }

        config.timesheetAdmins = timesheetAdmins;
        config.timesheetAdminGroups = timesheetAdminGroups;

        config.teams = [];
        for (var i = 0; i < teams.length; i++) {
            var tempTeamName = teams[i].replace(/\W/g, '-');
            var tempTeam = {};
            tempTeam.teamName = teams[i];

            tempTeam.coordinatorGroups = AJS.$("#" + tempTeamName + "-coordinator").auiSelect2("val");
            for (var j = 0; j < tempTeam.coordinatorGroups.length; j++) {
                tempTeam.coordinatorGroups[j] = tempTeam.coordinatorGroups[j].replace(/^groups-/i, "");
            }

            tempTeam.developerGroups = AJS.$("#" + tempTeamName + "-developer").auiSelect2("val");
            for (var j = 0; j < tempTeam.developerGroups.length; j++) {
                tempTeam.developerGroups[j] = tempTeam.developerGroups[j].replace(/^groups-/i, "");
            }

            tempTeam.teamCategoryNames = AJS.$("#" + tempTeamName + "-category").auiSelect2("val");
            for (var j = 0; j < tempTeam.teamCategoryNames.length; j++) {
                tempTeam.teamCategoryNames[j] = tempTeam.teamCategoryNames[j].replace(/^groups-/i, "");
            }
            config.teams.push(tempTeam);
        }

        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'config/saveConfig',
            type: "PUT",
            contentType: "application/json",
            data: JSON.stringify(config),
            processData: false,
            success: function () {
                AJS.messages.success({
                    title: "Success!",
                    body: "Settings saved!"
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

    function addTeam() {
        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'config/addTeamPermission',
            type: "PUT",
            contentType: "application/json",
            data: AJS.$("#team-name").attr("value"),
            processData: false,
            success: function () {
                AJS.messages.success({
                    title: "Success!",
                    body: "'Team' added successfully."
                });
                //empty the team name text field
                AJS.$("#team-name").val("");
                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
                AJS.messages.error({
                    title: "Error!",
                    body: "Could not add 'Team'.<br />" + error.responseText
                });
                AJS.$(".loadingDiv").hide();
            }
        });
    }

    function addCategory() {
        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'config/addCategory',
            type: "PUT",
            contentType: "application/json",
            data: AJS.$("#category-name").attr("value"),
            processData: false,
            success: function () {
                AJS.messages.success({
                    title: "Success!",
                    body: "'Category' added successfully."
                });
                //empty the category name text field
                AJS.$("#category-name").val("");
                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
                AJS.messages.error({
                    title: "Error!",
                    body: "Could not add 'Category'<br />" + error.responseText
                });
                AJS.$(".loadingDiv").hide();
            }
        });
    }

    function editTeam(teamName) {
        // may be in background and therefore needs to be removed
        if (editTeamNameDialog) {
            try {
                editTeamNameDialog.remove();
            } catch (err) {
                // may be removed already
            }
        }

        editTeamNameDialog = new AJS.Dialog({
            width: 600,
            height: 200,
            id: "edit-team-name-dialog",
            closeOnOutsideClick: true
        });

        var content = "<form class=\"aui\">\n" +
            "    <fieldset>\n" +
            "        <div class=\"field-group\">\n" +
            "            <label for=\"new-team-name\">New Team Name</label>\n" +
            "            <input class=\"text\" type=\"text\" id=\"new-team-name\" name=\"new-team-name\" title=\"new-team-name\">\n" +
            "        </div>\n" +
            "    </fieldset>\n" +
            " </form> ";

        editTeamNameDialog.addHeader("New Team Name for " + teamName);
        editTeamNameDialog.addPanel("Panel 1", content, "panel-body");
        editTeamNameDialog.addButton("Save", function (dialog) {
            AJS.$(".loadingDiv").show();
            AJS.$.ajax({
                url: restBaseUrl + 'config/editTeamName',
                type: "PUT",
                contentType: "application/json",
                data: JSON.stringify([teamName, AJS.$("#new-team-name").val()]),
                processData: false,
                success: function () {
                    AJS.messages.success({
                        title: "Success!",
                        body: "'Team' renamed successfully."
                    });
                    fetchData();
                    AJS.$(".loadingDiv").hide();
                },
                error: function (error) {
                    AJS.messages.error({
                        title: "Error!",
                        body: "Could not rename 'Team'.<br />" + error.responseText
                    });
                    scrollToAnchor('top');
                    AJS.$(".loadingDiv").hide();
                }
            });

            dialog.remove();
        });
        editTeamNameDialog.addLink("Cancel", function (dialog) {
            dialog.remove();
        }, "#");

        editTeamNameDialog.gotoPage(0);
        editTeamNameDialog.gotoPanel(0);
        editTeamNameDialog.show();
    }

    function editCategory(categoryName) {
        //remove prefix
        categoryName = categoryName.slice(2);
        // may be in background and therefore needs to be removed
        if (editCategoryNameDialog) {
            try {
                editCategoryNameDialog.remove();
            } catch (err) {
                // may be removed already
            }
        }

        editCategoryNameDialog = new AJS.Dialog({
            width: 600,
            height: 200,
            id: "edit-category-name-dialog",
            closeOnOutsideClick: true
        });

        var content = "<form class=\"aui\">\n" +
            "    <fieldset>\n" +
            "        <div class=\"field-group\">\n" +
            "            <label for=\"new-category-name\">New Category Name</label>\n" +
            "            <input class=\"text\" type=\"text\" id=\"new-category-name\" name=\"new-category-name\" title=\"new-category-name\">\n" +
            "        </div>\n" +
            "    </fieldset>\n" +
            " </form> ";

        editCategoryNameDialog.addHeader("New Category Name for " + categoryName);
        editCategoryNameDialog.addPanel("Panel 2", content, "panel-body");

        editCategoryNameDialog.addButton("Save", function (dialog) {
            AJS.$(".loadingDiv").show();
            AJS.$.ajax({
                url: restBaseUrl + 'config/editCategoryName',
                type: "PUT",
                contentType: "application/json",
                data: JSON.stringify([categoryName, AJS.$("#new-category-name").val()]),
                processData: false,
                success: function () {
                    AJS.messages.success({
                        title: "Success!",
                        body: "'Category' renamed. successfully"
                    });
                    fetchData();
                    AJS.$(".loadingDiv").hide();
                },
                error: function (error) {
                    AJS.messages.error({
                        title: "Error!",
                        body: "Could not rename 'Category'<br />" + error.responseText
                    });
                    scrollToAnchor('top');
                    AJS.$(".loadingDiv").hide();
                }
            });

            dialog.remove();
        });
        editCategoryNameDialog.addLink("Cancel", function (dialog) {
            dialog.remove();
        }, "#");

        editCategoryNameDialog.gotoPage(0);
        editCategoryNameDialog.gotoPanel(0);
        editCategoryNameDialog.show();
    }

    function removeTeam() {
        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'config/removeTeamPermission',
            type: "PUT",
            contentType: "application/json",
            data: AJS.$("#team-name").attr("value"),
            processData: false,
            success: function () {
                AJS.messages.success({
                    title: "Success!",
                    body: "'Team' deleted successfully."
                });
                //empty the team name text field
                AJS.$("#team-name").val("");
                AJS.$(".loadingDiv").hide();
            },
            error: function () {
                AJS.messages.error({
                    title: "Error!",
                    body: "Could not delete 'Team'."
                });
                AJS.$(".loadingDiv").hide();
            }
        });
    }

    function removeCategory() {
        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'config/removeCategory',
            type: "PUT",
            contentType: "application/json",
            data: AJS.$("#category-name").attr("value"),
            processData: false,
            success: function () {
                AJS.messages.success({
                    title: "Success!",
                    body: "'Category' deleted successfully."
                });
                //empty the team name text field
                AJS.$("#category-name").val("");
                AJS.$(".loadingDiv").hide();
            },
            error: function (xhr) {
                AJS.messages.error({
                    title: "Error!",
                    body: xhr.responseText
                });
                AJS.$(".loadingDiv").hide();
            }
        });
    }

    fetchData();

    AJS.$("#general").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === 'Save') {
            updateConfig();
            scrollToAnchor('top');
        } else if ((AJS.$(document.activeElement).val()[0] === "C") &&
            (AJS.$(document.activeElement).val()[1] === "-")) {
            editCategory(AJS.$(document.activeElement).val());
        } else {
            editTeam(AJS.$(document.activeElement).val());
        }
    });

    AJS.$("#team-general").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === 'Save') {
            updateConfig();
            scrollToAnchor('top');
        } else if ((AJS.$(document.activeElement).val()[0] === "C") &&
            (AJS.$(document.activeElement).val()[1] === "-")) {
            editCategory(AJS.$(document.activeElement).val());
        } else {
            editTeam(AJS.$(document.activeElement).val());
        }
    });

    AJS.$("#category-general").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === 'Save') {
            updateConfig();
            scrollToAnchor('top');
        } else if ((AJS.$(document.activeElement).val()[0] === "C") &&
            (AJS.$(document.activeElement).val()[1] === "-")) {
            editCategory(AJS.$(document.activeElement).val());
        } else {
            editTeam(AJS.$(document.activeElement).val());
        }
    });

    AJS.$("#modify-team").submit(function (e) {
        e.preventDefault();
        addTeam();
        scrollToAnchor('top');
    });

    AJS.$("#removeTeam").click(function (e) {
        e.preventDefault();
        removeTeam();
        scrollToAnchor('top');
    });

    AJS.$("#modify-categoryList").submit(function (e) {
        e.preventDefault();
        addCategory();
        scrollToAnchor('top');
    });

    AJS.$("#removeCategory").click(function (e) {
        e.preventDefault();
        removeCategory();
        scrollToAnchor('top');
    });

    AJS.$("#modify-scheduling").submit(function (e) {
        e.preventDefault();
        if (AJS.$(document.activeElement).val() === 'Activity Verification') {
            triggerJobManually("trigger/activity/verification", "Activity-Verification");
        } else if (AJS.$(document.activeElement).val() === 'Activity Notification') {
            triggerJobManually("trigger/activity/notification", "Activity-Notification");
        } else if (AJS.$(document.activeElement).val() === 'Out Of Time') {
            triggerJobManually("trigger/out/of/time/notification", "Out-Of-Time-Notification");
        } else if (AJS.$(document.activeElement).val() === 'Save') {
            updateScheduling();
        }
    });

    function updateScheduling() {
        var scheduling = {};

        scheduling.inactiveTime = AJS.$("#scheduling-inactive-time").val();
        scheduling.offlineTime = AJS.$("#scheduling-offline-time").val();
        scheduling.remainingTime = AJS.$("#scheduling-remaining-time").val();
        scheduling.outOfTime = AJS.$("#scheduling-out-of-time").val();

        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'scheduling/saveScheduling',
            type: "PUT",
            contentType: "application/json",
            data: JSON.stringify(scheduling),
            processData: false,
            success: function () {
                AJS.messages.success({
                    title: "Success!",
                    body: "Settings saved!"
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

    function triggerJobManually(restUrl, jobName) {
        var callREST = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'scheduling/' + restUrl,
            contentType: "application/json"
        });

        AJS.$.when(callREST)
            .done(function (success) {
                AJS.messages.success({
                    title: "Success!",
                    body: jobName + " Job triggered successfully."
                });
                fetchData();
                AJS.$(".loadingDiv").hide();
            })
            .fail(function (error) {
                AJS.messages.error({
                    title: 'There was an error while triggering the Job REST url: ' + restUrl,
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
                AJS.$(".loadingDiv").hide();
            });
    }

    AJS.$("a[href='#tabs-general']").click(function () {
        AJS.$("#teams").html("");
        fetchData();
    });

    function unescapeHtml(safe) {
        if (safe) {
            return AJS.$('<div />').html(safe).text();
        } else {
            return '';
        }
    }

    AJS.$(document).keydown(function (e) {
        var keyCode = e.keyCode || e.which;
        if (e.ctrlKey && e.altKey && e.shiftKey) {
            if (keyCode == 84) { // keycode == 't'
                loadData();
            }
        }
        //console.log(event.keyCode);
    });

    function loadData() {
        var inactiveUsersFetched = AJS.$.ajax({
            type: 'GET',
            url: restBaseUrl + 'inactiveUsers',
            contentType: "application/json"
        });

        AJS.$.when(inactiveUsersFetched)
            .done(function (inactiveList){
                inactiveUsers = inactiveList;
                AJS.dialog2("#hidden-dialog").show();
            })
            .fail(function (error) {
                AJS.messages.error({
                    title: 'There was an error while fetching user data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
                AJS.$(".loadingDiv").hide();
            });
    }

    function initDialog() {

        AJS.$("#dialog-submit-button").click(function (e) {
            e.preventDefault();
            AJS.dialog2("#hidden-dialog").hide();
        });

        // Show event - this is triggered when the dialog is shown
        AJS.dialog2("#hidden-dialog").on("show", function () {
            AJS.$(".aui").focus();
            AJS.log("hidden-dialog was shown");
            AJS.$(".aui-dialog2-footer-hint").html("Created on " + new Date().toDateString());

            var content = "";
            inactiveUsers.forEach(function (user) {
                content += user + "<br/>";
            });

            AJS.$(".aui-dialog2-content").html(content);
        });

        AJS.$("#search-field").keyup(function (e) {
            var keyCode = e.keyCode || e.which;
            if (keyCode === 13) {
                e.preventDefault();
                return false;
            }
            var searchText = AJS.$("#search-field").val().toLowerCase();
            var content = "";
            for (var i = 0; i < inactiveUsers.length; i++) {
                if (inactiveUsers[i].toLowerCase().includes(searchText)) {
                    content += inactiveUsers[i] + "<br/>";
                }
            }
            AJS.$(".aui-dialog2-content").html(content);
        });

//     // Hide event - this is triggered when the dialog is hidden
//     AJS.dialog2("#hidden-dialog").on("hide", function () {
//         AJS.log("hidden-dialog was hidden");
//     });
//
// // Global show event - this is triggered when any dialog is show
//     AJS.dialog2.on("show", function () {
//         AJS.log("a dialog was shown");
//     });
//
// // Global hide event - this is triggered when any dialog is hidden
//     AJS.dialog2.on("hide", function () {
//         AJS.log("a dialog was hidden");
//     });
    }
});

