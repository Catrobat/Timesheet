package org.catrobat.jira.timesheet.scheduling;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.service.ServiceException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.scheduling.PluginJob;

import org.catrobat.jira.timesheet.activeobjects.Team;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.*;

import java.util.*;


public class ActivityVerificationJob implements PluginJob {

    private TimesheetService sheetService;
    private TimesheetEntryService entryService;
    private CategoryService categoryService;
    private SchedulingService schedulingService;
    private PermissionService permissionService;
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.LogManager.getLogger(ActivityVerificationJob.class);

    @Override
    public void execute(Map<String, Object> map) {
        LOGGER.error("ActivityVerificationJob triggered at: {}" + (new Date()).toString());

        Date today = new Date();

        sheetService = (TimesheetService) map.get("sheetService");
        entryService = (TimesheetEntryService) map.get("entryService");
        categoryService = (CategoryService) map.get("categoryService");
        schedulingService = (SchedulingService) map.get("schedulingService");
        permissionService = (PermissionService) map.get("permissionService");

        List<Timesheet> timesheetList = sheetService.all();
        for (Timesheet timesheet : timesheetList) {
        	
        	String userKey = timesheet.getUserKey();
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(userKey);
            String statusFlagMessage = "nothing changed";
            
            TimesheetEntry[] entries = entryService.getEntriesBySheet(timesheet);
            if (entries.length == 0) {
                continue;
            }
            
            if (permissionService == null) {
            	LOGGER.error("ATTENTION! permissionService is null...! For: " + "USER: " + user.getDisplayName() + " / STATE: " + timesheet.getState());
                LOGGER.error("equals Timesheet.State.DONE: " + timesheet.getState().equals(Timesheet.State.DONE));
            }
            else if (permissionService == null && !timesheet.getState().equals(Timesheet.State.DONE)) {
            	LOGGER.error("---DONE DONE DONE ATTENTION! permissionService is null...! For: " + "USER: " + user.getDisplayName() + " / STATE: " + timesheet.getState());
                LOGGER.error("equals Timesheet.State.DONE: " + timesheet.getState().equals(Timesheet.State.DONE));
            }
            else {
            	LOGGER.debug("isUserInUserGroupDisabled(user): " + permissionService.isUserInUserGroupDisabled(user));
                if (!timesheet.getState().equals(Timesheet.State.DONE) && permissionService.isUserInUserGroupDisabled(user)) {
                	LOGGER.info("setting Timesheet to DONE for: " + user.getDisplayName());
                	timesheet.setState(Timesheet.State.DONE);
                	timesheet.save();
                	statusFlagMessage = "Timesheet set to DONE";
                }
                
                if (timesheet.getState().equals(Timesheet.State.DONE) && !permissionService.isUserInUserGroupDisabled(user)) {
                	LOGGER.info("setting Timesheet to ACTIVE from DONE for: " + user.getDisplayName());
                	timesheet.setState(Timesheet.State.ACTIVE);
                	timesheet.save();
                	statusFlagMessage = "Timesheet set to ACTIVE";
                }
            }
            
            Date latestEntryDate = timesheet.getLatestEntryBeginDate();
            TimesheetEntry latestInactiveEntry = entryService.getLatestInactiveEntry(timesheet);
            Timesheet.State state = timesheet.getState();
            if (state == Timesheet.State.ACTIVE && schedulingService.isOlderThanInactiveTime(latestEntryDate)) {
                setAutoInactive(timesheet);
                statusFlagMessage = "user is active, but latest entry is older than the specified inactive limit";
            }
            else if (state == Timesheet.State.AUTO_INACTIVE &&
                    schedulingService.isOlderThanOfflineTime(latestEntryDate)) {
                timesheet.setState(Timesheet.State.DISABLED);
                timesheet.save();
                statusFlagMessage = "user is still inactive since the specified disabled limit";
            }
            else if (isManualInactiveState(state) && latestInactiveEntry != null &&
                    latestInactiveEntry.getInactiveEndDate().compareTo(today) < 0) {
                timesheet.setState(Timesheet.State.ACTIVE);
                timesheet.save();
                statusFlagMessage = "user is back again";
            }
            printStatusFlags(timesheet, statusFlagMessage);
        }
    }

    private boolean isManualInactiveState(Timesheet.State state) {
        return state == Timesheet.State.INACTIVE || state == Timesheet.State.INACTIVE_OFFLINE;
    }

    private void setAutoInactive(Timesheet timesheet) {
        LOGGER.error("Setting timesheet autoinactive of " + timesheet.getDisplayName());

        Date begin = new Date();
        Calendar cal = Calendar.getInstance();

        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, 7);

        Date end = cal.getTime();

        Team team = entryService.getLatestEntry(timesheet).getTeam();
        if(team == null){
            LOGGER.error("we got no team to add the entry to!!");
            return;
        }

        try {
            LOGGER.error("adding auto inactive entry to team: " + team.getTeamName());

            entryService.add(
                    timesheet,
                    begin,
                    begin,
                    categoryService.getCategoryByName("Inactive"),
                    "Auto generated inactivity entry",
                    0,
                    team,
                    false,
                    end,
                    "",
                    ""
            );
        } catch (ServiceException e) {
            e.printStackTrace();
            LOGGER.error("in autoinactive");
            LOGGER.error(e.getMessage());
        }
        timesheet.setState(Timesheet.State.AUTO_INACTIVE);
        timesheet.save();
    }

    private String printStatusFlags(Timesheet timesheet, String statusString) {
        String header = "\n Timesheet from: " + timesheet.getDisplayName() + "\n-------------------------------------\n";
        header += ("Transition: " + statusString) + "\n";
        String body = "state: " + timesheet.getState() + "\n";
        body += "latest Entry: " + timesheet.getLatestEntryBeginDate().toString() + "\n";
        body += "END Status \n-------------------------------------" + "\n" ;
        String message = header + body + "\n";
        
        LOGGER.error(message);

        return message;
    }
}
