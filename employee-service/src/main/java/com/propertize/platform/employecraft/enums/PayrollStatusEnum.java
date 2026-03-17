package com.propertize.platform.employecraft.enums;

/**
 * Payroll processing status
 */
public enum PayrollStatusEnum {
    DRAFT("Draft"),
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved"),
    PROCESSING("Processing"),
    PROCESSED("Processed"),
    PAID("Paid"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PayrollStatusEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
