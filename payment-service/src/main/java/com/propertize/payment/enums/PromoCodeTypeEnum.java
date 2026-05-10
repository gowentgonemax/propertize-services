package com.propertize.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of promo codes — categorizes what the promo code can be applied to.
 */
@Getter
@RequiredArgsConstructor
public enum PromoCodeTypeEnum {

    RENTAL_APPLICATION_FEE("Application Fee", "Discount on rental application fees"),
    MONTHLY_RENT("Monthly Rent", "Discount on monthly rent payments"),
    SECURITY_DEPOSIT("Security Deposit", "Discount on security deposit"),
    LATE_FEE("Late Fee", "Discount on late fee charges"),
    GENERAL("General", "General-purpose promo code applicable to any payment");

    private final String displayName;
    private final String description;
}
