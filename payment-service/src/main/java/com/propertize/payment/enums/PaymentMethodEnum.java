package com.propertize.payment.enums;

import lombok.Getter;

/**
 * Enum representing payment methods available in the system
 */
@Getter
public enum PaymentMethodEnum {
    CASH("Cash"),
    CHECK("Check"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    ACH("ACH Transfer"),
    WIRE_TRANSFER("Wire Transfer"),
    MONEY_ORDER("Money Order"),
    PAYPAL("PayPal"),
    VENMO("Venmo"),
    ZELLE("Zelle"),
    STRIPE("Stripe"),
    SQUARE("Square"),
    APPLE_PAY("Apple Pay"),
    GOOGLE_PAY("Google Pay"),
    OTHER("Other");

    private final String displayName;

    PaymentMethodEnum(String displayName) {
        this.displayName = displayName;
    }

}

