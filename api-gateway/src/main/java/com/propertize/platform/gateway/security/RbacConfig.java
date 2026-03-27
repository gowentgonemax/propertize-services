package com.propertize.platform.gateway.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

/**
 * RBAC Configuration for API Gateway
 *
 * Loads role-based access control configuration from rbac.yml.
 * This is the edge enforcement point - auth-service is the centralized RBAC
 * source.
 * Gateway uses a local copy for fast permission resolution without per-request
 * calls.
 * 
 * To sync: GET /api/v1/auth/rbac/config from auth-service.
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "security.rbac")
@Data
public class RbacConfig {

    private String configLocation = "classpath:rbac.yml";
    private int cacheTtl = 3600;

    private Map<String, RoleConfig> roles = new HashMap<>();
    private Map<String, PermissionTemplate> permissionTemplates = new HashMap<>();
    private List<EndpointMapping> endpointMappings = new ArrayList<>();

    @PostConstruct
    public void loadConfig() {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("rbac.yml");

            if (inputStream == null) {
                log.warn("rbac.yml not found, using default configuration");
                return;
            }

            Map<String, Object> config = yaml.load(inputStream);
            if (config == null) {
                return;
            }

            Map<String, Object> rbac = (Map<String, Object>) config.get("rbac");
            if (rbac == null) {
                return;
            }

            // Load permission templates
            Map<String, Object> templates = (Map<String, Object>) rbac.get("permissionTemplates");
            if (templates != null) {
                templates.forEach((name, value) -> {
                    Map<String, Object> templateData = (Map<String, Object>) value;
                    PermissionTemplate template = new PermissionTemplate();
                    template.setPermissions((List<String>) templateData.get("permissions"));
                    template.setDescription((String) templateData.get("description"));
                    permissionTemplates.put(name, template);
                });
            }

            // Load roles
            Map<String, Object> rolesConfig = (Map<String, Object>) rbac.get("roles");
            if (rolesConfig != null) {
                rolesConfig.forEach((name, value) -> {
                    Map<String, Object> roleData = (Map<String, Object>) value;
                    RoleConfig role = new RoleConfig();
                    role.setDescription((String) roleData.get("description"));
                    role.setPermissions((List<String>) roleData.get("permissions"));
                    role.setInherits((List<String>) roleData.get("inherits"));

                    // Load resource permissions
                    Map<String, Object> resourcePerms = (Map<String, Object>) roleData.get("resourcePermissions");
                    if (resourcePerms != null) {
                        Map<String, ResourcePermission> rp = new HashMap<>();
                        resourcePerms.forEach((resource, perms) -> {
                            Map<String, Object> permData = (Map<String, Object>) perms;
                            ResourcePermission resourcePerm = new ResourcePermission();
                            resourcePerm.setTemplate((String) permData.get("template"));
                            resourcePerm.setPermissions((List<String>) permData.get("permissions"));
                            rp.put(resource, resourcePerm);
                        });
                        role.setResourcePermissions(rp);
                    }

                    roles.put(name, role);
                });
            }

            // Load endpoint mappings
            Map<String, Object> mappings = (Map<String, Object>) rbac.get("endpointMappings");
            if (mappings != null) {
                // Process endpoint mappings...
            }

            log.info("✅ RBAC configuration loaded: {} roles, {} templates",
                    roles.size(), permissionTemplates.size());

            // ── Drift detection ─────────────────────────────────────────────────────
            // The gateway has its own rbac.yml copy. If someone updates the auth-service
            // rbac.yml without updating the gateway rbac.yml, authorization decisions
            // will silently diverge. This startup check logs a WARNING if the role set
            // in the gateway YAML differs from the canonical 22 built-in roles.
            // TODO: Replace this static check with an HTTP call to GET /api/v1/auth/roles
            // and compare role names + permission counts at startup.
            Set<String> canonicalRoles = Set.of(
                    "PLATFORM_OVERSIGHT", "PLATFORM_OPERATIONS", "PLATFORM_ENGINEERING", "PLATFORM_ANALYTICS",
                    "PORTFOLIO_OWNER", "ORGANIZATION_OWNER", "ORGANIZATION_ADMIN",
                    "PROPERTY_MANAGER", "ACCOUNTANT", "LEASING_AGENT", "MAINTENANCE_SUPERVISOR",
                    "TENANT_COORDINATOR", "LEASE_SPECIALIST", "INSPECTOR", "MAINTENANCE_TECHNICIAN",
                    "TEAM_LEAD", "TEAM_MEMBER", "VENDOR", "TENANT", "APPLICANT",
                    "EMERGENCY_ACCESS", "READ_ONLY");
            Set<String> missing = new java.util.HashSet<>(canonicalRoles);
            missing.removeAll(roles.keySet());
            Set<String> extra = new java.util.HashSet<>(roles.keySet());
            extra.removeAll(canonicalRoles);
            if (!missing.isEmpty()) {
                log.warn("⚠️  RBAC DRIFT DETECTED — gateway rbac.yml is missing roles: {}. " +
                        "Sync the gateway rbac.yml with the auth-service rbac.yml.", missing);
            }
            if (!extra.isEmpty()) {
                log.info("ℹ️  Gateway rbac.yml has additional custom roles not in canonical set: {}. " +
                        "Verify these are intentional custom roles.", extra);
            }

        } catch (Exception e) {
            log.error("Failed to load RBAC configuration: {}", e.getMessage(), e);
        }
    }

    /**
     * Get all permissions for a given role
     * Includes inherited permissions and expanded resource permissions
     */
    public Set<String> getPermissionsForRole(String roleName) {
        RoleConfig roleConfig = roles.get(roleName);
        if (roleConfig == null) {
            return Collections.emptySet();
        }

        Set<String> permissions = new LinkedHashSet<>();

        // Add direct permissions
        if (roleConfig.getPermissions() != null) {
            permissions.addAll(roleConfig.getPermissions());
        }

        // Add resource-based permissions
        if (roleConfig.getResourcePermissions() != null) {
            roleConfig.getResourcePermissions().forEach((resource, resourcePerm) -> {
                if (resourcePerm.getTemplate() != null) {
                    PermissionTemplate template = permissionTemplates.get(resourcePerm.getTemplate());
                    if (template != null && template.getPermissions() != null) {
                        template.getPermissions().forEach(action -> permissions.add(resource + ":" + action));
                    }
                }
                if (resourcePerm.getPermissions() != null) {
                    resourcePerm.getPermissions().forEach(action -> permissions.add(resource + ":" + action));
                }
            });
        }

        // Add inherited permissions
        if (roleConfig.getInherits() != null) {
            roleConfig.getInherits().forEach(inheritedRole -> permissions.addAll(getPermissionsForRole(inheritedRole)));
        }

        // Handle wildcard
        if (permissions.contains("*")) {
            Set<String> allPermissions = new LinkedHashSet<>();
            roles.values().forEach(config -> {
                if (config.getPermissions() != null) {
                    allPermissions.addAll(config.getPermissions());
                }
            });
            permissions.addAll(allPermissions);
            permissions.remove("*");
        }

        return permissions;
    }

    /**
     * Get all configured role names
     */
    public Set<String> getAllRoles() {
        return roles.keySet();
    }

    @Data
    public static class RoleConfig {
        private String description;
        private List<String> permissions;
        private List<String> inherits;
        private Map<String, ResourcePermission> resourcePermissions;
    }

    @Data
    public static class ResourcePermission {
        private String template;
        private List<String> permissions;
    }

    @Data
    public static class PermissionTemplate {
        private List<String> permissions;
        private String description;
    }

    @Data
    public static class EndpointMapping {
        private String pattern;
        private String method;
        private String permission;
    }
}
