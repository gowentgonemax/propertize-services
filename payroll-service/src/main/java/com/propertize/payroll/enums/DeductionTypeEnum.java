package com.propertize.payroll.enums;

/**
 * Enum representing broad categories of payroll deductions.
 */
public enum DeductionTypeEnum {
    TAX,           // Tax-related deduction
    BENEFIT,       // Benefit-related deduction
    BENEFITS,      // Alias for BENEFIT
    GARNISHMENT,   // Court-ordered garnishment
    RETIREMENT,    // 401k, pension
    INSURANCE,     // Health, dental, vision
    UNION_DUES,    // Union membership
    LOAN,          // Employee loan repayment
    CHILD_SUPPORT, // Child support garnishment
    OTHER          // Miscellaneous
}

