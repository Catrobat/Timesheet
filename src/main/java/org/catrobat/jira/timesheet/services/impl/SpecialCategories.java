package org.catrobat.jira.timesheet.services.impl;

import com.google.common.collect.ImmutableList;

public class SpecialCategories {
    private SpecialCategories() {}

    private static final String INACTIVE = "Inactive";
    private static final String DEACTIVATED= "Deactivated";
    public static final String THEORY = "Theory";
    public static final String GOOGLEDOCSIMPORT = "GoogleDocsImport";
    public static final ImmutableList<String> DefaultCategories = ImmutableList.of(INACTIVE, DEACTIVATED, THEORY);
    public static final ImmutableList<String> AllSpecialCategories = ImmutableList.of(GOOGLEDOCSIMPORT, INACTIVE, DEACTIVATED, THEORY);
}
