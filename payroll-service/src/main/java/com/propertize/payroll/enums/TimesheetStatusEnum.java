package com.propertize.payroll.enums;

/**
 * Enum representing the status of a timesheet.
 */
public enum TimesheetStatusEnum {
    /**
     * Timesheet is in draft state, can be edited.
     */
    DRAFT,

    /**
     * Timesheet has been submitted for approval.
     */
    SUBMITTED,

    /**
     * Timesheet has been approved.
     */
    APPROVED,

    /**
     * Timesheet has been rejected.
     */
    REJECTED
}
