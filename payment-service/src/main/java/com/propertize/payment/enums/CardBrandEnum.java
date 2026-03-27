package com.propertize.payment.enums;

import lombok.Getter;

/**
 * Enum representing supported credit/debit card brands.
 * Used for payment processing and display purposes.
 *
 * @author Propertize Team
 * @since January 5, 2026
 */
@Getter
public enum CardBrandEnum {
    VISA("Visa", "visa", "4"),
    MASTERCARD("Mastercard", "mastercard", "5"),
    AMEX("American Express", "amex", "34,37"),
    DISCOVER("Discover", "discover", "6011,644-649,65"),
    UNKNOWN("Unknown", "unknown", "");

    private final String displayName;
    private final String stripeCode;
    private final String binPrefixes;

    CardBrandEnum(String displayName, String stripeCode, String binPrefixes) {
        this.displayName = displayName;
        this.stripeCode = stripeCode;
        this.binPrefixes = binPrefixes;
    }

    /**
     * Get card brand from Stripe brand code
     */
    public static CardBrandEnum fromStripeCode(String code) {
        if (code == null) return UNKNOWN;
        return switch (code.toLowerCase()) {
            case "visa" -> VISA;
            case "mastercard" -> MASTERCARD;
            case "amex", "american_express" -> AMEX;
            case "discover" -> DISCOVER;
            default -> UNKNOWN;
        };
    }

    /**
     * Get card brand from card number prefix (BIN)
     */
    public static CardBrandEnum fromCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 2) return UNKNOWN;
        String firstDigit = cardNumber.substring(0, 1);
        String firstTwo = cardNumber.substring(0, Math.min(2, cardNumber.length()));
        String firstFour = cardNumber.length() >= 4 ? cardNumber.substring(0, 4) : cardNumber;

        if (firstDigit.equals("4")) return VISA;
        if (firstDigit.equals("5")) return MASTERCARD;
        if (firstTwo.equals("34") || firstTwo.equals("37")) return AMEX;
        if (firstFour.equals("6011") || firstTwo.equals("65") ||
            (cardNumber.length() >= 3 && cardNumber.substring(0, 3).compareTo("644") >= 0 &&
             cardNumber.substring(0, 3).compareTo("649") <= 0)) return DISCOVER;

        return UNKNOWN;
    }
}

