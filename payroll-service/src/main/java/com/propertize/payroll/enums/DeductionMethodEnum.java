package com.propertize.payroll.enums;

/**
 * Enum representing the method used to calculate a deduction.
 */
public enum DeductionMethodEnum {
    FLAT_AMOUNT,    // Fixed dollar amount
    FIXED_AMOUNT,   // Alias for FLAT_AMOUNT (for backward compatibility)
    PERCENTAGE      // Percentage of gross pay
}

