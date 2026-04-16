package com.propertize.payroll.enums;
/**
 * Status of a direct deposit bank account.
 * Replaces the magic string "PRENOTE" default on DirectDepositAccountEntity#status.
 */
public enum DirectDepositStatusEnum {
    PRENOTE,
    ACTIVE,
    INACTIVE
}
