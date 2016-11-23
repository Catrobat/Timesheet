package org.catrobat.jira.timesheet.activeobjects;

import net.java.ao.Entity;
public interface Scheduling extends Entity {

    int getInactiveTime();

    void setInactiveTime(int inactiveTime);
}
