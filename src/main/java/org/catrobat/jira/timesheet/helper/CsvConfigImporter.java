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

package org.catrobat.jira.timesheet.helper;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.CategoryService;
import org.catrobat.jira.timesheet.services.TeamService;

import java.util.LinkedList;
import java.util.List;

public class CsvConfigImporter {

    private final ConfigService configService;
    private final CategoryService categoryService;
    private final TeamService teamService;

    public CsvConfigImporter(ConfigService configService, CategoryService categoryService, TeamService teamService) {

        this.configService = configService;
        this.categoryService = categoryService;
        this.teamService = teamService;
    }

    public String importCsv(String csvString) throws ServiceException {
        StringBuilder errorStringBuilder = new StringBuilder("<ul>");
        int lineNumber = 0;
        List<String> assignedCoordinators = new LinkedList<String>();
        List<String> assignedUsers = new LinkedList<String>();
        List<String> assignedCategories = new LinkedList<String>();
        List<String> addedCategories = new LinkedList<String>();
        String readOnlyUsers = "";
        //create new Config
        Config config = configService.getConfiguration();

        for (String line : csvString.split("\\r?\\n")) {
            lineNumber++;

            if (line.length() == 0) {
                continue;
            }
            // skip line if comment
            if (line.charAt(0) == '#') {
                errorStringBuilder.append("<li>comment on line ")
                        .append(lineNumber)
                        .append(" (line will be ignored)</li>");
                continue;
            }

            //String[] columns = line.split(CsvConstants.DELIMITER, 24);
            String[] columns = line.split(CsvConstants.DELIMITER);

            if (columns.length < 2) {
                errorStringBuilder.append("<li>field has no value (line ")
                        .append(lineNumber)
                        .append(": \"")
                        .append(line)
                        .append("\" will be ignored)</li>");
            } else if (columns[0].equals("Supervisors")) {
                for (int i = 1; i < columns.length; i++) {
                    readOnlyUsers += columns[i] + ",";
                }
                config.setReadOnlyUsers(readOnlyUsers.substring(0, readOnlyUsers.length() - 1));
            } else if (columns[0].equals("Timesheet Admins and Groups") && columns.length > 1) {
                for (int i = 1; i < columns.length; i++) {
                    ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(columns[i]);
                    if (!user.getName().isEmpty()) {
                        configService.addTimesheetAdmin(user);
                    }
                }
            } else if (columns[0].equals("Email From Name")) {
                config.setMailFromName(columns[1]);
            } else if (columns[0].equals("Email From Mail-Address")) {
                config.setMailFrom(columns[1]);
            } else if (columns[0].equals("Email Out Of Time Subject")) {
                config.setMailSubjectTime(columns[1]);
            } else if (columns[0].equals("Email Out Of Time Body")) {
                config.setMailBodyTime(columns[1]);
            } else if (columns[0].equals("Email Inactive Subject")) {
                config.setMailSubjectInactiveState(columns[1]);
            } else if (columns[0].equals("Email Inactive Body")) {
                config.setMailBodyInactiveState(columns[1]);
            } else if (columns[0].equals("Email Offline Subject")) {
                config.setMailSubjectOfflineState(columns[1]);
            } else if (columns[0].equals("Email Offline Body")) {
                config.setMailBodyOfflineState(columns[1]);
            } else if (columns[0].equals("Email Active Subject")) {
                config.setMailSubjectActiveState(columns[1]);
            } else if (columns[0].equals("Email Active Body")) {
                config.setMailBodyActiveState(columns[1]);
            } else if (columns[0].equals("Email Admin Changed Entry Subject")) {
                config.setMailSubjectEntry(columns[1]);
            } else if (columns[0].equals("Email Admin Changed Entry Body")) {
                config.setMailBodyEntry(columns[1]);
            }
            //Team Data
            else if (columns[0].equals("Assigned Coordinators")) {
                for (int i = 1; i < columns.length; i++) {
                    assignedCoordinators.add(columns[i]);
                }
            } else if (columns[0].equals("Assigned Users")) {
                for (int i = 1; i < columns.length; i++) {
                    assignedUsers.add(columns[i]);
                }
            } else if (columns[0].equals("Assigned Categories")) {
                for (int i = 1; i < columns.length; i++) {
                    assignedCategories.add(columns[i]);
                    if (!addedCategories.contains(columns[i])) {
                        addedCategories.add(columns[i]);
                        categoryService.add(columns[i]);
                    } else {
                        errorStringBuilder.append("<li>duplicated category name detected (line ")
                                .append(lineNumber)
                                .append(" will be ignored)</li>");
                    }
                }
            } else if (columns[0].equals("Team Name")) {
                configService.addTeam(columns[1], assignedCoordinators, assignedUsers, assignedCategories);

                //clear temp arrays after team was created
                assignedCoordinators.clear();
                assignedUsers.clear();
                assignedCategories.clear();
            } else {
                errorStringBuilder.append("<li>cannot add config data (line ")
                        .append(lineNumber)
                        .append(": \"")
                        .append(line)
                        .append("\" will be ignored)</li>");
            }
        }

        addedCategories.clear();
        config.save();
        errorStringBuilder.append("</ul>");

        return errorStringBuilder.toString();
    }
}
