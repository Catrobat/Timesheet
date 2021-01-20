package org.catrobat.jira.timesheet.servlet;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.websudo.WebSudoManager;
import org.apache.poi.ss.usermodel.Workbook;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.services.ConfigService;
import org.catrobat.jira.timesheet.services.PermissionService;
import org.catrobat.jira.timesheet.services.TimesheetService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.catrobat.jira.timesheet.services.XlsxExportService;

public class ExportAllTimesheetsAsXlsxServlet extends HighPrivilegeServlet {

    public static final String CONTENT_TYPE = "application/vnd.ms-excel";
    private final TimesheetService timesheetService;
    private final XlsxExportService exportService;

    public ExportAllTimesheetsAsXlsxServlet(LoginUriProvider loginUriProvider, WebSudoManager webSudoManager,
                                            TimesheetService timesheetService,
                                            XlsxExportService exportService,
                                            ConfigService configService, PermissionService permissionService) {
        super(loginUriProvider, webSudoManager, permissionService, configService);
        this.timesheetService = timesheetService;
        this.exportService = exportService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.doGet(request, response);

        if (response.getStatus() == HttpServletResponse.SC_MOVED_TEMPORARILY) {
            return;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-dd-MM");
        String date = simpleDateFormat.format(new Date());

        String filename = "attachment; filename=\"Timesheets-" + date + ".xlsx\"";

        response.setContentType(CONTENT_TYPE);
        response.setHeader("Content-Disposition", filename);

        List<Timesheet> timesheetList = timesheetService.all();

        Workbook workbook = exportService.exportTimesheets(timesheetList);
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}