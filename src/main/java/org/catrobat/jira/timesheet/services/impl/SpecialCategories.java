package org.catrobat.jira.timesheet.services.impl;

import com.google.common.collect.ImmutableList;

public class SpecialCategories {
    private SpecialCategories() {}

    public static final String INACTIVE = "Inactive";
    public static final String INACTIVE_OFFLINE = "Inactive & Offline";
    public static final String THEORY = "Theory";
    public static final String RESEARCH = "Research";
    public static final String GOOGLEDOCSIMPORT = "GoogleDocsImport";

    public static final String MEETING = "Meeting";
    public static final String PAIR_PROGRAMMING = "Pair programming";
    public static final String PROGRAMMING = "Programming";
    public static final String PLANNING_GAME = "Planning Game";
    public static final String REFACTORING = "Refactoring";
    public static final String REFACTORING_PP = "Refactoring (PP)";
    public static final String CODE_ACCEPTANCE = "Code Acceptance";
    public static final String ORGANISATIONAL_TASKS = "Organisational tasks";
    public static final String DISCUSSING_ISSUES_SUPPORTING_CONSULTING = "Discussing issues/Supporting/Consulting";
    public static final String OTHER = "Other";
    public static final String BUG_FIXING_PP = "Bug fixing (PP)";
    public static final String BUG_FIXING = "Bug fixing";
    public static final String DEFAULT = "Default Category (original category got deleted)";
    
    public static final ImmutableList<String> DefaultCategories = 
    		ImmutableList.of(INACTIVE, INACTIVE_OFFLINE, THEORY, RESEARCH);
    
    public static final ImmutableList<String> AllSpecialCategories = 
    		ImmutableList.of(GOOGLEDOCSIMPORT, INACTIVE, INACTIVE_OFFLINE, THEORY, DEFAULT);
    
    public static final ImmutableList<String> PredefinedCategories = 
    		ImmutableList.of(RESEARCH, MEETING, PAIR_PROGRAMMING, PROGRAMMING, PLANNING_GAME, 
    				REFACTORING, REFACTORING_PP, CODE_ACCEPTANCE, ORGANISATIONAL_TASKS, 
    				DISCUSSING_ISSUES_SUPPORTING_CONSULTING, OTHER, BUG_FIXING_PP, BUG_FIXING);
}
