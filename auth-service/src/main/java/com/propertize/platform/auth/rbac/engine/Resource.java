package com.propertize.platform.auth.rbac.engine;

import com.propertize.platform.auth.rbac.enums.CategoryEnum;
import lombok.Getter;

/**
 * Centralized registry of all resources in the system.
 * Maps to resources defined in rbac.yml.
 *
 * @version 2.0 - Centralized in auth-service
 */
@Getter
public enum Resource {

    // CORE BUSINESS
    PROPERTY("property", "Property management", CategoryEnum.BUSINESS),
    LEASE("lease", "Lease management", CategoryEnum.BUSINESS),
    TENANT("tenant", "Tenant management", CategoryEnum.BUSINESS),
    PAYMENT("payment", "Payment processing", CategoryEnum.FINANCIAL),
    INVOICE("invoice", "Invoicing", CategoryEnum.FINANCIAL),
    LATE_FEE("late_fee", "Late fee management", CategoryEnum.FINANCIAL),

    // OPERATIONAL
    MAINTENANCE("maintenance", "Maintenance requests", CategoryEnum.OPERATIONS),
    INSPECTION("inspection", "Property inspections", CategoryEnum.OPERATIONS),
    ASSET("asset", "Asset management", CategoryEnum.OPERATIONS),
    VENDOR("vendor", "Vendor management", CategoryEnum.OPERATIONS),
    UTILITY("utility", "Utility tracking", CategoryEnum.OPERATIONS),
    TASK("task", "Task management", CategoryEnum.OPERATIONS),
    SCHEDULE("schedule", "Schedule management", CategoryEnum.OPERATIONS),

    // SYSTEM
    USER("user", "User management", CategoryEnum.SYSTEM),
    ORGANIZATION("organization", "Organization management", CategoryEnum.SYSTEM),
    ROLE("role", "Role management", CategoryEnum.SYSTEM),
    SESSION("session", "Session management", CategoryEnum.SYSTEM),
    SYSTEM("system", "System administration", CategoryEnum.SYSTEM),
    PLATFORM("platform", "Platform management", CategoryEnum.SYSTEM),
    AUDIT_LOG("audit_log", "Audit logs", CategoryEnum.SYSTEM),

    // COMMUNICATION
    NOTIFICATION("notification", "Notifications", CategoryEnum.COMMUNICATION),
    REMINDER("reminder", "Payment reminders", CategoryEnum.COMMUNICATION),
    DOCUMENT("document", "Document management", CategoryEnum.COMMUNICATION),
    CONTACT("contact", "Contact management", CategoryEnum.COMMUNICATION),

    // WORKFLOW
    RENTAL_APPLICATION("rental_application", "Rental applications", CategoryEnum.WORKFLOW),
    WORKFLOW("workflow", "Workflow management", CategoryEnum.WORKFLOW),
    ONBOARDING("onboarding", "Organization onboarding", CategoryEnum.WORKFLOW),
    APPROVAL("approval", "Approval workflows", CategoryEnum.WORKFLOW),

    // ANALYTICS
    REPORT("report", "Reporting", CategoryEnum.ANALYTICS),
    DASHBOARD("dashboard", "Dashboard access", CategoryEnum.ANALYTICS),
    MILESTONE("milestone", "Milestone tracking", CategoryEnum.ANALYTICS);

    private final String key;
    private final String description;
    private final CategoryEnum categoryEnum;

    Resource(String key, String description, CategoryEnum categoryEnum) {
        this.key = key;
        this.description = description;
        this.categoryEnum = categoryEnum;
    }

    public static Resource fromKey(String key) {
        for (Resource resource : values()) {
            if (resource.key.equals(key)) {
                return resource;
            }
        }
        throw new IllegalArgumentException("Unknown resource key: " + key);
    }

    public static boolean exists(String key) {
        for (Resource resource : values()) {
            if (resource.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return key;
    }
}
