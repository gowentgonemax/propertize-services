package com.propertize.payroll.enums;
/**
 * Allocation strategy for a direct deposit account.
 * Replaces the magic string "REMAINDER" default on DirectDepositAccountEntity#allocationType.
 */
public enum DirectDepositAllocationTypeEnum {
    /** A fixed monetary amount deposited each pay period. */
    FIXED_AMOUNT,
    /** A percentage of net pay deposited each pay period. */
    PERCENTAGE,
    /** Remainder of net pay after all other allocations. */
    REMAINDER
}
