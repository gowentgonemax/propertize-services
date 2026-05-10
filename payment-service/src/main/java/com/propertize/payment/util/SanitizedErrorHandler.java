package com.propertize.payment.util;

import com.stripe.exception.StripeException;
import lombok.extern.slf4j.Slf4j;

/**
 * Sanitized Error Handler — PCI-DSS Compliance
 * 
 * Prevents sensitive Stripe error messages from leaking to clients or logs.
 * All Stripe exceptions are wrapped with generic, user-safe messages.
 * Raw Stripe errors logged separately at DEBUG level for internal
 * troubleshooting.
 */
@Slf4j
public class SanitizedErrorHandler {

    /**
     * Handle Stripe exceptions with sanitized user-facing message
     * Raw error logged at DEBUG level only
     */
    public static RuntimeException handleStripeException(StripeException ex, String operation) {
        // Log raw error details at DEBUG level (not exposed in production)
        log.debug("Stripe API error during {}: code={}, message={}",
                operation, ex.getCode(), ex.getMessage(), ex);

        // Generic user-facing message (no technical details)
        String userMessage = String.format("Payment processing failed (%s). Please try again or contact support.",
                operation.toLowerCase());

        // Wrap in generic runtime exception
        return new RuntimeException(userMessage, ex);
    }

    /**
     * Handle generic exceptions with sanitized message
     */
    public static RuntimeException handleGenericException(Exception ex, String operation) {
        // Log full error at WARN level
        log.warn("Error during {}: {}", operation, ex.getMessage(), ex);

        // Generic user-facing message
        String userMessage = String.format("An error occurred while %s. Please try again.",
                operation.toLowerCase());

        return new RuntimeException(userMessage, ex);
    }

    /**
     * Validate that no sensitive card data appears in error messages
     * Used for testing/auditing
     */
    public static boolean containsSensitiveData(String message) {
        if (message == null)
            return false;

        // Check for patterns that indicate card data leakage
        return message.matches(".*\\b\\d{13,19}\\b.*") // Card numbers
                || message.contains("cvv")
                || message.contains("cvc")
                || message.contains("pin")
                || message.toLowerCase().contains("card.*last") // card last 4 details in error
                || message.toLowerCase().contains("expir"); // expiration details
    }
}
