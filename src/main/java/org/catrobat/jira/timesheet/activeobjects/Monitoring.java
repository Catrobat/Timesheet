package org.catrobat.jira.timesheet.activeobjects;

import net.java.ao.Entity;

public interface Monitoring extends Entity {

    int getPeriod();
    void setPeriod(int period);

    int getRequiredHours();
    void setRequiredHours(int requiredHours);

    int getExceptions();
    void setExceptions(int exceptions);

}
