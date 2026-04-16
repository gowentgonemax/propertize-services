package com.propertize.commons.enums.employee;

import lombok.Getter;

/**
 * Canonical payroll processing status enum shared across all Propertize
 * services.
 *
 * <p>
 * Superset consolidation of {@code employee-service} (8 values) and
 * {@code payroll-service} (5 values) versions. Includes COMPLETED from
 * payroll-service and PENDING_APPROVAL/PROCESSED/PAID/CANCELLED from
 * employee-service.
 */
@Getter
public enum PayrollStatusEnum {
    DRAFT("Draft"),
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved"),
    PROCESSING("Processing"),
    PROCESSED("Processed"),
    COMPLETED("Completed"),
    PAID("Paid"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PayrollStatusEnum(String displayName) {
        this.displayName = displayName;
    }
}
