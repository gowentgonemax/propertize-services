package com.propertize.commons.enums.employee;

import lombok.Getter;

/**
 * Canonical employee lifecycle status enum shared across all Propertize
 * services.
 *
 * <p>
 * Superset consolidation of {@code employee-service} and
 * {@code payroll-service} versions.
 */
@Getter
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
}
