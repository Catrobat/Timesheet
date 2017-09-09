package org.catrobat.jira.timesheet.activeobjects;

import net.java.ao.Entity;

public interface Scheduling extends Entity {

    int getInactiveTime();
    void setInactiveTime(int inactiveTime);

    int getOfflineTime();
    void setOfflineTime(int offlineTime);

    int getRemainingTime();
    void setRemainingTime(int remainingTime);

    int getOutOfTime();
    void setOutOfTime(int outOfTime);

    int getMaxModificationdDays();
    void setMaxModificationdDays();
}
