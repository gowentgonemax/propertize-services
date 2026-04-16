package com.propertize.commons.enums.payment;

import lombok.Getter;

/**
 * Canonical payment method enum shared across all Propertize services.
 *
 * <p>
 * Replaces duplicate enums in {@code propertize} and {@code payment-service}.
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
