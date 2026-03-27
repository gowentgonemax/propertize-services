package com.propertize.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Payment Plan Status Enumeration
 *
 * @author Propertize Team
 * @since January 4, 2026
 */
@Getter
@RequiredArgsConstructor
public enum PaymentPlanStatus {

    ACTIVE("Active", "Payment plan is active"),
    COMPLETED("Completed", "All installments paid"),
    DEFAULTED("Defaulted", "Payment plan defaulted"),
    CANCELLED("Cancelled", "Payment plan cancelled");

    private final String displayName;
    private final String description;
}

