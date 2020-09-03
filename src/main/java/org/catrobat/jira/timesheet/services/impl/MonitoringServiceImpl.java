package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.catrobat.jira.timesheet.activeobjects.Monitoring;
import org.catrobat.jira.timesheet.services.MonitoringService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Map;

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

        if (period <= 0) {
            period = 1; // half a year
        }

        if (requiredHours <= 0) {
            requiredHours = 1;
        }

        if (exceptions <= 0) {
            exceptions = 1; // one exception
        }

        monitoring[0].setPeriod(period);
        monitoring[0].setRequiredHours(requiredHours);
        monitoring[0].setExceptions(exceptions);
        monitoring[0].save();
    }

    @Override
    public Map.Entry<LocalDate, LocalDate> getLastInterval() {
        LocalDate begin = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();
        if (getMonitoring().getPeriod() > 1) {
            begin = begin.minusMonths(getMonitoring().getPeriod() - 1);
        }
        return new AbstractMap.SimpleEntry<>(begin, end);
    }

    @Override
    public String getLastIntervalFormattedAsString() {
        DateTimeFormatter formatter;
        Map.Entry<LocalDate, LocalDate> interval = getLastInterval();
        if (interval.getKey().getYear() != interval.getValue().getYear()) {
            formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        }  else if(interval.getKey().getMonth() != interval.getValue().getMonth()){
            formatter = DateTimeFormatter.ofPattern("dd.MM.");
        }
        else {
            formatter = DateTimeFormatter.ofPattern("dd.");
        }

        return interval.getKey().format(formatter) + "-" + interval.getValue().format(DateTimeFormatter.ofPattern("dd.MM.yy"));
    }
}
