package com.propertize.platform.employecraft.enums;

/**
 * Pay frequency for salary/wage distribution
 */
public enum PayFrequencyEnum {
    WEEKLY("Weekly"),
    BIWEEKLY("Bi-Weekly"),
    SEMI_MONTHLY("Semi-Monthly"),
    MONTHLY("Monthly");

    private final String displayName;

    PayFrequencyEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
