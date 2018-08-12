package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import net.java.ao.Query;
import org.catrobat.jira.timesheet.activeobjects.AllowedModUsers;
import org.catrobat.jira.timesheet.services.AllowedModUsersService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class AllowedModUsersServiceImpl implements AllowedModUsersService {

    private final ActiveObjects ao;
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(AllowedModUsersServiceImpl.class);

    public AllowedModUsersServiceImpl(ActiveObjects ao){this.ao = ao;}

    @Override
    public boolean checkIfUserIsInList(String user_key){
        LOGGER.error("checking if user with key: " + user_key + " exists");

        return ao.find(AllowedModUsers.class, Query.select().where("USER_KEY = ?", user_key)).length == 1;
    }

    @Override
    public void update(ArrayList<String> user_keys){
        //first reseting DB and replace them with given entries
        LOGGER.error("reseting DB");
        for(AllowedModUsers modUsers : ao.find(AllowedModUsers.class)){
            ao.delete(modUsers);
        }
        LOGGER.error("processing data: " + user_keys);
        for(String user_key : user_keys){
            if(ComponentAccessor.getUserManager().getUserByKey(user_key) != null) {
                AllowedModUsers current_entry = ao.create(AllowedModUsers.class);
                current_entry.setUserKey(user_key);
                current_entry.save();
            }else{
                LOGGER.error("User with key " + user_key + " does not exist, continue");
            }
        }
    }

    @Override
    public ArrayList<Map<String, String>> getAllowedModUsers(){
        AllowedModUsers[] users = ao.find(AllowedModUsers.class);
        ArrayList<Map<String, String>> result = new ArrayList<>();

        for(AllowedModUsers user : users){
            Map<String, String> current_user_object = new HashMap<>();
            current_user_object.put("userKey" ,  user.getUserKey());
            current_user_object.put("displayName", ComponentAccessor.getUserManager().getUserByKey(user.getUserKey()).getDisplayName());

            result.add(current_user_object);
        }

        return result;
    }
}
