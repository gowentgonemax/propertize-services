package com.propertize.payment.enums;

import lombok.Getter;

/**
 * Generic Payment Type Enum - Context-agnostic payment type classification
 *
 * This enum replaces TenantPaymentTypeEnum and supports all payment contexts:
 * - Tenant payments (rent, deposits, utilities)
 * - Platform subscriptions
 * - Vendor payments (maintenance, services)
 * - Owner payouts (distributions, dividends)
 * - Administrative fees and charges
 *
 * Design Philosophy:
 * - Generic and reusable across all payment categories
 * - Descriptive names that work in any context
 * - Avoids context-specific terminology
 */
@Getter
public enum PaymentTypeEnum {
    // Recurring Payments
    RENT("Rent Payment"),
    SUBSCRIPTION("Subscription Fee"),
    RECURRING_FEE("Recurring Fee"),

    // Deposits & Refundable
    SECURITY_DEPOSIT("Security Deposit"),
    PET_DEPOSIT("Pet Deposit"),
    KEY_DEPOSIT("Key Deposit"),
    REFUND("Refund"),

    // Fees & Charges
    APPLICATION_FEE("Application Fee"),
    LATE_FEE("Late Payment Fee"),
    CLEANING_FEE("Cleaning Fee"),
    ADMINISTRATIVE_FEE("Administrative Fee"),
    PROCESSING_FEE("Processing Fee"),
    SERVICE_FEE("Service Fee"),

    // Operational Expenses
    UTILITY("Utility Payment"),
    MAINTENANCE("Maintenance Fee"),
    REPAIR("Repair Cost"),
    INSURANCE("Insurance Payment"),

    // Business Payments
    COMMISSION("Commission"),
    CONTRACTOR_PAYMENT("Contractor Payment"),
    VENDOR_PAYMENT("Vendor Payment"),
    VENDOR_SERVICE("Vendor Service"),
    OWNER_PAYOUT("Owner Payout"),

    // Miscellaneous
    PENALTY("Penalty Charge"),
    DAMAGE_CHARGE("Damage Charge"),
    REIMBURSEMENT("Reimbursement"),
    OTHER("Other");

    private final String displayName;

    PaymentTypeEnum(String displayName) {
        this.displayName = displayName;
    }
}

