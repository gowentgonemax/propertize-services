package com.propertize.payment.enums;

/**
 * Payment Context Enum
 * Represents WHO the payment is associated with (the context/entity)
 */
public enum PaymentContextEnum {
    /**
     * Platform context - Platform-level payments (SAAS subscriptions)
     */
    PLATFORM,

    /**
     * Platform Admin context - Platform-level payments visible to platform oversight
     */
    PLATFORM_ADMIN,

    /**
     * Organization context - Payments at the organization/property manager level
     */
    ORGANIZATION,

    /**
     * Tenant context - Payments made by or for tenants
     */
    TENANT,

    /**
     * Vendor context - Payments to vendors
     */
    VENDOR,

    /**
     * Property Owner context - Payments to property owners
     */
    OWNER,

    /**
     * Property context - Payments related to specific properties
     */
    PROPERTY,

    /**
     * System context - System-generated payments
     */
    SYSTEM
}

