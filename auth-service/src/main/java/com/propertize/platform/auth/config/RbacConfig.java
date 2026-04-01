package com.propertize.platform.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Enhanced YAML-based RBAC Configuration with inheritance, hierarchy,
 * templates, endpoints, restrictions, and capabilities support.
 *
 * This is the SINGLE SOURCE OF TRUTH for all RBAC configuration.
 * All other services must call auth-service APIs for permission checks.
 *
 * @version 6.0 - Enhanced RBAC with ABAC evaluators, field-level, time-based,
 *          data scope, conditional permissions
 */
@Configuration
@ConfigurationProperties(prefix = "rbac")
@Getter
@Setter
public class RbacConfig {

    private static final Logger log = LoggerFactory.getLogger(RbacConfig.class);

    private String version;
    private String lastUpdated;
    private CoreConfig core;
    private Map<String, PermissionTemplate> permissionTemplates;
    private Map<String, List<String>> permissionCategories;
    private Map<String, RoleConfig> roles;
    private Map<String, PermissionHierarchy> permissionHierarchy;
    private Map<String, PermissionHierarchy> permissionPatterns;
    private Map<String, ScopeDefinition> scopes;
    private Map<String, Map<String, String>> endpoints;

    // Phase 1: Time-Based Access Control
    private Map<String, TimeRestriction> timeRestrictions;

    // Phase 1: Field-Level Permissions
    private Map<String, Map<String, FieldPermission>> fieldLevelPermissions;

    // Phase 2: Data Scope Constraints (Row-Level Security)
    private Map<String, Map<String, String>> dataScopes;

    // Phase 2: Conditional Permissions (Amount/Value Limits)
    private Map<String, Map<String, Map<String, Object>>> conditionalPermissions;

    // Phase 3: Dynamic Role Composition
    private Map<String, DynamicRoleComposition> dynamicRoleComposition;

    @PostConstruct
    public void verifyConfiguration() {
        if (roles == null || roles.isEmpty()) {
            log.error("❌ CRITICAL: RBAC configuration is EMPTY! Check rbac.yml file!");
            throw new IllegalStateException("RBAC configuration not loaded! Check rbac.yml.");
        }

        if (permissionHierarchy == null && permissionPatterns != null) {
            permissionHierarchy = permissionPatterns;
            log.info("✅ Using permissionPatterns as permissionHierarchy");
        }

        log.info("✅ RBAC Configuration loaded: {} roles, version: {}", roles.size(), version);
        log.info("✅ Scopes: {}, Permission Patterns: {}",
                scopes != null ? scopes.size() : 0,
                permissionPatterns != null ? permissionPatterns.size() : 0);

        if (endpoints != null && !endpoints.isEmpty()) {
            log.info("✅ RBAC endpoints mapping loaded: {} entries", endpoints.size());
        }

        if (timeRestrictions != null && !timeRestrictions.isEmpty()) {
            log.info("✅ Time restrictions loaded: {} roles", timeRestrictions.size());
        }
        if (fieldLevelPermissions != null && !fieldLevelPermissions.isEmpty()) {
            log.info("✅ Field-level permissions loaded: {} resources", fieldLevelPermissions.size());
        }
        if (dataScopes != null && !dataScopes.isEmpty()) {
            log.info("✅ Data scopes loaded: {} roles", dataScopes.size());
        }
        if (conditionalPermissions != null && !conditionalPermissions.isEmpty()) {
            log.info("✅ Conditional permissions loaded: {} roles", conditionalPermissions.size());
        }
        if (dynamicRoleComposition != null && !dynamicRoleComposition.isEmpty()) {
            log.info("✅ Dynamic role composition loaded: {} roles", dynamicRoleComposition.size());
        }
    }

    @Data
    public static class RoleConfig {
        private String description;
        private List<String> inherits;
        private List<String> permissions;
        private List<String> explicitDenials;
        private String scope;
        private Integer level;
        private String category;
        private boolean bypassAllChecks = false;
        private RoleRestrictions restrictions;
        private Map<String, Boolean> capabilities;
        private List<String> features;
        /**
         * Advisory list of org-types this role is designed for (e.g.
         * INDIVIDUAL_PROPERTY_OWNER).
         */
        private List<String> applicableOrgTypes;
    }

    @Data
    public static class RoleRestrictions {
        private Integer maxUsers;
        private Integer maxProperties;
        private Boolean canDeleteSelf;
        private Boolean requireMfa;
        private Boolean readOnly;
        private Boolean canExport;
        private Boolean canApproveExpenses;
        private Boolean canViewOthers;
        private Integer uploadLimitMb;
    }

    @Data
    public static class PermissionHierarchy {
        private List<String> includes;
    }

    @Data
    public static class ScopeDefinition {
        private String description;
        private Integer level;
    }

    @Data
    public static class CoreConfig {
        private String defaultScope;
        private Boolean enableInheritance;
        private Boolean enableDynamicPermissions;
        private Boolean enableRoleComposition;
        private Boolean enablePermissionTemplates;
        private Boolean cachePermissions;
        private Integer cacheTTL;
        private Boolean allowRuntimeRoleCreation;
        private Integer maxCustomRolesPerClient;
    }

    @Data
    public static class PermissionTemplate {
        private List<String> permissions;
        private String description;
    }

    @Data
    public static class TimeRestriction {
        private String activeHours; // e.g., "08:00-18:00"
        private List<String> activeDays; // e.g., ["MON","TUE","WED","THU","FRI"]
        private String timezone; // e.g., "America/New_York"
    }

    @Data
    public static class FieldPermission {
        private List<String> visible;
        private List<String> hidden;
    }

    @Data
    public static class DynamicRoleComposition {
        private Map<String, Object> activateWhen;
        private List<String> additionalRoles;
        private Map<String, Object> conditions;
    }
}
