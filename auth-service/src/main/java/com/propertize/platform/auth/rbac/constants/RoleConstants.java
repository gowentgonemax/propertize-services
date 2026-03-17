package com.propertize.platform.auth.rbac.constants;

/**
 * Role Constants - RBAC v5.0
 * Centralized role names matching rbac.yml configuration.
 * Single source of truth for all services.
 *
 * @version 5.0 - Centralized in auth-service
 */
public final class RoleConstants {

    private RoleConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // PLATFORM ROLES (Level 950-1000)
    public static final String PLATFORM_OVERSIGHT = "PLATFORM_OVERSIGHT";
    public static final String PLATFORM_OPERATIONS = "PLATFORM_OPERATIONS";
    public static final String PLATFORM_ENGINEERING = "PLATFORM_ENGINEERING";
    public static final String PLATFORM_ANALYTICS = "PLATFORM_ANALYTICS";

    // ORGANIZATION ROLES (Level 800-900)
    public static final String ORGANIZATION_OWNER = "ORGANIZATION_OWNER";
    public static final String ORGANIZATION_ADMIN = "ORGANIZATION_ADMIN";
    public static final String PORTFOLIO_OWNER = "PORTFOLIO_OWNER";

    // PROPERTY MANAGEMENT ROLES (Level 600-700)
    public static final String PROPERTY_MANAGER = "PROPERTY_MANAGER";
    public static final String PROPERTY_ACCOUNTANT = "PROPERTY_ACCOUNTANT";
    public static final String ASSISTANT_PROPERTY_MANAGER = "ASSISTANT_PROPERTY_MANAGER";

    // LEASING ROLES (Level 500-600)
    public static final String LEASING_AGENT = "LEASING_AGENT";
    public static final String LEASE_SPECIALIST = "LEASE_SPECIALIST";
    public static final String LEASING_COORDINATOR = "LEASING_COORDINATOR";
    public static final String TENANT_COORDINATOR = "TENANT_COORDINATOR";

    // MAINTENANCE ROLES (Level 400-500)
    public static final String MAINTENANCE_SUPERVISOR = "MAINTENANCE_SUPERVISOR";
    public static final String MAINTENANCE_TECHNICIAN = "MAINTENANCE_TECHNICIAN";
    public static final String MAINTENANCE_COORDINATOR = "MAINTENANCE_COORDINATOR";
    public static final String INSPECTOR = "INSPECTOR";

    // FINANCIAL ROLES (Level 550-650)
    public static final String ACCOUNTS_PAYABLE_MANAGER = "ACCOUNTS_PAYABLE_MANAGER";
    public static final String ACCOUNTS_RECEIVABLE_MANAGER = "ACCOUNTS_RECEIVABLE_MANAGER";
    public static final String ACCOUNTANT = "ACCOUNTANT";
    public static final String FINANCIAL_ANALYST = "FINANCIAL_ANALYST";

    // OPERATIONAL ROLES (Level 300-400)
    public static final String OPERATIONS_MANAGER = "OPERATIONS_MANAGER";
    public static final String OPERATIONS_COORDINATOR = "OPERATIONS_COORDINATOR";
    public static final String COMPLIANCE_OFFICER = "COMPLIANCE_OFFICER";
    public static final String COMPLIANCE_SPECIALIST = "COMPLIANCE_SPECIALIST";

    // SUPPORT ROLES (Level 200-300)
    public static final String CUSTOMER_SUPPORT_MANAGER = "CUSTOMER_SUPPORT_MANAGER";
    public static final String CUSTOMER_SUPPORT_AGENT = "CUSTOMER_SUPPORT_AGENT";
    public static final String ADMINISTRATIVE_ASSISTANT = "ADMINISTRATIVE_ASSISTANT";
    public static final String RECEPTIONIST = "RECEPTIONIST";
    public static final String READ_ONLY = "READ_ONLY";

    // TEAM ROLES
    public static final String TEAM_LEAD = "TEAM_LEAD";
    public static final String TEAM_MEMBER = "TEAM_MEMBER";

    // TENANT ROLES (Level 50-100)
    public static final String TENANT = "TENANT";
    public static final String GUEST_TENANT = "GUEST_TENANT";

    // VENDOR ROLES (Level 50-100)
    public static final String VENDOR = "VENDOR";
    public static final String VENDOR_PARTNER = "VENDOR_PARTNER";
    public static final String GUEST_VENDOR = "GUEST_VENDOR";

    // APPLICANT / SPECIAL
    public static final String APPLICANT = "APPLICANT";
    public static final String EMERGENCY_ACCESS = "EMERGENCY_ACCESS";

    // ROLE GROUPS
    public static final String[] PLATFORM_ROLES = {
            PLATFORM_OVERSIGHT, PLATFORM_OPERATIONS, PLATFORM_ENGINEERING, PLATFORM_ANALYTICS
    };

    public static final String[] ORGANIZATION_ROLES = {
            ORGANIZATION_OWNER, ORGANIZATION_ADMIN, PORTFOLIO_OWNER
    };

    public static final String[] PROPERTY_MANAGEMENT_ROLES = {
            PROPERTY_MANAGER, PROPERTY_ACCOUNTANT, ASSISTANT_PROPERTY_MANAGER
    };

    public static final String[] FINANCIAL_ROLES = {
            ACCOUNTS_PAYABLE_MANAGER, ACCOUNTS_RECEIVABLE_MANAGER, ACCOUNTANT, FINANCIAL_ANALYST, PROPERTY_ACCOUNTANT
    };
}
