package com.propertize.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of discounts applied by promo codes.
 */
@Getter
@RequiredArgsConstructor
public enum DiscountTypeEnum {

    PERCENTAGE("Percentage", "Percentage off the total amount"),
    FIXED("Fixed Amount", "Fixed dollar amount off"),
    WAIVE("Full Waive", "Waives the entire fee");

    private final String displayName;
    private final String description;
}
