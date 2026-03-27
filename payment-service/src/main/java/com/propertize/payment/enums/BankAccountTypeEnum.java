package com.propertize.payment.enums;

import lombok.Getter;

/**
 * Enumeration for bank account types
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

