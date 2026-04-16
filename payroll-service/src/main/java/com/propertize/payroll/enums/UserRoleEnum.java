package com.propertize.payroll.enums;

/**
 * Enum representing user roles in the system.
 * Aligned with RBAC v7.0-restructured (rbac.yml) role definitions.
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
    HOA_DIRECTOR("HOA Director", "HOA board chair / director — governance, finance, and community management"),
    CFO("CFO", "Chief Financial Officer — full financial control, payroll approval"),
    SOLO_OWNER("Solo Owner", "Simplified owner role for individual landlords (1-3 properties, no staff)"),
    ORGANIZATION_ADMIN("Organization Admin", "Organization operations administrator"),
    HR_MANAGER("HR Manager", "Human Resources manager — employee lifecycle and payroll management"),

    // Functional specialist roles
    PROPERTY_MANAGER("Property Manager", "Property management and operations"),
    OPERATIONS_MANAGER("Operations Manager", "Property and maintenance operations"),
    LEASE_SPECIALIST("Lease Specialist", "Lease agreement and contract management"),
    PORTFOLIO_ANALYST("Portfolio Analyst", "Investment portfolio analyst — analytics, ROI, market analysis"),
    INVESTOR_RELATIONS("Investor Relations", "Investor relations manager — owner/investor reporting and communication"),
    LEASING_AGENT("Leasing Agent", "Tenant acquisition and rental applications"),
    OWNER_RELATIONS("Owner Relations",
            "Owner relations specialist — manages owner portal and property owner reporting"),
    ACCOUNTANT("Accountant", "Financial management and reporting"),
    COMMUNITY_MANAGER("Community Manager", "Community / social housing manager — resident welfare and social programs"),
    MAINTENANCE_SUPERVISOR("Maintenance Supervisor", "Maintenance operations supervisor"),
    BOARD_MEMBER("Board Member", "HOA board member — governance read-access and voting"),
    TENANT_COORDINATOR("Tenant Coordinator", "Tenant relations and communication"),
    CASE_WORKER("Case Worker", "Case worker — manages individual resident support cases"),

    // Team roles
    TEAM_LEAD("Team Lead", "Team or department lead"),

    // Operational roles
    INSPECTOR("Inspector", "Property inspection specialist"),
    MAINTENANCE_TECHNICIAN("Maintenance Technician", "Maintenance task execution"),

    // Team member
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
