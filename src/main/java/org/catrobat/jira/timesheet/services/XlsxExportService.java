package org.catrobat.jira.timesheet.services;

import org.apache.poi.ss.usermodel.Workbook;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;

import java.util.List;

public interface XlsxExportService {

    Workbook exportTimesheets(List<Timesheet> timesheetList);
}
