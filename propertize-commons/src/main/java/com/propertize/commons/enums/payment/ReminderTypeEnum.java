package com.propertize.commons.enums.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Canonical reminder type enum shared across all Propertize services.
 *
 * <p>
 * Defines the types of payment reminders.
 *
 * <p>
 * Replaces duplicate enums in {@code propertize} and {@code payment-service}.
 */
@Getter
@RequiredArgsConstructor
public enum ReminderTypeEnum {
    /** Courtesy reminder — sent 7 days before due date. */
    COURTESY("Courtesy Reminder", 7, true),
    /** Final reminder — sent 1 day before due date. */
    FINAL("Final Reminder", 1, true),
    /** Due today — sent on the due date. */
    DUE_TODAY("Payment Due Today", 0, true),
    /** Overdue notice — sent 3 days after due date. */
    OVERDUE("Overdue Payment Notice", -3, false);

    private final String displayName;
    /** Positive = before, Negative = after, 0 = on due date. */
    private final int daysBeforeDue;
    private final boolean isPreDue;

    public boolean isBeforeDueDate() {
        return daysBeforeDue > 0;
    }

    public boolean isAfterDueDate() {
        return daysBeforeDue < 0;
    }

    public boolean isDueDate() {
        return daysBeforeDue == 0;
    }
}
