package com.propertize.platform.auth.rbac.enums;

import lombok.Getter;

/**
 * Resource category classifications for RBAC engine
 */
@Getter
public enum CategoryEnum {
    BUSINESS("Core Business Operations"),
    FINANCIAL("Financial Operations"),
    OPERATIONS("Operational Management"),
    SYSTEM("System Administration"),
    COMMUNICATION("Communication & Documents"),
    WORKFLOW("Workflows & Processes"),
    ANALYTICS("Analytics & Reporting");

    private final String description;

    CategoryEnum(String description) {
        this.description = description;
    }
}
