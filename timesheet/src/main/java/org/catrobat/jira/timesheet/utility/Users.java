package org.catrobat.jira.timesheet.utility;


//threadsafe implementation
class Users {
    private static class Holder {
        static final Users INSTANCE = new Users();
    }

    public static Users getInstance() {
        return Holder.INSTANCE;
    }
}
