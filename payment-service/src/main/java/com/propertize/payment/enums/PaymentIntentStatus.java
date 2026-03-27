package com.propertize.payment.enums;

import lombok.Getter;

/**
 * Status of a payment intent
 */
@Getter
public enum PaymentIntentStatus {
    PENDING("Pending", "Payment intent created, awaiting processing"),
    PROCESSING("Processing", "Payment is being processed"),
    REQUIRES_ACTION("Requires Action", "Additional action required (e.g., 3D Secure)"),
    REQUIRES_CONFIRMATION("Requires Confirmation", "Awaiting confirmation from customer"),
    SUCCEEDED("Succeeded", "Payment succeeded"),
    FAILED("Failed", "Payment failed"),
    CANCELLED("Cancelled", "Payment intent cancelled"),
    EXPIRED("Expired", "Payment intent expired");

    private final String displayName;
    private final String description;

    PaymentIntentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}

