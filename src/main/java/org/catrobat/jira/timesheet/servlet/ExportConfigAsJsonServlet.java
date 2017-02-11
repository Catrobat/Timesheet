package org.catrobat.jira.timesheet.servlet;


import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.google.gson.Gson;
import org.catrobat.jira.timesheet.rest.json.JsonConfig;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.time.ZonedDateTime;

public class ExportConfigAsJsonServlet extends HighPrivilegeServlet {

    private final ConfigService configService;

    public ExportConfigAsJsonServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                     ConfigService configService, PermissionService permissionService) {
        super(loginUriProvider, webSudoManager, permissionService, configService);
        this.configService = configService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        ZonedDateTime today = ZonedDateTime.now();
        String dateString = today.getYear() + "-" + today.getMonth() + "-" + today.getDayOfMonth();
        System.out.println(today.toString());
        String filename = "attachment; filename=\"" +
                dateString + "_Timesheet_Config.json\"";

        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", filename);

        JsonConfig jsonConfig = new JsonConfig(configService);

        Gson gson = new Gson();
        String jsonString = gson.toJson(jsonConfig);

        PrintStream printStream = new PrintStream(response.getOutputStream(), false, "UTF-8");
        printStream.print(jsonString);
        printStream.flush();
        printStream.close();
    }
}
