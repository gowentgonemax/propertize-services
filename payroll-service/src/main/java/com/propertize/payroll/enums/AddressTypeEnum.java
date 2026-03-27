package com.propertize.payroll.enums;

/**
 * Enum representing the type of address.
 */
public enum AddressTypeEnum {
    PRIMARY("Primary Address"),
    HOME("Home Address"),
    OFFICE("Office Address"),
    BILLING("Billing Address"),
    MAILING("Mailing Address"),
    WORK("Work Address"),
    OTHER("Other Address");

    private final String displayName;

    AddressTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
