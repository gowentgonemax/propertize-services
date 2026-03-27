package com.propertize.payment.enums;

/**
 * Payment Category Enum
 * Represents the high-level category of payment in the property management system
 */
public enum PaymentCategoryEnum {
    /**
     * Platform Subscription - Property managers paying SAAS owner for platform usage
     */
    PLATFORM_SUBSCRIPTION,

    /**
     * Tenant Payment - Tenants paying rent, utilities, fees to property managers
     */
    TENANT_PAYMENT,

    /**
     * Vendor Payment - Payments to maintenance providers, contractors, vendors
     */
    VENDOR_PAYMENT,

    /**
     * Owner Payout - Property managers paying property owners their share
     */
    OWNER_PAYOUT,

    /**
     * Security Deposit - Special handling with refund workflows
     */
    SECURITY_DEPOSIT,

    /**
     * Late Fee - Auto-generated penalty charges
     */
    LATE_FEE,

    /**
     * Utility Payment - Utility bills and charges
     */
    UTILITY_PAYMENT,

    /**
     * Maintenance Payment - Maintenance-related costs
     */
    MAINTENANCE_PAYMENT,

    /**
     * Insurance Payment - Insurance premium payments
     */
    INSURANCE_PAYMENT,

    /**
     * Other - Miscellaneous payments
     */
    OTHER
}

