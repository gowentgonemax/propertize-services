package com.propertize.payroll.enums;

/**
 * Enum representing the type of payroll run.
 */
public enum PayrollTypeEnum {
    REGULAR("Regular Payroll"),
    OFF_CYCLE("Off-Cycle Payroll"),
    BONUS("Bonus Payroll"),
    COMMISSION("Commission Payroll"),
    CORRECTION("Correction Payroll"),
    FINAL("Final Payroll");

    private final String displayName;

    PayrollTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
