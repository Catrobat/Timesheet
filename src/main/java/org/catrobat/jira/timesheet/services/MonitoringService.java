package org.catrobat.jira.timesheet.services;

import com.atlassian.activeobjects.tx.Transactional;
import org.catrobat.jira.timesheet.activeobjects.Monitoring;

import java.time.LocalDate;
import java.util.Map;

@Transactional
public interface MonitoringService {

    Monitoring getMonitoring();

    void setMonitoring(int period, int requiredHours, int exceptions);

    Map.Entry<LocalDate, LocalDate> getCurrentInterval();

    Map.Entry<LocalDate, LocalDate> getLastInterval();

    String formatIntervalToString(Map.Entry<LocalDate, LocalDate> interval);

}
