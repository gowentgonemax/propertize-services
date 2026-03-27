package com.propertize.payment.enums;

import lombok.Getter;

/**
 * Payment method types supported by the system
 */
@Getter
public enum PaymentMethodType {
    CARD("Card"),
    BANK_ACCOUNT("Bank Account"),
    ACH("ACH Transfer"),
    WIRE_TRANSFER("Wire Transfer"),
    CHECK("Check"),
    CASH("Cash"),
    OTHER("Other");

    private final String displayName;

    PaymentMethodType(String displayName) {
        this.displayName = displayName;
    }

}

