package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.catrobat.jira.timesheet.activeobjects.Scheduling;
import org.catrobat.jira.timesheet.services.SchedulingService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Component
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
    public void setScheduling(int inactiveTime, int offlineTime, int remainingTime, int outOfTime) {
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        if (inactiveTime <= 0 || inactiveTime >= 999) {
            inactiveTime = 4; // ~ 1 month
        }

        if (offlineTime <= 0 || offlineTime >= 999) {
            offlineTime = 8; // ~ 2 months
        }

        if (remainingTime <= 2 || remainingTime >= 999) {
            remainingTime = 1; // one week
        }

        if (outOfTime <= 0 || outOfTime >= 999) {
            outOfTime = 80;
        }

        scheduling[0].setInactiveTime(inactiveTime);
        scheduling[0].setOfflineTime(offlineTime);
        scheduling[0].setRemainingTime(remainingTime);
        scheduling[0].setOutOfTime(outOfTime);
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

        int inactiveTimeWeeks = scheduling[0].getInactiveTime();

        return isDateOlderThanXWeeks(date, inactiveTimeWeeks);
    }

    // default value two months
    @Override
    public boolean isOlderThanOfflineTime(Date date) {
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        int offlineTimeWeeks = scheduling[0].getOfflineTime();

        return isDateOlderThanXWeeks(date, offlineTimeWeeks);
    }

    // default value one week, minimum value 3 days
    @Override
    public boolean isOlderThanRemainingTime(Date date) {
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        int remainingTimeWeeks = scheduling[0].getRemainingTime();

        return isDateOlderThanXWeeks(date, remainingTimeWeeks);
    }

    @Override
    public boolean isDateOlderThanXWeeks(Date date, int weeks) {
        Instant instant = date.toInstant();
        ZonedDateTime dataTime = instant.atZone(ZoneId.systemDefault());
        ZonedDateTime xWeeksAgo = ZonedDateTime.now().minusWeeks(weeks);

        return dataTime.isBefore(xWeeksAgo);
    }

    @Override
    public int getMaxModificationDays(){
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }
        if(scheduling[0].getMaxModificationdDays() == 0){
            return 30;
        }
        return scheduling[0].getMaxModificationdDays();
    }

    @Override
    public void setMaxModificationDays(int days){
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        scheduling[0].setMaxModificationdDays(days);
        scheduling[0].save();
    }
}
