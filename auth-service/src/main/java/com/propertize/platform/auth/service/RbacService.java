package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.entity.RbacRole;
import com.propertize.platform.auth.repository.RbacRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized RBAC Service for permission management.
 * This is the SINGLE SOURCE OF TRUTH for all permission resolution.
 *
 * @version 5.0 - Centralized in auth-service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacService {

    private final RbacConfig rbacConfig;

    /**
     * Injected after construction (optional to avoid circular dep during startup).
     * Used to check system roles from the DB-backed catalog.
     */
    @Autowired(required = false)
    private RbacRoleRepository rbacRoleRepository;

    /**
     * Get BASE permissions for a role (only those defined in rbac.yml, no
     * expansion).
     * Use this for JWT token storage to keep token size small.
     */
    public Set<String> getBasePermissionsForRole(String role) {
        if (rbacConfig.getRoles() == null) {
            log.warn("⚠️ RBAC roles is NULL for role: {}", role);
            return Collections.emptySet();
        }

        RbacConfig.RoleConfig roleConfig = rbacConfig.getRoles().get(role);
        if (roleConfig == null) {
            log.warn("⚠️ RoleConfig not found for role: {}", role);
            return Collections.emptySet();
        }

        Set<String> permissions = new LinkedHashSet<>();
        if (roleConfig.getPermissions() != null) {
            permissions.addAll(roleConfig.getPermissions().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }

        log.debug("🔍 Role {} has {} base permissions (no expansion)", role, permissions.size());

        // Include inherited role base permissions
        if (roleConfig.getInherits() != null && !roleConfig.getInherits().isEmpty()) {
            roleConfig.getInherits().stream()
                    .filter(Objects::nonNull)
                    .forEach(inheritedRole -> permissions.addAll(getBasePermissionsForRole(inheritedRole)));
        }

        log.info("✅ Returning {} base permissions for role {} (for JWT storage)", permissions.size(), role);
        return Collections.unmodifiableSet(permissions);
    }

    /**
     * Collects explicit denials across a set of role names.
     * Permissions in this set MUST be removed from the final JWT permission set,
     * even if granted via inheritance or custom roles.
     *
     * @param roles the role names assigned to the user
     * @return union of all explicitDenials for the given roles (from rbac.yml)
     */
    public Set<String> getExplicitDenialsForRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty() || rbacConfig.getRoles() == null) {
            return Collections.emptySet();
        }
        Set<String> denied = new LinkedHashSet<>();
        for (String role : roles) {
            RbacConfig.RoleConfig cfg = rbacConfig.getRoles().get(role);
            if (cfg != null && cfg.getExplicitDenials() != null) {
                cfg.getExplicitDenials().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(denied::add);
            }
        }
        return Collections.unmodifiableSet(denied);
    }

    /**
     * Get all permissions for a role including inheritance and wildcard expansion.
     * This expands permission hierarchies for actual permission checks.
     */
    public Set<String> getPermissionsForRole(String role) {
        if (rbacConfig.getRoles() == null) {
            log.warn("⚠️ RBAC roles is NULL for role: {}", role);
            return Collections.emptySet();
        }

        RbacConfig.RoleConfig roleConfig = rbacConfig.getRoles().get(role);
        if (roleConfig == null) {
            log.warn("⚠️ RoleConfig not found for role: {}", role);
            return Collections.emptySet();
        }

        Set<String> permissions = new LinkedHashSet<>();
        if (roleConfig.getPermissions() != null) {
            permissions.addAll(roleConfig.getPermissions().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }

        log.debug("🔍 Role {} has {} direct permissions", role, permissions.size());

        // Inherited roles
        if (roleConfig.getInherits() != null && !roleConfig.getInherits().isEmpty()) {
            roleConfig.getInherits().stream()
                    .filter(Objects::nonNull)
                    .forEach(inheritedRole -> permissions.addAll(getPermissionsForRole(inheritedRole)));
        }

        // Wildcard expansion
        if (permissions.contains("*")) {
            Set<String> all = new LinkedHashSet<>();
            rbacConfig.getRoles().values().forEach(config -> {
                if (config.getPermissions() != null) {
                    config.getPermissions().stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .forEach(all::add);
                }
            });
            permissions.addAll(all);
            permissions.remove("*");
            log.debug("🔍 Wildcard expanded to {} permissions for role {}", permissions.size(), role);
        }

        log.info("✅ Returning {} total permissions for role {}", permissions.size(), role);
        Set<String> expanded = expandPermissions(permissions);
        return Collections.unmodifiableSet(toCanonicalAuthorities(expanded));
    }

    private Set<String> toCanonicalAuthorities(Set<String> expanded) {
        Set<String> normalized = new LinkedHashSet<>();
        expanded.stream()
                .filter(p -> p != null && !p.isBlank())
                .forEach(p -> {
                    String trimmed = p.trim();
                    normalized.add(trimmed);
                    String canon = trimmed.replaceAll("[:.\\-]", "_").toUpperCase(Locale.ROOT);
                    normalized.add(canon);
                });
        return normalized;
    }

    private Set<String> expandPermissions(Set<String> permissions) {
        Set<String> expanded = new LinkedHashSet<>();
        if (permissions == null || permissions.isEmpty())
            return expanded;

        permissions.forEach(permission -> {
            expanded.add(permission);
            if (rbacConfig.getPermissionHierarchy() != null) {
                RbacConfig.PermissionHierarchy hierarchy = rbacConfig.getPermissionHierarchy().get(permission);
                if (hierarchy != null && hierarchy.getIncludes() != null) {
                    hierarchy.getIncludes().stream()
                            .filter(inc -> inc != null && !inc.isBlank())
                            .forEach(expanded::add);
                }
            }
        });

        return expanded;
    }

    public Set<String> getAllRoles() {
        if (rbacConfig.getRoles() == null)
            return Collections.emptySet();
        return rbacConfig.getRoles().keySet();
    }

    /**
     * Returns all system roles from the DB catalog ({@code rbac_roles} where
     * {@code is_system=true}), falling back to YAML keys if the repository is
     * not yet available.
     */
    public List<RbacRole> getSystemRolesFromDb() {
        if (rbacRoleRepository == null)
            return Collections.emptyList();
        return rbacRoleRepository.findByIsSystemTrueAndIsActiveTrue();
    }

    /**
     * Check if a specific role has a permission.
     */
    public boolean hasPermission(String role, String permission) {
        if (permission == null)
            return false;
        Set<String> perms = getPermissionsForRole(role);
        return perms.contains(permission) || perms.contains(normalizePermission(permission));
    }

    /**
     * Check if current authenticated user has a permission.
     */
    public boolean hasPermission(String permission) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated())
                return false;

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            if (authorities == null || authorities.isEmpty())
                return false;

            String normalized = normalizePermission(permission);
            return authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(auth -> permission.equals(auth) || normalized.equals(auth));
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizePermission(String p) {
        if (p == null)
            return null;
        return p.trim().replaceAll("[:.\\-]", "_").toUpperCase(Locale.ROOT);
    }

    public String getScopeForRole(String role) {
        if (rbacConfig.getRoles() == null)
            return null;
        RbacConfig.RoleConfig roleConfig = rbacConfig.getRoles().get(role);
        return roleConfig != null ? roleConfig.getScope() : null;
    }

    /**
     * Convert role to Spring Security authorities.
     */
    public Collection<GrantedAuthority> getAuthoritiesForRole(String role, String organizationId) {
        Set<String> permissions = getPermissionsForRole(role);
        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

        permissions.stream()
                .filter(permission -> permission != null && !permission.isBlank())
                .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

        String scope = getScopeForRole(role);
        if ("client".equals(scope) && organizationId != null && !organizationId.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ORG_" + organizationId));
        }

        return Collections.unmodifiableList(authorities);
    }

    /**
     * Get roles that have a specific permission.
     */
    public Set<String> getRolesWithPermission(String permission) {
        String norm = normalizePermission(permission);
        Set<String> result = new LinkedHashSet<>();
        if (rbacConfig.getRoles() == null)
            return result;

        rbacConfig.getRoles().entrySet().stream()
                .filter(entry -> {
                    List<String> perms = entry.getValue().getPermissions();
                    return perms != null && perms.stream().anyMatch(p -> p != null
                            && (Objects.equals(p, permission) || Objects.equals(normalizePermission(p), norm)));
                })
                .forEach(entry -> result.add(entry.getKey()));
        return result;
    }

    /**
     * Check if a role has bypass_all_checks enabled (PLATFORM_OVERSIGHT).
     */
    public boolean hasBypassAllChecks(String role) {
        RbacConfig.RoleConfig roleConfig = rbacConfig.getRoles() != null ? rbacConfig.getRoles().get(role) : null;
        return roleConfig != null && roleConfig.isBypassAllChecks();
    }

    public boolean isPlatformAdmin(Collection<String> roles) {
        return roles != null && roles.contains("PLATFORM_OVERSIGHT");
    }

    public boolean shouldBypassAllChecks(Collection<String> roles) {
        if (roles == null || roles.isEmpty())
            return false;
        return roles.stream().anyMatch(this::hasBypassAllChecks);
    }

    /**
     * Check if current user has a specific role.
     */
    public boolean hasRole(String roleName) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated())
                return false;

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            if (authorities == null || authorities.isEmpty())
                return false;

            String roleWithPrefix = "ROLE_" + roleName;
            return authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(auth -> roleName.equals(auth) || roleWithPrefix.equals(auth));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if current user has any of the specified roles.
     */
    public boolean hasAnyRole(String... roleNames) {
        if (roleNames == null || roleNames.length == 0)
            return false;

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated())
                return false;

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            if (authorities == null || authorities.isEmpty())
                return false;

            Set<String> userAuthorities = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            return Arrays.stream(roleNames)
                    .anyMatch(role -> userAuthorities.contains(role) ||
                            userAuthorities.contains("ROLE_" + role));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSuperAdmin() {
        return hasRole("PLATFORM_OVERSIGHT");
    }

    /**
     * Get endpoint permission mapping from rbac.yml.
     */
    public Map<String, Map<String, String>> getEndpointPermissions() {
        if (rbacConfig.getEndpoints() == null)
            return Collections.emptyMap();
        return rbacConfig.getEndpoints();
    }

    /**
     * Get role configuration details.
     */
    public RbacConfig.RoleConfig getRoleConfig(String role) {
        if (rbacConfig.getRoles() == null)
            return null;
        return rbacConfig.getRoles().get(role);
    }

    /**
     * Get the RBAC config version.
     */
    public String getConfigVersion() {
        return rbacConfig.getVersion();
    }

    public static String formatAccessDenied(String username, String method, String uri, String missingPermission) {
        StringBuilder sb = new StringBuilder();
        sb.append("user=").append(username == null || username.isBlank() ? "<anonymous>" : username);
        sb.append(" method=").append(method == null ? "<unknown>" : method);
        sb.append(" uri=").append(uri == null ? "<unknown>" : uri);
        if (missingPermission != null && !missingPermission.isBlank()) {
            sb.append(" missingPermission=").append(missingPermission);
        }
        return sb.toString();
    }
}
