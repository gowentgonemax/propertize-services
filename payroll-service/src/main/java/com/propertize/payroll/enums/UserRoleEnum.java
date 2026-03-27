package com.propertize.payroll.enums;

/**
 * Enum representing user roles in the system.
 */
public enum UserRoleEnum {
    ROLE_SUPER_ADMIN("Super Admin", "Full system access"),
    ROLE_ADMIN("Admin", "Organization admin access"),
    ROLE_HR_MANAGER("HR Manager", "HR and employee management access"),
    ROLE_PAYROLL_ADMIN("Payroll Admin", "Payroll administration access"),
    ROLE_PAYROLL_PROCESSOR("Payroll Processor", "Payroll processing access"),
    ROLE_ACCOUNTANT("Accountant", "Financial and accounting access"),
    ROLE_MANAGER("Manager", "Team management access"),
    ROLE_EMPLOYEE("Employee", "Basic employee access"),
    ROLE_USER("User", "Default user access"),
    ROLE_READ_ONLY("Read Only", "View-only access");

    private final String displayName;
    private final String description;

    UserRoleEnum(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
