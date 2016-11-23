package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.catrobat.jira.timesheet.activeobjects.Scheduling;
import org.catrobat.jira.timesheet.services.SchedulingService;
import org.joda.time.DateTime;

import java.util.Date;

public class SchedulingServiceImpl implements SchedulingService{

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
    public void setScheduling(int inactiveTime) {
        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        scheduling[0].setInactiveTime(inactiveTime);
        scheduling[0].save();
    }

    public boolean isOlderThanInactiveTime(Date date) {

        Scheduling[] scheduling = ao.find(Scheduling.class);

        if (scheduling.length == 0) {
            ao.create(Scheduling.class).save();
            scheduling = ao.find(Scheduling.class);
        }

        int inactiveTimeDays = scheduling[0].getInactiveTime();

        DateTime twoWeeksAgo = new DateTime().minusDays(inactiveTimeDays);
        DateTime datetime = new DateTime(date);
        return (datetime.compareTo(twoWeeksAgo) < 0);
    }
}
