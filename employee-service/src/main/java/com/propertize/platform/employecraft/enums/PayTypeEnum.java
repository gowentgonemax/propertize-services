package com.propertize.platform.employecraft.enums;

/**
 * Pay type for compensation
 */
public enum PayTypeEnum {
    SALARY("Salary"),
    HOURLY("Hourly"),
    COMMISSION("Commission"),
    SALARY_PLUS_COMMISSION("Salary + Commission");

    private final String displayName;

    PayTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
