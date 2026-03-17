package com.propertize.platform.employecraft.enums;

/**
 * Employment type classification
 */
public enum EmploymentTypeEnum {
    FULL_TIME("Full Time"),
    PART_TIME("Part Time"),
    CONTRACT("Contract"),
    TEMPORARY("Temporary"),
    INTERN("Intern");

    private final String displayName;

    EmploymentTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
