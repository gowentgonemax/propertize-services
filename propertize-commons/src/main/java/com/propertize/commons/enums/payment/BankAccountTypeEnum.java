package com.propertize.commons.enums.payment;

import lombok.Getter;

/**
 * Canonical bank account type enum shared across all Propertize services.
 *
 * <p>
 * Replaces duplicate enums in {@code propertize}, {@code payment-service},
 * and {@code payroll-service}.
 */
@Getter
public enum BankAccountTypeEnum {
    CHECKING("Personal Checking"),
    SAVINGS("Personal Savings"),
    BUSINESS_CHECKING("Business Checking"),
    BUSINESS_SAVINGS("Business Savings"),
    MONEY_MARKET("Money Market"),
    ESCROW("Escrow Account");

    private final String description;

    BankAccountTypeEnum(String description) {
        this.description = description;
    }
}
