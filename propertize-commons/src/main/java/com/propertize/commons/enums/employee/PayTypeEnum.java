package com.propertize.commons.enums.employee;

import lombok.Getter;

/**
 * Canonical pay type enum shared across all Propertize services.
 *
 * <p>
 * Superset consolidation of {@code employee-service} (4 values) and
 * {@code payroll-service} (2 values) versions.
 */
@Getter
public enum PayTypeEnum {
    SALARY("Salary"),
    HOURLY("Hourly"),
    COMMISSION("Commission"),
    SALARY_PLUS_COMMISSION("Salary + Commission");

    private final String displayName;

    PayTypeEnum(String displayName) {
        this.displayName = displayName;
    }
}
