package com.propertize.commons.enums.payment;

/**
 * Canonical payment context enum shared across all Propertize services.
 *
 * <p>
 * Represents WHO the payment is associated with (the context/entity).
 *
 * <p>
 * Replaces duplicate enums in {@code propertize} and {@code payment-service}.
 */
public enum PaymentContextEnum {
    /** Platform-level payments (SaaS subscriptions). */
    PLATFORM,
    /** Platform-level payments visible to platform oversight. */
    PLATFORM_ADMIN,
    /** Payments at the organization / property manager level. */
    ORGANIZATION,
    /** Payments made by or for tenants. */
    TENANT,
    /** Payments to vendors. */
    VENDOR,
    /** Payments to property owners. */
    OWNER,
    /** Payments related to specific properties. */
    PROPERTY,
    /** System-generated payments. */
    SYSTEM
}
