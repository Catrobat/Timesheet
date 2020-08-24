package org.catrobat.jira.timesheet.services;

import com.atlassian.activeobjects.tx.Transactional;
import javafx.util.Pair;
import org.catrobat.jira.timesheet.activeobjects.Monitoring;

import java.time.LocalDate;

@Transactional
public interface MonitoringService {

    Monitoring getMonitoring();

    void setMonitoring(int period, int requiredHours, int exceptions);

    Pair<LocalDate, LocalDate> getLastInterval();

}
