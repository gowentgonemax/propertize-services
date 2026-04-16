package com.propertize.commons.enums.employee;

import lombok.Getter;

/**
 * Canonical employment type enum shared across all Propertize services.
 *
 * <p>
 * Superset consolidation of {@code employee-service} (5 values) and
 * {@code payroll-service} (3 values) versions.
 */
@Getter
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
}
