package org.catrobat.jira.timesheet.activeobjects;

import net.java.ao.Entity;

public interface AllowedModUsers extends Entity {
    String getUserKey();
    void setUserKey(String user_key);
}
