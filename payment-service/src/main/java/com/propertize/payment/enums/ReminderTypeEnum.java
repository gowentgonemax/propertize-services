package com.propertize.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Reminder Type Enumeration
 *
 * Defines the types of payment reminders
 *
 * @author Propertize Team
 * @since January 4, 2026
 */
@Getter
@RequiredArgsConstructor
public enum ReminderTypeEnum {

    /**
     * Courtesy reminder - sent 7 days before due date
     */
    COURTESY("Courtesy Reminder", 7, true),

    /**
     * Final reminder - sent 1 day before due date
     */
    FINAL("Final Reminder", 1, true),

    /**
     * Due today - sent on the due date
     */
    DUE_TODAY("Payment Due Today", 0, true),

    /**
     * Overdue notice - sent 3 days after due date
     */
    OVERDUE("Overdue Payment Notice", -3, false);

    private final String displayName;
    private final int daysBeforeDue; // Positive = before, Negative = after, 0 = on due date
    private final boolean isPreDue;

    /**
     * Check if this is a pre-due reminder
     */
    public boolean isBeforeDueDate() {
        return daysBeforeDue > 0;
    }

    /**
     * Check if this is an overdue reminder
     */
    public boolean isAfterDueDate() {
        return daysBeforeDue < 0;
    }

    /**
     * Check if this is due date reminder
     */
    public boolean isDueDate() {
        return daysBeforeDue == 0;
    }
}

