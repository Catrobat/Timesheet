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

    var teams = [];
    var approvedUsers = [];
    var editCategoryNameDialog, editTeamNameDialog;

    function scrollToAnchor(aid) {
        var aTag = AJS.$("a[name='" + aid + "']");
        AJS.$('html,body').animate({scrollTop: aTag.offset().top}, 'slow');
    }

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

        AJS.$.when(allUsersFetched, categoriesFetched)
            .done(populateForm)
            .done(populateApprovedUsers)
            .fail(function (error) {
                AJS.messages.error({
                    title: 'There was an error while fetching user data.',
                    body: '<p>Reason: ' + error.responseText + '</p>'
                });
                console.log(error);
                AJS.$(".loadingDiv").hide();
            });
    }

    function populateApprovedUsers(allUsers) {
        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'config/getConfig',
            dataType: "json",
            success: function (config) {
                AJS.$("#permissions").empty();
                AJS.$("#permissions").append("<fieldset>");
                AJS.$("#permissions").append("<div class=\"field-group\"><label for=\"approvedUser\">Approved User</label><input class=\"text approved\" type=\"text\" id=\"approvedUser\" value=\"" + config.approvedUsers + "\"></div>");
                AJS.$("#permissions").append("</fieldset>");

                var userNameList = [];
                for (var i = 0; i < allUsers[0].length; i++) {
                    userNameList.push(allUsers[0][i]['userName']);
                }

                AJS.$(".approved").auiSelect2({
                    placeholder: "Search for user",
                    tags: userNameList.sort(),
                    tokenSeparators: [",", " "]
                });

                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
                AJS.messages.error({
                    title: "Error!",
                    body: "Something went wrong while loading the config file!"
                });

                AJS.$(".loadingDiv").hide();
            }
        });
    }

    function populateForm(allUsers, allCategories) {
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
                //mange mail subjects
                if (config.mailSubjectTime)
                    AJS.$("#mail-subject-out-of-time").val(config.mailSubjectTime);
                if (config.mailSubjectInactive)
                    AJS.$("#mail-subject-inactive").val(config.mailSubjectInactive);
                if (config.mailSubjectEntry)
                    AJS.$("#mail-subject-entry-change").val(config.mailSubjectEntry);
                //mange mail bodies
                if (config.mailBodyTime)
                    AJS.$("#mail-body-out-of-time").val(config.mailBodyTime);
                if (config.mailBodyInactive)
                    AJS.$("#mail-body-inactive").val(config.mailBodyInactive);
                if (config.mailBodyEntry)
                    AJS.$("#mail-body-entry-change").val(config.mailBodyEntry);

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

                var userNameList = [];
                for (var i = 0; i < allUsers[0].length; i++) {
                    userNameList.push(allUsers[0][i]['userName']);
                }

                var categoryList = [];
                for (var i = 0; i < allCategories[0].length; i++) {
                    categoryList.push(allCategories[0][i]['categoryName']);
                }

                //build category list
                AJS.$("#categories").empty();
                for (var i = 0; i < categoryList.length; i++) {
                    AJS.$("#categories").append("<h3>" + categoryList[i] +
                        "<button class=\"aui-button aui-button-subtle\" value=\"C-" + categoryList[i] + "\">" +
                        "<span class=\"aui-icon aui-icon-small aui-iconfont-edit\">Editing</span> Edit Category Name </button></h3><fieldset>");
                    AJS.$("#categories").append("</fieldset>");
                }

                AJS.$(".coordinator").auiSelect2({
                    placeholder: "Search for user",
                    tags: userNameList.sort(),
                    tokenSeparators: [",", " "]
                });
                AJS.$(".user").auiSelect2({
                    placeholder: "Search for user",
                    tags: userNameList.sort(),
                    tokenSeparators: [",", " "]
                });
                AJS.$(".category").auiSelect2({
                    placeholder: "Search for category",
                    tags: categoryList.sort(),
                    tokenSeparators: [",", " "]
                });

                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
                AJS.messages.error({
                    title: "Error!",
                    body: "Something went wrong!"
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
        config.mailSubjectEntry = AJS.$("#mail-subject-entry-change").val();

        config.mailBodyTime = AJS.$("#mail-body-out-of-time").val();
        config.mailBodyInactive = AJS.$("#mail-body-inactive").val();
        config.mailBodyEntry = AJS.$("#mail-body-entry-change").val();

        var approvedUsers = AJS.$("#approvedUser").auiSelect2("val");
        var approvedGroups = [];

        config.approvedUsers = approvedUsers;
        config.approvedGroups = approvedGroups;
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
                    body: "Team added!"
                });
                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
                AJS.messages.error({
                    title: "Error!",
                    body: "Something went wrong!<br />" + error.responseText
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
                    body: "Category added!"
                });
                AJS.$(".loadingDiv").hide();
            },
            error: function (error) {
                AJS.messages.error({
                    title: "Error!",
                    body: "Something went wrong!<br />" + error.responseText
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

        console.log(teamName);

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
                        body: "Team renamed."
                    });
                    fetchData();
                    AJS.$(".loadingDiv").hide();
                },
                error: function (error) {
                    AJS.messages.error({
                        title: "Error!",
                        body: "Something went wrong!<br />" + error.responseText
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
                        body: "Category renamed."
                    });
                    fetchData();
                    AJS.$(".loadingDiv").hide();
                },
                error: function (error) {
                    AJS.messages.error({
                        title: "Error!",
                        body: "Something went wrong!<br />" + error.responseText
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
                    body: "Team removed!"
                });
                AJS.$(".loadingDiv").hide();
            },
            error: function () {
                AJS.messages.error({
                    title: "Error!",
                    body: "Something went wrong!"
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
                    body: "Category removed!"
                });
                AJS.$(".loadingDiv").hide();
            },
            error: function () {
                AJS.messages.error({
                    title: "Error!",
                    body: "Something went wrong!"
                });
                AJS.$(".loadingDiv").hide();
            }
        });
    }

    function modifyActivityVerificationScheduling() {
        AJS.$(".loadingDiv").show();
        AJS.$.ajax({
            url: restBaseUrl + 'scheduling/changeVerificationInterval',
            type: "PUT",
            contentType: "application/json",
            data: AJS.$("#activity-verification-job").attr("value"),
            processData: false,
            success: function () {
                AJS.messages.success({
                    title: "Success!",
                    body: "Scheduling interval changes applied!"
                });
                AJS.$(".loadingDiv").hide();
            },
            error: function () {
                AJS.messages.error({
                    title: "Error!",
                    body: "Something went wrong!"
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
        } else {
            triggerJobManually("trigger/out/of/time/notification", "Out-Of-Time-Notification");
        }
    });

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
});
