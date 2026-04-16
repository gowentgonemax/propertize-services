package com.propertize.commons.enums.payment;

import lombok.Getter;

/**
 * Canonical transaction status enum shared across all Propertize services.
 *
 * <p>
 * Replaces duplicate enums in {@code propertize} and {@code payment-service}.
 */
@Getter
public enum TransactionStatusEnum {
    PENDING("Pending", "Transaction is awaiting processing"),
    PROCESSING("Processing", "Transaction is being processed"),
    SUCCESS("Success", "Transaction completed successfully"),
    FAILED("Failed", "Transaction failed"),
    CANCELLED("Cancelled", "Transaction was cancelled"),
    REFUNDED("Refunded", "Transaction was refunded"),
    DISPUTED("Disputed", "Transaction is under dispute"),
    VOID("Void", "Transaction was voided");

    private final String displayName;
    private final String description;

    TransactionStatusEnum(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /** Check if status is final (no further changes expected). */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED
                || this == REFUNDED || this == VOID;
    }

    /** Check if transaction can be refunded. */
    public boolean canRefund() {
        return this == SUCCESS;
    }

    /** Check if transaction can be retried. */
    public boolean canRetry() {
        return this == FAILED || this == PENDING;
    }
}
