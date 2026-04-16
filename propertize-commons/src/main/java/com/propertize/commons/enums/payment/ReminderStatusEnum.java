package com.propertize.commons.enums.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Canonical reminder status enum shared across all Propertize services.
 *
 * <p>
 * Replaces duplicate enums in {@code propertize} and {@code payment-service}.
 */
@Getter
@RequiredArgsConstructor
public enum ReminderStatusEnum {
    SCHEDULED("Scheduled", "Reminder is scheduled to be sent"),
    PENDING("Pending", "Reminder is being processed"),
    SENT("Sent", "Reminder was sent successfully"),
    FAILED("Failed", "Reminder failed to send"),
    CANCELLED("Cancelled", "Reminder was cancelled");

    private final String displayName;
    private final String description;
}
