package com.propertize.payment.enums;

import lombok.Getter;

/**
 * Enum representing payment status in the system
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

