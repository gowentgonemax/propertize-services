package com.propertize.payroll.enums;

/**
 * Enum representing payroll actions for audit trail.
 */
public enum PayrollActionEnum {
    /**
     * Payroll run was created.
     */
    CREATED,

    /**
     * Payroll was processed/calculated.
     */
    PROCESSED,

    /**
     * Payroll was approved.
     */
    APPROVED,

    /**
     * Payroll was voided/cancelled.
     */
    VOIDED,

    /**
     * Payroll was recalculated.
     */
    RECALCULATED,

    /**
     * Payroll was exported to file.
     */
    EXPORTED,

    /**
     * Payroll payments were issued.
     */
    PAID,

    /**
     * Payroll was reversed.
     */
    REVERSED,

    /**
     * Adjustment was made.
     */
    ADJUSTED,

    /**
     * Status was changed.
     */
    STATUS_CHANGED
}
