package com.propertize.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Installment Status Enumeration
 *
 * @author Propertize Team
 * @since January 4, 2026
 */
@Getter
@RequiredArgsConstructor
public enum InstallmentStatus {

    PENDING("Pending", "Installment is pending payment"),
    PAID("Paid", "Installment has been paid"),
    LATE("Late", "Installment is late"),
    MISSED("Missed", "Installment was missed");

    private final String displayName;
    private final String description;
}

