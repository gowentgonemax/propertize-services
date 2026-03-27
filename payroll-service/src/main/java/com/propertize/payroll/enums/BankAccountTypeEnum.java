package com.propertize.payroll.enums;

/**
 * Enum representing bank account type.
 */
public enum BankAccountTypeEnum {
    CHECKING("Checking Account"),
    SAVINGS("Savings Account");

    private final String displayName;

    BankAccountTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
