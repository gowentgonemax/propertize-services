package com.propertize.commons.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Canonical enum representing user roles with hierarchical permissions.
 * Single source of truth — all services import from propertize-commons.
 * Synced with RBAC v7.0-restructured (rbac.yml).
 *
 * <p>
 * Format: ROLE_NAME("Display Name", "Description", privilege_level,
 * "AUTHORITY_STRING")
 * </p>
 *
 * @since 1.0.0
 */
public enum UserRoleEnum {
    // ============ PLATFORM LEVEL (1000-930) ============
    PLATFORM_OVERSIGHT("Platform Oversight", "Platform-wide oversight and approval authority", 1000,
            "ROLE_PLATFORM_OVERSIGHT"),
    PLATFORM_OPERATIONS("Platform Operations", "Platform operations and organization management", 970,
            "ROLE_PLATFORM_OPERATIONS"),
    PLATFORM_ENGINEERING("Platform Engineering", "System operations and infrastructure", 950,
            "ROLE_PLATFORM_ENGINEERING"),
    PLATFORM_ANALYTICS("Platform Analytics", "Business intelligence and cross-org reporting", 930,
            "ROLE_PLATFORM_ANALYTICS"),

    // ============ PORTFOLIO LEVEL (920) ============
    PORTFOLIO_OWNER("Portfolio Owner", "Multi-organization portfolio investor", 920, "ROLE_PORTFOLIO_OWNER"),

    // ============ ORGANIZATION LEVEL (900-850) ============
    ORGANIZATION_OWNER("Organization Owner", "Single organization owner/landlord", 900, "ROLE_ORGANIZATION_OWNER"),
    HOA_DIRECTOR("HOA Director", "HOA board chair / director — governance, finance, and community management", 890,
            "ROLE_HOA_DIRECTOR"),
    CFO("CFO", "Chief Financial Officer — full financial control, payroll approval", 890, "ROLE_CFO"),
    SOLO_OWNER("Solo Owner", "Simplified owner role for individual landlords (1-3 properties, no staff)", 870,
            "ROLE_SOLO_OWNER"),
    ORGANIZATION_ADMIN("Organization Admin", "Organization operations administrator", 850, "ROLE_ORGANIZATION_ADMIN"),
    HR_MANAGER("HR Manager", "Human Resources manager — employee lifecycle and payroll management", 820,
            "ROLE_HR_MANAGER"),

    // ============ FUNCTIONAL SPECIALIST ROLES (800-550) ============
    PROPERTY_MANAGER("Property Manager", "Property management and operations", 800, "ROLE_PROPERTY_MANAGER"),
    OPERATIONS_MANAGER("Operations Manager", "Property and maintenance operations", 780, "ROLE_OPERATIONS_MANAGER"),
    LEASE_SPECIALIST("Lease Specialist", "Lease agreement and contract management", 750, "ROLE_LEASE_SPECIALIST"),
    PORTFOLIO_ANALYST("Portfolio Analyst", "Investment portfolio analyst — analytics, ROI, market analysis", 750,
            "ROLE_PORTFOLIO_ANALYST"),
    INVESTOR_RELATIONS("Investor Relations", "Investor relations manager — owner/investor reporting and communication",
            730, "ROLE_INVESTOR_RELATIONS"),
    LEASING_AGENT("Leasing Agent", "Tenant acquisition and rental applications", 700, "ROLE_LEASING_AGENT"),
    OWNER_RELATIONS("Owner Relations", "Owner relations specialist — manages owner portal and property owner reporting",
            700, "ROLE_OWNER_RELATIONS"),
    ACCOUNTANT("Accountant", "Financial management and reporting", 650, "ROLE_ACCOUNTANT"),
    COMMUNITY_MANAGER("Community Manager", "Community / social housing manager — resident welfare and social programs",
            650, "ROLE_COMMUNITY_MANAGER"),
    MAINTENANCE_SUPERVISOR("Maintenance Supervisor", "Maintenance operations supervisor", 600,
            "ROLE_MAINTENANCE_SUPERVISOR"),
    BOARD_MEMBER("Board Member", "HOA board member — governance read-access and voting", 600, "ROLE_BOARD_MEMBER"),
    TENANT_COORDINATOR("Tenant Coordinator", "Tenant relations and communication", 550, "ROLE_TENANT_COORDINATOR"),
    CASE_WORKER("Case Worker", "Case worker — manages individual resident support cases", 550, "ROLE_CASE_WORKER"),

    // ============ TEAM ROLES (500-300) ============
    TEAM_LEAD("Team Lead", "Team or department lead", 500, "ROLE_TEAM_LEAD"),

    // ============ OPERATIONAL ROLES (450-400) ============
    INSPECTOR("Inspector", "Property inspection specialist", 450, "ROLE_INSPECTOR"),
    MAINTENANCE_TECHNICIAN("Maintenance Technician", "Maintenance task execution", 400, "ROLE_MAINTENANCE_TECHNICIAN"),

    // ============ TEAM MEMBER (300) ============
    TEAM_MEMBER("Team Member", "General team member", 300, "ROLE_TEAM_MEMBER"),

    // ============ EXTERNAL ROLES (200) ============
    VENDOR("Vendor", "External vendor access", 200, "ROLE_VENDOR"),

    // ============ RESIDENT ROLES (150-100) ============
    TENANT("Tenant", "Tenant self-service portal access", 150, "ROLE_TENANT"),
    APPLICANT("Applicant", "Rental application submission", 100, "ROLE_APPLICANT"),

    // ============ SPECIAL ROLES (999/50) ============
    EMERGENCY_ACCESS("Emergency Access", "Emergency temporary access (break-glass)", 999, "ROLE_EMERGENCY_ACCESS"),
    READ_ONLY("Read Only", "Read-only access for auditors or observers", 50, "ROLE_READ_ONLY");

    /** Threshold level at or above which a role is considered platform-level. */
    public static final int PLATFORM_LEVEL_THRESHOLD = 930;

    private static final Map<String, UserRoleEnum> AUTH_MAP = Collections.unmodifiableMap(Arrays.stream(values())
            .collect(Collectors.toMap(UserRoleEnum::getAuthority, r -> r)));

    private static final Map<String, UserRoleEnum> NAME_MAP = Collections.unmodifiableMap(Arrays.stream(values())
            .collect(Collectors.toMap(Enum::name, r -> r)));

    private final String displayName;
    private final String description;
    private final int level;
    private final String authority;

    UserRoleEnum(String displayName, String description, int level, String authority) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
        this.authority = authority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    public String getAuthority() {
        return authority;
    }

    /**
     * Looks up a role by its Spring Security authority string (e.g.
     * "ROLE_PLATFORM_OVERSIGHT").
     *
     * @param authority the authority string
     * @return the matching role, or {@code null} if not found
     */
    public static UserRoleEnum fromAuthority(String authority) {
        if (authority == null)
            return null;
        return AUTH_MAP.get(authority);
    }

    /**
     * Case-insensitive lookup by enum name (spaces converted to underscores).
     *
     * @param name the role name
     * @return the matching role, or {@code null} if not found
     */
    public static UserRoleEnum fromNameIgnoreCase(String name) {
        if (name == null || name.trim().isEmpty())
            return null;
        try {
            return UserRoleEnum.valueOf(name.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Returns {@code true} if this role is platform-level, APPLICANT, or
     * EMERGENCY_ACCESS.
     */
    public boolean isPlatformRole() {
        return this.level >= PLATFORM_LEVEL_THRESHOLD || this == APPLICANT || this == EMERGENCY_ACCESS;
    }

    /** Returns {@code true} if any role in the set is a platform role. */
    public static boolean containsPlatformRole(java.util.Set<UserRoleEnum> roles) {
        if (roles == null || roles.isEmpty())
            return false;
        return roles.stream().anyMatch(UserRoleEnum::isPlatformRole);
    }

    /**
     * Returns {@code true} if this role is organization-level (850 ≤ level < 950).
     */
    public boolean isOrganizationRole() {
        return level >= 850 && level < 950;
    }

    /** Returns {@code true} if this role is administrative (level ≥ 640). */
    public boolean isAdministrative() {
        return level >= 640;
    }

    /**
     * Maps this role to its RBAC template name in rbac.yml.
     *
     * @return the RBAC template name
     */
    public String getRbacTemplateName() {
        return switch (this) {
            case PLATFORM_OVERSIGHT -> "PLATFORM_OVERSIGHT";
            case PLATFORM_OPERATIONS -> "PLATFORM_OPERATIONS";
            case PLATFORM_ENGINEERING -> "PLATFORM_ENGINEERING";
            case PLATFORM_ANALYTICS -> "PLATFORM_ANALYTICS";
            case PORTFOLIO_OWNER -> "PORTFOLIO_OWNER";
            case ORGANIZATION_OWNER -> "ORGANIZATION_OWNER";
            case HOA_DIRECTOR -> "HOA_DIRECTOR";
            case CFO -> "CFO";
            case SOLO_OWNER -> "SOLO_OWNER";
            case ORGANIZATION_ADMIN -> "ORGANIZATION_ADMIN";
            case HR_MANAGER -> "HR_MANAGER";
            case PROPERTY_MANAGER -> "PROPERTY_MANAGER";
            case OPERATIONS_MANAGER -> "OPERATIONS_MANAGER";
            case LEASE_SPECIALIST -> "LEASE_SPECIALIST";
            case PORTFOLIO_ANALYST -> "PORTFOLIO_ANALYST";
            case INVESTOR_RELATIONS -> "INVESTOR_RELATIONS";
            case LEASING_AGENT -> "LEASING_AGENT";
            case OWNER_RELATIONS -> "OWNER_RELATIONS";
            case ACCOUNTANT -> "ACCOUNTANT";
            case COMMUNITY_MANAGER -> "COMMUNITY_MANAGER";
            case MAINTENANCE_SUPERVISOR -> "MAINTENANCE_SUPERVISOR";
            case BOARD_MEMBER -> "BOARD_MEMBER";
            case TENANT_COORDINATOR -> "TENANT_COORDINATOR";
            case CASE_WORKER -> "CASE_WORKER";
            case TEAM_LEAD -> "TEAM_LEAD";
            case INSPECTOR -> "INSPECTOR";
            case MAINTENANCE_TECHNICIAN -> "MAINTENANCE_TECHNICIAN";
            case TEAM_MEMBER -> "TEAM_MEMBER";
            case VENDOR -> "VENDOR";
            case TENANT -> "TENANT";
            case APPLICANT -> "APPLICANT";
            case EMERGENCY_ACCESS -> "EMERGENCY_ACCESS";
            case READ_ONLY -> "READ_ONLY";
        };
    }

    /**
     * Determines whether a user with this role may create a role at the given
     * privilege level.
     *
     * @param targetLevel the privilege level of the role to be created
     * @return {@code true} if creation is allowed
     */
    public boolean canCreateRoleAtLevel(int targetLevel) {
        if (this == PLATFORM_OVERSIGHT)
            return targetLevel >= 50 && targetLevel <= 950;
        else if (this == PLATFORM_OPERATIONS)
            return targetLevel >= 50 && targetLevel <= 929;
        else if (this == PORTFOLIO_OWNER)
            return targetLevel >= 50 && targetLevel <= 919;
        else if (this == ORGANIZATION_OWNER)
            return targetLevel >= 50 && targetLevel <= 899;
        else if (this == ORGANIZATION_ADMIN)
            return targetLevel >= 50 && targetLevel <= 849;
        return false;
    }

    /**
     * Tolerant deserialization — accepts authority string, enum name, or display
     * name.
     *
     * @param key the input string
     * @return the matching role, or {@code null} if no match
     */
    @JsonCreator
    public static UserRoleEnum fromString(String key) {
        if (key == null)
            return null;
        String k = key.trim();
        if (k.isEmpty())
            return null;

        UserRoleEnum byAuth = AUTH_MAP.get(k);
        if (byAuth != null)
            return byAuth;
        UserRoleEnum byAuthUpper = AUTH_MAP.get(k.toUpperCase());
        if (byAuthUpper != null)
            return byAuthUpper;

        UserRoleEnum byName = NAME_MAP.get(k.toUpperCase());
        if (byName != null)
            return byName;

        try {
            String normalized = k.toUpperCase().replace('-', '_').replace(' ', '_');
            return UserRoleEnum.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
        }

        String upper = k.toUpperCase();
        if (upper.startsWith("ROLE_")) {
            String candidate = upper.substring(5);
            UserRoleEnum fromNameMap = NAME_MAP.get(candidate);
            if (fromNameMap != null)
                return fromNameMap;
            try {
                return UserRoleEnum.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (UserRoleEnum r : values()) {
            if (r.displayName.equalsIgnoreCase(k) || r.description.equalsIgnoreCase(k))
                return r;
            if (r.displayName.trim().equalsIgnoreCase(k.trim()))
                return r;
        }

        return null;
    }

    /**
     * Returns all roles sorted by descending privilege level.
     *
     * @return sorted array of roles
     */
    public static UserRoleEnum[] byPrivilegeDesc() {
        return Arrays.stream(values())
                .sorted((a, b) -> Integer.compare(b.level, a.level))
                .toArray(UserRoleEnum[]::new);
    }

    /**
     * Converts this role to a Spring Security {@link GrantedAuthority}.
     *
     * @return the granted authority
     */
    public GrantedAuthority toGrantedAuthority() {
        return new SimpleGrantedAuthority(authority);
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    /**
     * Returns {@code true} if this role has a strictly higher privilege level than
     * {@code other}.
     */
    public boolean hasHigherPrivilegeThan(UserRoleEnum other) {
        return this.level > other.level;
    }

    /**
     * Returns {@code true} if this role has an equal or higher privilege level than
     * {@code other}.
     */
    public boolean hasEqualOrHigherPrivilegeThan(UserRoleEnum other) {
        return this.level >= other.level;
    }
}
