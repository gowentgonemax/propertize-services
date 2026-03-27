package com.propertize.payment.enums;

/**
 * Enum for payment channels/methods of payment collection
 */
public enum PaymentChannelEnum {
    ONLINE("Online Payment"),
    IN_PERSON("In-Person"),
    MAIL("Mail/Check"),
    BANK_TRANSFER("Bank Transfer"),
    WIRE_TRANSFER("Wire Transfer"),
    AUTO_PAY("Auto-Pay/Automatic"),
    CASH("Cash"),
    MONEY_ORDER("Money Order"),
    MOBILE_APP("Mobile App"),
    PAYMENT_PORTAL("Payment Portal");

    private final String displayName;

    PaymentChannelEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

