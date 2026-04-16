package com.propertize.commons.enums.common;

import lombok.Getter;

/**
 * Unified enum representing different types of addresses in the system.
 * Merges address types from both property management and payroll contexts.
 */
@Getter
public enum AddressTypeEnum {
    // Personal/Individual Addresses
    CURRENT("Current Address", "The current residential address"),
    PREVIOUS("Previous Address", "A previous residential address"),
    MAILING("Mailing Address", "Address for correspondence"),
    EMERGENCY("Emergency Contact Address", "Address of emergency contact"),
    HOME("Home Address", "Home/residential address"),

    // Business/Organization Addresses
    BUSINESS("Business Address", "Primary business/office address"),
    BILLING("Billing Address", "Address for billing and invoices"),
    SHIPPING("Shipping Address", "Address for deliveries and shipments"),
    REGISTERED("Registered Address", "Legal/registered business address"),
    OFFICE("Office Address", "Office location address"),
    WORK("Work Address", "Workplace address"),

    // Property Addresses
    PROPERTY("Property Address", "Physical property location"),

    // Employment Addresses
    EMPLOYER("Employer Address", "Address of employer/workplace"),

    // Reference Addresses
    REFERENCE("Reference Address", "Address of a reference contact"),

    // General
    PRIMARY("Primary Address", "Default primary address"),

    // Other
    OTHER("Other Address", "Any other type of address");

    private final String displayName;
    private final String description;

    AddressTypeEnum(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
