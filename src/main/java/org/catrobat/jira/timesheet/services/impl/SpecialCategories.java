package org.catrobat.jira.timesheet.services.impl;

import com.google.common.collect.ImmutableList;

public class SpecialCategories {
    private SpecialCategories() {}

    public static final String INACTIVE = "Inactive";
    public static final String INACTIVE_OFFLINE = "Inactive & Offline";
    public static final String THEORY = "Theory";
    public static final String GOOGLEDOCSIMPORT = "GoogleDocsImport";
    public static final String DEFAULT = "Default Category (original cateogry got deleted)";
    public static final ImmutableList<String> DefaultCategories = ImmutableList.of(INACTIVE, INACTIVE_OFFLINE, THEORY);
    public static final ImmutableList<String> AllSpecialCategories = ImmutableList.of(GOOGLEDOCSIMPORT, INACTIVE, INACTIVE_OFFLINE, THEORY, DEFAULT);
}
