package org.catrobat.jira.timesheet.services;

import com.atlassian.activeobjects.tx.Transactional;

import java.util.ArrayList;
import java.util.Map;

@Transactional
public interface AllowedModUsersService {

    boolean checkIfUserIsInList(String user_key);

    void update(ArrayList<String> user_keys);

    ArrayList<Map<String, String>> getAllowedModUsers();
}
