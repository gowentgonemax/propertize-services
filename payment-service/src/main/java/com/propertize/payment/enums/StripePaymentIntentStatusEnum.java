package com.propertize.payment.enums;

import lombok.Getter;

/**
 * Stripe Payment Intent Status Values
 *
 * Represents the lifecycle status of a Stripe Payment Intent.
 *
 * @see <a href="https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status">Stripe API Documentation</a>
 */
@Getter
public enum StripePaymentIntentStatusEnum {
    /**
     * Payment Intent created, requires payment method
     */
    REQUIRES_PAYMENT_METHOD("requires_payment_method", "Requires Payment Method",
        "Customer must provide a payment method"),

    /**
     * Payment method provided, requires customer confirmation
     */
    REQUIRES_CONFIRMATION("requires_confirmation", "Requires Confirmation",
        "Customer must confirm the payment"),

    /**
     * Requires additional customer action (e.g., 3D Secure authentication)
     */
    REQUIRES_ACTION("requires_action", "Requires Action",
        "Customer must complete additional authentication"),

    /**
     * Payment is being processed by the payment provider
     */
    PROCESSING("processing", "Processing",
        "Payment is being processed"),

    /**
     * Payment requires customer to be present
     */
    REQUIRES_CAPTURE("requires_capture", "Requires Capture",
        "Payment authorized, awaiting manual capture"),

    /**
     * Payment succeeded
     */
    SUCCEEDED("succeeded", "Succeeded",
        "Payment completed successfully"),

    /**
     * Payment was canceled
     */
    CANCELED("canceled", "Canceled",
        "Payment was canceled before completion"),

    /**
     * Payment failed
     */
    FAILED("failed", "Failed",
        "Payment failed");

    private final String stripeValue;
    private final String displayName;
    private final String description;

    StripePaymentIntentStatusEnum(String stripeValue, String displayName, String description) {
        this.stripeValue = stripeValue;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Convert from Stripe API string value to enum
     *
     * @param stripeValue The value from Stripe API
     * @return The corresponding enum value
     * @throws IllegalArgumentException if the value is not recognized
     */
    public static StripePaymentIntentStatusEnum fromStripeValue(String stripeValue) {
        if (stripeValue == null) {
            throw new IllegalArgumentException("Stripe status cannot be null");
        }

        for (StripePaymentIntentStatusEnum status : values()) {
            if (status.stripeValue.equalsIgnoreCase(stripeValue)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown Stripe payment intent status: " + stripeValue);
    }

    /**
     * Check if the payment is in a final state (cannot be changed)
     */
    public boolean isFinal() {
        return this == SUCCEEDED || this == CANCELED || this == FAILED;
    }

    /**
     * Check if the payment requires customer action
     */
    public boolean requiresCustomerAction() {
        return this == REQUIRES_PAYMENT_METHOD ||
               this == REQUIRES_CONFIRMATION ||
               this == REQUIRES_ACTION;
    }

    /**
     * Check if the payment is successful
     */
    public boolean isSuccessful() {
        return this == SUCCEEDED;
    }

    /**
     * Map to internal PaymentStatusEnum
     */
    public PaymentStatusEnum toInternalStatus() {
        return switch (this) {
            case SUCCEEDED -> PaymentStatusEnum.PAID;
            case PROCESSING, REQUIRES_CAPTURE -> PaymentStatusEnum.PROCESSING;
            case REQUIRES_PAYMENT_METHOD, REQUIRES_CONFIRMATION, REQUIRES_ACTION -> PaymentStatusEnum.PENDING;
            case CANCELED -> PaymentStatusEnum.CANCELLED;
            case FAILED -> PaymentStatusEnum.FAILED;
        };
    }
}
