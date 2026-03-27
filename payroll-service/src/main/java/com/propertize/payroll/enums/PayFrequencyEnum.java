package com.propertize.payroll.enums;

/**
 * Enum representing pay frequency for payroll.
 */
public enum PayFrequencyEnum {
    WEEKLY("Weekly", 52),
    BI_WEEKLY("Bi-Weekly", 26),
    SEMI_MONTHLY("Semi-Monthly", 24),
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    ANNUALLY("Annually", 1);

    private final String displayName;
    private final int periodsPerYear;

    PayFrequencyEnum(String displayName, int periodsPerYear) {
        this.displayName = displayName;
        this.periodsPerYear = periodsPerYear;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPeriodsPerYear() {
        return periodsPerYear;
    }
}
