package com.propertize.commons.enums.employee;

import lombok.Getter;

/**
 * Canonical pay frequency enum shared across all Propertize services.
 *
 * <p>
 * Superset consolidation of {@code payroll-service} (6 values with
 * periodsPerYear)
 * and {@code employee-service} (4 values) versions.
 */
@Getter
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
}
