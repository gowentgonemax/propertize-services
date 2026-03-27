package com.propertize.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum defining supported payment gateway types.
 *
 * <p>Used by {@link com.propertize.factory.payment.PaymentGatewayFactory}
 * to identify and retrieve specific gateway implementations.</p>
 *
 * @author Propertize Team
 * @since January 2026
 */
@Getter
@RequiredArgsConstructor
public enum PaymentGatewayType {

    /**
     * Stripe payment gateway (default)
     */
    STRIPE("Stripe", "https://stripe.com", true, true),

    /**
     * PayPal payment gateway
     */
    PAYPAL("PayPal", "https://paypal.com", true, true),

    /**
     * Square payment gateway
     */
    SQUARE("Square", "https://squareup.com", true, false),

    /**
     * Authorize.net payment gateway
     */
    AUTHORIZE_NET("Authorize.net", "https://authorize.net", true, true),

    /**
     * Braintree payment gateway (owned by PayPal)
     */
    BRAINTREE("Braintree", "https://braintreepayments.com", true, true),

    /**
     * Manual/offline payment processing
     */
    MANUAL("Manual Processing", null, false, false),

    /**
     * ACH bank transfer processing
     */
    ACH("ACH Bank Transfer", null, true, false);

    private final String displayName;
    private final String websiteUrl;
    private final boolean supportsRefunds;
    private final boolean supportsRecurring;

    /**
     * Get gateway type from string (case-insensitive).
     *
     * @param name Gateway name
     * @return PaymentGatewayType or null if not found
     */
    public static PaymentGatewayType fromString(String name) {
        if (name == null || name.isBlank()) return null;

        try {
            return valueOf(name.toUpperCase().replace("-", "_").replace(".", "_"));
        } catch (IllegalArgumentException e) {
            // Try matching display name
            for (PaymentGatewayType type : values()) {
                if (type.displayName.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Check if this gateway type supports automated processing.
     *
     * @return true if gateway supports API-based processing
     */
    public boolean isAutomated() {
        return this != MANUAL;
    }
}
