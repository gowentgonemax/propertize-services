package com.propertize.platform.employecraft.enums;

/**
 * Employee lifecycle status
 */
public enum EmployeeStatusEnum {
    PENDING("Pending Onboarding"),
    ACTIVE("Active"),
    ON_LEAVE("On Leave"),
    SUSPENDED("Suspended"),
    TERMINATED("Terminated"),
    RETIRED("Retired");

    private final String displayName;

    EmployeeStatusEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
