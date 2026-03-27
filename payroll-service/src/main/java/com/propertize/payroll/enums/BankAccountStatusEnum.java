package com.propertize.payroll.enums;

/**
 * Enum representing the status of a bank account for direct deposit.
 */
public enum BankAccountStatusEnum {
    PENDING_VERIFICATION,  // Account submitted, awaiting verification
    ACTIVE,                // Verified and active for deposits
    INACTIVE,              // Temporarily disabled
    FAILED_VERIFICATION,   // Verification failed
    CLOSED                 // Account closed
}
