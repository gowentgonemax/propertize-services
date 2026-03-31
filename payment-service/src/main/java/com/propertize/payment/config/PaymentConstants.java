package com.propertize.payment.config;

/**
 * Application-wide payment constants.
 * Eliminates magic strings scattered across service and entity classes.
 */
public final class PaymentConstants {

    private PaymentConstants() {
    }

    /** ISO 4217 default currency code for all payment records. */
    public static final String DEFAULT_CURRENCY = "USD";

    /** Stripe-normalized default currency code (lowercase per Stripe API). */
    public static final String DEFAULT_CURRENCY_STRIPE = "usd";
}
