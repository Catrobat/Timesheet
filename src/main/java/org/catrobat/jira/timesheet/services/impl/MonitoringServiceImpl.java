package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.catrobat.jira.timesheet.activeobjects.Monitoring;
import org.catrobat.jira.timesheet.services.MonitoringService;
import org.springframework.stereotype.Component;

@Component
public class MonitoringServiceImpl implements MonitoringService {

    private final ActiveObjects ao;

    public MonitoringServiceImpl(ActiveObjects ao) {
        this.ao = ao;
    }

    @Override
    public Monitoring getMonitoring() {
        Monitoring[] monitoring = ao.find(Monitoring.class);

        if (monitoring.length == 0) {
            ao.create(Monitoring.class).save();
            monitoring = ao.find(Monitoring.class);
        }

        return monitoring[0];
    }

    @Override
    public void setMonitoring(int period, int requiredHours, int exceptions) {
        Monitoring[] monitoring = ao.find(Monitoring.class);

        if (monitoring.length == 0) {
            ao.create(Monitoring.class).save();
            monitoring = ao.find(Monitoring.class);
        }

        if (period <= 0 || period >= 999) {
            period = 6; // half a year
        }

        if (requiredHours <= 0 || requiredHours >= 999) {
            requiredHours = 100;
        }

        if (exceptions <= 2 || exceptions >= 999) {
            exceptions = 1; // one exception
        }

        monitoring[0].setPeriod(period);
        monitoring[0].setRequiredHours(requiredHours);
        monitoring[0].setExceptions(exceptions);
        monitoring[0].save();
    }
}
