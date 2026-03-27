package com.propertize.payroll.enums;

/**
 * Enum representing the status of a pay period.
 */
public enum PayPeriodStatusEnum {
    /**
     * Pay period is open for time entry.
     */
    OPEN,

    /**
     * Pay period is being processed for payroll.
     */
    PROCESSING,

    /**
     * Pay period is closed and locked.
     */
    CLOSED
}
