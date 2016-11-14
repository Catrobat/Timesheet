package org.catrobat.jira.timesheet.services.impl;

import com.google.common.collect.ImmutableList;

public class SpecialCategories {
    private SpecialCategories() {}

    private static final String INACTIVE = "Inactive";
    private static final String DEACTIVATED= "Deactivated";
    public static final ImmutableList<String> LIST = ImmutableList.of(INACTIVE, DEACTIVATED);
}
