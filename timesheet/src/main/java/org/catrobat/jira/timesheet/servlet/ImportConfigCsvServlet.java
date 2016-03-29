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

package org.catrobat.jira.timesheet.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.sun.xml.internal.ws.wsdl.writer.document.Part;
import org.catrobat.jira.timesheet.activeobjects.*;
import org.catrobat.jira.timesheet.helper.CsvConfigImporter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ImportConfigCsvServlet extends HelperServlet {

    private final ConfigService configService;
    private final ActiveObjects activeObjects;

    public ImportConfigCsvServlet(UserManager userManager, LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                  GroupManager groupManager, ConfigService configService, ActiveObjects activeObjects) {
        super(userManager, loginUriProvider, webSudoManager, groupManager, configService);
        this.configService = configService;
        this.activeObjects = activeObjects;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        // Dangerous servlet - should be forbidden in production use
        if (configService.getConfiguration().getTeams().length != 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        PrintWriter writer = response.getWriter();
        writer.print("<html>" +
                "<body>" +
                "<h1>Dangerzone!</h1>" +
                "Just upload files when you know what you're doing - this upload will manipulate the database!<br />" +
                "<form action=\"config\" method=\"post\"><br />" +
                "<textarea name=\"csv\" rows=\"20\" cols=\"175\" wrap=\"off\">" +
                "# lines beginning with '#' are comments and will be ignored;;;;;;;;;;;;;;;;;;;;;;;\n" +
                "# Name;Version;Type of Device;Operating System;Producer;Article Number;Price;IMEI;Serial Number;Inventory Number;Received Date;Received From;Useful Life Of Asset;Sorted Out Comment;Sorted Out Date;Lending Begin;Lending End;Lending Purpose;Lending Comment;Lending Issuer;Lent By;Device Comment;Device Comment Author;Device Comment Date\n" +
                "Nexus 6;32 GB;Smartphone;Android Lollipop;Motorola;1337;600;123123;123123;123123;10/14/2014;Google Inc.;3 Years;Sorted Out Comment;10/14/2014;10/14/2014;10/14/2014;testing;just lending;issuer;lent by;Device Comment;comment author;10/14/2014\n" +
                "Nexus 6;32 GB;Smartphone;Android Lollipop;Motorola;1337;600;234234;234234;234234;10/14/2014;Google Inc.;3 Years;;;;;;;;;;;\n" +
                "Nexus 6;32 GB;Smartphone;Android Lollipop;Motorola;1337;600;345345;345345;345345;10/14/2014;Google Inc.;3 Years;;;10/14/2014;;testing 3;just lending 3;issuer 3;lent by 3;Device Comment 3;comment author 3;10/14/2014\n" +
                "</textarea><br />\n" +
                "<input type=\"checkbox\" name=\"drop\" value=\"drop\">Drop existing table entries<br /><br />\n" +
                "<input type=\"submit\" />" +
                "</form>" +
                "</body>" +
                "</html>");
        writer.flush();
        writer.close();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);

        // Dangerous servlet - should be forbidden in production use
        if (configService.getConfiguration().getTeams().length != 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String csvString = request.getParameter("csv");

        if (request.getParameter("drop") != null && request.getParameter("drop").equals("drop")) {
            dropEntries();
        }

        CsvConfigImporter csvImporter = new CsvConfigImporter(configService);
        String errorString = csvImporter.importCsv(csvString);

        response.getWriter().print("Successfully executed following string:<br />" +
                "<textarea rows=\"20\" cols=\"200\" wrap=\"off\" disabled>" + csvString + "</textarea>" +
                "<br /><br />" +
                "Following errors occurred:<br />" + errorString);
    }

    private void dropEntries() {
        activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                for (Config config : activeObjects.find(Config.class)) {
                    activeObjects.delete(config);
                }
                for (ApprovedUser approvedUser : activeObjects.find(ApprovedUser.class)) {
                    activeObjects.delete(approvedUser);
                }
                for (ApprovedGroup approvedGroup : activeObjects.find(ApprovedGroup.class)) {
                    activeObjects.delete(approvedGroup);
                }
                for (Category category : activeObjects.find(Category.class)) {
                    activeObjects.delete(category);
                }
                for (CategoryToTeam categoryToTeam : activeObjects.find(CategoryToTeam.class)) {
                    activeObjects.delete(categoryToTeam);
                }
                for (Group group : activeObjects.find(Group.class)) {
                    activeObjects.delete(group);
                }
                for (TeamToGroup teamToGroup : activeObjects.find(TeamToGroup.class)) {
                    activeObjects.delete(teamToGroup);
                }
                for (Team team : activeObjects.find(Team.class)) {
                    activeObjects.delete(team);
                }
                return null;
            }
        });
    }
}