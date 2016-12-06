package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.catrobat.jira.timesheet.activeobjects.Scheduling;
import org.catrobat.jira.timesheet.services.SchedulingService;
import org.joda.time.DateTime;

import java.util.Date;

public class SchedulingServiceImpl implements SchedulingService {

    private final ActiveObjects ao;

    public SchedulingServiceImpl(ActiveObjects ao) {
        this.ao = ao;
    }

    @Override
    public Scheduling getScheduling() {
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        return scheduling[0];
    }

    @Override
    public void setScheduling(int inactiveTime, int offlineTime, int remainingTime) {
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        if (inactiveTime <= 0 || inactiveTime >= 999) {
            inactiveTime = 31; // 1 month
        }

        if (offlineTime <= 0 || offlineTime >= 999) {
            offlineTime = 61; // 2 months
        }

        if (remainingTime <= 2 || remainingTime >= 999) {
            remainingTime = 7; // one week
        }

        scheduling[0].setInactiveTime(inactiveTime);
        scheduling[0].setOfflineTime(offlineTime);
        scheduling[0].setRemainingTime(remainingTime);
        scheduling[0].save();
    }

    // default value one month
    @Override
    public boolean isOlderThanInactiveTime(Date date) {

        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        int inactiveTimeDays = scheduling[0].getInactiveTime();

        return isDateOlderThanXDays(date, inactiveTimeDays);
    }

    // default value two months
    @Override
    public boolean isOlderThanOfflineTime(Date date) {
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        int offlineTimeDays = scheduling[0].getOfflineTime();

        return isDateOlderThanXDays(date, offlineTimeDays);
    }

    // default value one week, minimum value 3 days
    @Override
    public boolean isOlderThanRemainingTime(Date date) {
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        int remainingTimeDays = scheduling[0].getRemainingTime();

        return isDateOlderThanXDays(date, remainingTimeDays);
    }

    @Override
    public boolean isDateOlderThanXDays(Date date, int days) {
        DateTime xDaysAgo = new DateTime().minusDays(days);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(xDaysAgo) < 0);
    }
}
