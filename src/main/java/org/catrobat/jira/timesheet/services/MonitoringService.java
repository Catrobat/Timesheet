package org.catrobat.jira.timesheet.services;

import com.atlassian.activeobjects.tx.Transactional;
import org.catrobat.jira.timesheet.activeobjects.Monitoring;

@Transactional
public interface MonitoringService {

    Monitoring getMonitoring();

    void setMonitoring(int period, int requiredHours, int exceptions);

}
