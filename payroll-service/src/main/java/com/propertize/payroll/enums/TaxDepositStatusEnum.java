package com.propertize.payroll.enums;

/**
 * Enum representing the status of a tax deposit.
 */
public enum TaxDepositStatusEnum {
    /**
     * Tax deposit is pending and not yet scheduled.
     */
    PENDING,

    /**
     * Tax deposit has been scheduled for payment.
     */
    SCHEDULED,

    /**
     * Tax deposit has been paid.
     */
    PAID,

    /**
     * Tax deposit is overdue.
     */
    OVERDUE,

    /**
     * Tax deposit payment failed.
     */
    FAILED
}
