package org.catrobat.jira.timesheet.services;

import com.atlassian.activeobjects.tx.Transactional;
import org.catrobat.jira.timesheet.activeobjects.Scheduling;

import java.util.Date;

@Transactional
public interface SchedulingService {

    Scheduling getScheduling();

    void setScheduling(int inactiveTime, int offlineTime, int remainingTime, int outOfTime);

    boolean isOlderThanInactiveTime(Date date);

    boolean isOlderThanOfflineTime(Date date);

    boolean isDateOlderThanXWeeks(Date date, int weeks);

    boolean isOlderThanRemainingTime(Date date);
}
