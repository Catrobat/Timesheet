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

import org.catrobat.jira.timesheet.activeobjects.Config;
import org.catrobat.jira.timesheet.activeobjects.ConfigService;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CsvConfigImporter {

    private final ConfigService configService;

    public CsvConfigImporter(ConfigService configService) {

        this.configService = configService;
    }

    public String importCsv(String csvString) {
        StringBuilder errorStringBuilder = new StringBuilder("<ul>");
        int lineNumber = 0;
        List<String> assignedCoordinators = new LinkedList<String>();
        List<String> assignedUsers = new LinkedList<String>();
        List<String> assignedCategories = new LinkedList<String>();
        //create new Config
        Config config = configService.getConfiguration();

        for (String line : csvString.split("\\r?\\n")) {
            lineNumber++;

            // skip line if empty or comment
            if (line.length() == 0 || line.charAt(0) == '#') {
                errorStringBuilder.append("<li>comment on line ")
                        .append(lineNumber)
                        .append(" (line will be ignored)</li>");
                continue;
            }

            //String[] columns = line.split(CsvExporter.DELIMITER, 24);
            String[] columns = line.split(CsvExporter.DELIMITER);

            /*
            if (columns[0].equals("Approved Users") && columns.length > 0) {
                for (int i = 1; i < columns.length; i++) {
                    configService.addApprovedUser(columns[i], columns[i]);
                }
            } else if (columns[0].equals("Approved Groups") && columns.length > 0) {
                for (int i = 1; i < columns.length; i++) {
                    configService.addApprovedGroup(columns[i]);
                }
            }
            */
            for(int i = 0; i < columns.length; i++)
                System.out.println(columns[i]);

            if (columns[0].equals("Allowed Users and Groups") && columns.length > 0) {
                for (int i = 1; i < columns.length; i++) {
                    configService.addApprovedUser(columns[i], columns[i]);
                }
            } else if (columns[0].equals("Email From Name") && columns.length > 0) {
                config.setMailFromName(columns[1]);
            } else if (columns[0].equals("Email From Mail-Address") && columns.length > 0) {
                config.setMailFrom(columns[1]);
            } else if (columns[0].equals("Email Out Of Time Subject") && columns.length > 0) {
                config.setMailSubjectTime(columns[1]);
            } else if (columns[0].equals("Email Out Of Time Body") && columns.length > 0) {
                config.setMailBodyTime(columns[1]);
            } else if (columns[0].equals("Email Inactive Subject") && columns.length > 0) {
                config.setMailSubjectInactive(columns[1]);
            } else if (columns[0].equals("Email Inactive Body") && columns.length > 0) {
                config.setMailBodyInactive(columns[1]);
            } else if (columns[0].equals("Email Admin Changed Entry Subject") && columns.length > 0) {
                config.setMailSubjectEntry(columns[1]);
            } else if (columns[0].equals("Email Admin Changed Entry Body") && columns.length > 0) {
                config.setMailBodyEntry(columns[1]);
            }

            //Team Data
            if (columns[0].equals("Assigned Coordinators") && columns.length > 0) {
                for(int i = 1; i < columns.length; i++) {
                    assignedCoordinators.add(columns[i]);
                }
            } else if (columns[0].equals("Assigned Users") && columns.length > 0) {
                for(int i = 1; i < columns.length; i++) {
                    assignedUsers.add(columns[i]);
                }
            } else if (columns[0].equals("Assigned Categories") && columns.length > 0) {
                for(int i = 1; i < columns.length; i++) {
                    assignedCategories.add(columns[i]);
                }
            } else if (columns[0].equals("Team Name") && columns.length > 0) {
                configService.editTeam(columns[1], assignedCoordinators, assignedUsers, assignedCategories);
            }

            assignedCoordinators.clear();
            assignedUsers.clear();
            assignedCategories.clear();
            config.save();
        }
        config.save();

        errorStringBuilder.append("</ul>");

        return errorStringBuilder.toString();
    }
}
