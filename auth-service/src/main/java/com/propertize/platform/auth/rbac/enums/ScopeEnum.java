package com.propertize.platform.auth.rbac.enums;

import lombok.Getter;

/**
 * Action scope categories for RBAC engine
 */
@Getter
public enum ScopeEnum {
    READ("Read-only operations"),
    WRITE("Modification operations"),
    ADMIN("Administrative operations"),
    WORKFLOW("Workflow operations"),
    FINANCIAL("Financial operations"),
    DASHBOARD("Dashboard access");

    private final String description;

    ScopeEnum(String description) {
        this.description = description;
    }
}
