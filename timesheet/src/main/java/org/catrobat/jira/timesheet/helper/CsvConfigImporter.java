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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;

public class CsvConfigImporter {

    private final ConfigService configService;

    public CsvConfigImporter(ConfigService configService) {

        this.configService = configService;
    }

    public String importCsv(String csvString) {
        Map<String, Config> configMap = new TreeMap<String, Config>();
        DateFormat dateFormat = new SimpleDateFormat("M/d/y");
        StringBuilder errorStringBuilder = new StringBuilder("<ul>");
        int lineNumber = 0;
        for (String line : csvString.split("\\r?\\n")) {
            lineNumber++;

            // skip line if empty or comment
            if (line.length() == 0 || line.charAt(0) == '#') {
                errorStringBuilder.append("<li>comment on line ")
                        .append(lineNumber)
                        .append(" (line will be ignored)</li>");
                continue;
            }

            String[] columns = line.split(CsvTimesheetExporter.DELIMITER, 24);
            if (columns.length < 24) {
                errorStringBuilder.append("<li>too less columns on line ")
                        .append(lineNumber)
                        .append(" (line will be ignored)</li>");
                continue;
            }

            // Config
            /*
            String name = columns[0];
            String version = columns[1];
            HardwareModel hardwareModel = hardwareModelMap.get(name + ":" + version);
            if (hardwareModel == null) {
                String typeOfDeviceName = columns[2];
                String operationSystem = columns[3];
                String producer = columns[4];
                String articleNumber = columns[5];
                String price = columns[6];
                hardwareModel = hardwareModelService.add(name, typeOfDeviceName, version, price, producer, operationSystem, articleNumber);
                if (hardwareModel == null) {
                    for (HardwareModel tempModel : hardwareModelService.all()) {
                        if (tempModel.getName().toLowerCase().equals(name.toLowerCase()) &&
                                tempModel.getVersion().toLowerCase().equals(version.toLowerCase())) {
                            hardwareModel = tempModel;
                            break;
                        }
                    }
                }
                hardwareModelMap.put(name + ":" + version, hardwareModel);
            }


            if (hardwareModel == null) {
                errorStringBuilder.append("<li>cannot add hardware model (line ")
                        .append(lineNumber)
                        .append(" will be ignored)</li>");
            }

            // device
            String imei = columns[7];
            String serialNumber = columns[8];
            String inventoryNumber = columns[9];
            Date receivedDate = null;
            try {
                if (columns[10].length() != 0) {
                    receivedDate = dateFormat.parse(columns[10]);
                }
            } catch (ParseException e) {
                errorStringBuilder.append("<li>received date on line ")
                        .append(lineNumber)
                        .append(" has wrong format (date set to null)</li>");
            }
            String receivedFrom = columns[11];
            String usefulLifeOfAsset = columns[12];
            String sortedOutComment = columns[13];
            Date sortedOutDate = null;
            try {
                if (columns[14].length() != 0) {
                    sortedOutDate = dateFormat.parse(columns[14]);
                }
            } catch (ParseException e) {
                errorStringBuilder.append("<li>sorted out date on line ")
                        .append(lineNumber)
                        .append(" has wrong format (date set to null)</li>");
            }

            Device device = deviceService.add(hardwareModel, imei, serialNumber, inventoryNumber, receivedFrom, receivedDate, usefulLifeOfAsset);
            System.out.println("Add device: " + hardwareModel + ", " + imei + ", " + serialNumber + ", " + inventoryNumber);
            if (device == null) {
                System.out.println("failed: " + imei + ", " + serialNumber + ", " + inventoryNumber);
                errorStringBuilder.append("<li style=\"color:red;\">device couldn't be added (maybe already existing, line ")
                        .append(lineNumber)
                        .append(")</li>");
                continue;
            }

            deviceService.sortOutDevice(device.getID(), sortedOutDate, sortedOutComment);

            // lending
            String purpose = columns[17];
            String lendingComment = columns[18];
            String lendingIssuerUser = columns[19];
            String lendingByUser = columns[20];
            if (lendingIssuerUser.trim().length() != 0 && lendingByUser.trim().length() != 0) {
                Date lendingBegin = null;
                try {
                    if (columns[15].length() != 0) {
                        lendingBegin = dateFormat.parse(columns[15]);
                    }
                } catch (ParseException e) {
                    errorStringBuilder.append("<li>lending begin date on line ")
                            .append(lineNumber)
                            .append(" has wrong format (date set to null)</li>");
                }
                Date lendingEnd = null;
                try {
                    if (columns[16].length() != 0) {
                        lendingEnd = dateFormat.parse(columns[16]);
                    }
                } catch (ParseException e) {
                    errorStringBuilder.append("<li>lending end date on line ")
                            .append(lineNumber)
                            .append(" has wrong format (date set to null)</li>");
                }
                Lending lending = lendingService.lendOut(device, lendingByUser, lendingIssuerUser, purpose, lendingComment, lendingBegin);
                if (lending == null) {
                    errorStringBuilder.append("<li style=\"color:red;\">lending couldn't be added (line ")
                            .append(lineNumber)
                            .append(")</li>");
                }
                if (lendingEnd != null) {
                    lendingService.bringBack(lending, purpose, lendingComment, lendingEnd);
                }
            } else if (lendingIssuerUser.trim().length() != 0 || lendingByUser.trim().length() != 0) {
                errorStringBuilder.append("<li style=\"color:red;\">lending couldn't be added - issuer or user empty (line ")
                        .append(lineNumber)
                        .append(")</li>");
            }

            // comment
            String deviceComment = columns[21];
            String author = columns[22];
            if (deviceComment.trim().length() != 0 && author.trim().length() != 0) {
                Date commentDate = null;
                try {
                    if (columns[23].length() != 0) {
                        commentDate = dateFormat.parse(columns[23]);
                    }
                } catch (ParseException e) {
                    errorStringBuilder.append("<li>device comment date on line ")
                            .append(lineNumber)
                            .append(" has wrong format (date set to null)</li>");
                }
                DeviceComment addedDeviceComment = deviceCommentService.addDeviceComment(device, author, deviceComment, commentDate);
                if (addedDeviceComment == null) {
                    errorStringBuilder.append("<li style=\"color:red;\">device comment couldn't be added (line ")
                            .append(lineNumber)
                            .append(")</li>");
                }
            } else if (deviceComment.trim().length() != 0 || author.trim().length() != 0) {
                errorStringBuilder.append("<li style=\"color:red;\">device comment couldn't be added - author or comment empty (line ")
                        .append(lineNumber)
                        .append(")</li>");
            }
            */
        }

        errorStringBuilder.append("</ul>");

        return errorStringBuilder.toString();
    }
}
