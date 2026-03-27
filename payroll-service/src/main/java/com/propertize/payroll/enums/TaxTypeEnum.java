package com.propertize.payroll.enums;

/**
 * Enum representing types of taxes in payroll processing.
 */
public enum TaxTypeEnum {
    FEDERAL,
    STATE,
    LOCAL,
    FICA_SS,
    FICA_MEDICARE,
    SOCIAL_SECURITY,  // Alias for FICA_SS
    MEDICARE,         // Alias for FICA_MEDICARE
    FUTA,             // Federal Unemployment
    SUTA,             // State Unemployment
    SDI,              // State Disability Insurance
    FLI               // Family Leave Insurance
}

