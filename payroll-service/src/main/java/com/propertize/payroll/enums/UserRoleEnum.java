package com.propertize.payroll.enums;

/**
 * Enum representing user roles in the system.
 * Aligned with auth-service RBAC v5.0 role definitions.
 */
public enum UserRoleEnum {
    // Platform-level roles
    PLATFORM_OVERSIGHT("Platform Oversight", "Platform-wide oversight and approval authority"),
    PLATFORM_OPERATIONS("Platform Operations", "Platform operations and organization management"),
    PLATFORM_ENGINEERING("Platform Engineering", "System operations and infrastructure"),
    PLATFORM_ANALYTICS("Platform Analytics", "Business intelligence and cross-org reporting"),

    // Portfolio-level
    PORTFOLIO_OWNER("Portfolio Owner", "Multi-organization portfolio investor"),

    // Organization-level
    ORGANIZATION_OWNER("Organization Owner", "Single organization owner/landlord"),
    ORGANIZATION_ADMIN("Organization Admin", "Organization operations administrator"),

    // Functional specialist roles
    PROPERTY_MANAGER("Property Manager", "Property management and operations"),
    ACCOUNTANT("Accountant", "Financial management and reporting"),
    LEASING_AGENT("Leasing Agent", "Tenant acquisition and rental applications"),
    MAINTENANCE_SUPERVISOR("Maintenance Supervisor", "Maintenance operations supervisor"),
    TENANT_COORDINATOR("Tenant Coordinator", "Tenant relations and communication"),
    LEASE_SPECIALIST("Lease Specialist", "Lease agreement and contract management"),

    // Operational roles
    INSPECTOR("Inspector", "Property inspection specialist"),
    MAINTENANCE_TECHNICIAN("Maintenance Technician", "Maintenance task execution"),

    // Team roles
    TEAM_LEAD("Team Lead", "Team or department lead"),
    TEAM_MEMBER("Team Member", "General team member"),

    // External roles
    VENDOR("Vendor", "External vendor access"),

    // Resident roles
    TENANT("Tenant", "Tenant self-service portal access"),
    APPLICANT("Applicant", "Rental application submission"),

    // Special roles
    EMERGENCY_ACCESS("Emergency Access", "Emergency temporary access (break-glass)"),
    READ_ONLY("Read Only", "Read-only access for auditors or observers");

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
