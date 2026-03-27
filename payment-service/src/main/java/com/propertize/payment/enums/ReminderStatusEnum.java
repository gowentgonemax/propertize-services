package com.propertize.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Reminder Status Enumeration
 *
 * @author Propertize Team
 * @since January 4, 2026
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

