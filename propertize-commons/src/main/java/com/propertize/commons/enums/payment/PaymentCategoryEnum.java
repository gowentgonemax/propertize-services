package com.propertize.commons.enums.payment;

/**
 * Canonical payment category enum shared across all Propertize services.
 *
 * <p>
 * Represents the high-level category of a payment in the property management
 * system.
 *
 * <p>
 * Replaces duplicate enums in {@code propertize} and {@code payment-service}.
 */
public enum PaymentCategoryEnum {
    /**
     * Platform Subscription — property managers paying SaaS owner for platform
     * usage.
     */
    PLATFORM_SUBSCRIPTION,
    /**
     * Tenant Payment — tenants paying rent, utilities, fees to property managers.
     */
    TENANT_PAYMENT,
    /** Vendor Payment — payments to maintenance providers, contractors, vendors. */
    VENDOR_PAYMENT,
    /** Owner Payout — property managers paying property owners their share. */
    OWNER_PAYOUT,
    /** Security Deposit — special handling with refund workflows. */
    SECURITY_DEPOSIT,
    /** Late Fee — auto-generated penalty charges. */
    LATE_FEE,
    /** Utility Payment — utility bills and charges. */
    UTILITY_PAYMENT,
    /** Maintenance Payment — maintenance-related costs. */
    MAINTENANCE_PAYMENT,
    /** Insurance Payment — insurance premium payments. */
    INSURANCE_PAYMENT,
    /** Other — miscellaneous payments. */
    OTHER
}
