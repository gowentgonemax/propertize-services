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
 * @version 5.0 - Centralized RBAC
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
}
