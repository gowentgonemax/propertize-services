package com.propertize.commons.enums.payment;

import lombok.Getter;

/**
 * Canonical payment status enum shared across all Propertize services.
 *
 * <p>
 * Replaces duplicate enums in {@code propertize} and {@code payment-service}.
 */
@Getter
public enum PaymentStatusEnum {
    PENDING("Pending"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded"),
    PARTIALLY_REFUNDED("Partially Refunded"),
    SCHEDULED("Scheduled"),
    OVERDUE("Overdue"),
    PAID("Paid");

    private final String displayName;

    PaymentStatusEnum(String displayName) {
        this.displayName = displayName;
    }
}
