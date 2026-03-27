package com.propertize.payment.enums;

import lombok.Getter;

/**
 * Payment Frequency Enum
 * Defines how often payments are made
 */
@Getter
public enum PaymentFrequencyEnum {
    DAILY("Daily", "Every day", 1),
    WEEKLY("Weekly", "Every week", 7),
    BI_WEEKLY("Bi-Weekly", "Every two weeks", 14),
    SEMI_MONTHLY("Semi-Monthly", "Twice per month (1st and 15th)", 15),
    MONTHLY("Monthly", "Every month", 30),
    QUARTERLY("Quarterly", "Every three months", 90),
    SEMI_ANNUALLY("Semi-Annually", "Every six months", 180),
    ANNUALLY("Annually", "Once per year", 365),
    ONE_TIME("One-Time", "Single payment", 0);

    private final String displayName;
    private final String description;
    private final int approximateDays;

    PaymentFrequencyEnum(String displayName, String description, int approximateDays) {
        this.displayName = displayName;
        this.description = description;
        this.approximateDays = approximateDays;
    }

    /**
     * Parse from string value
     */
    public static PaymentFrequencyEnum fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.toUpperCase()
            .replace(" ", "_")
            .replace("-", "_");

        try {
            return PaymentFrequencyEnum.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching by display name
            for (PaymentFrequencyEnum freq : values()) {
                if (freq.displayName.equalsIgnoreCase(value)) {
                    return freq;
                }
            }
            return MONTHLY; // Default
        }
    }

    /**
     * Check if this is a recurring payment
     */
    public boolean isRecurring() {
        return this != ONE_TIME;
    }

    /**
     * Get number of payments per year
     */
    public int getPaymentsPerYear() {
        return switch (this) {
            case DAILY -> 365;
            case WEEKLY -> 52;
            case BI_WEEKLY -> 26;
            case SEMI_MONTHLY -> 24;
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case SEMI_ANNUALLY -> 2;
            case ANNUALLY -> 1;
            case ONE_TIME -> 1;
        };
    }
}
