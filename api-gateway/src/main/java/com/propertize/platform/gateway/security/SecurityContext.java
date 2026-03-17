package com.propertize.platform.gateway.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

/**
 * Unified Security Context
 *
 * This class represents the authenticated user's security context
 * that is propagated from the API Gateway to all downstream services.
 *
 * All microservices (Propertize, Employecraft, Wagecraft) should use this
 * class to access user information instead of directly parsing JWT tokens.
 *
 * Thread Safety:
 * - This class is immutable once created
 * - Use the builder pattern to create instances
 * - ThreadLocal should be used in downstream services if needed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityContext {

    private String userId;
    private String username;
    private String email;
    private String organizationId;
    private String organizationCode;
    private Set<String> roles;
    private String primaryRole;
    private String correlationId;
    private Set<String> permissions;
    private boolean isAuthenticated;
    private long authenticatedAt;
    private String tokenType;

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has a specific permission
     */
    public boolean hasPermission(String permission) {
        if (permissions != null && permissions.contains(permission)) {
            return true;
        }
        // Check for wildcard permission
        if (permission != null && permission.contains(":")) {
            String resource = permission.split(":")[0];
            return permissions != null && permissions.contains(resource + ":*");
        }
        return false;
    }

    /**
     * Check if user is platform administrator
     */
    public boolean isPlatformAdmin() {
        return hasRole("PLATFORM_OVERSIGHT") ||
               hasRole("PLATFORM_ADMIN") ||
               hasRole("SUPER_ADMIN");
    }

    /**
     * Check if user is organization owner
     */
    public boolean isOrganizationOwner() {
        return hasRole("ORGANIZATION_OWNER");
    }

    /**
     * Check if user is organization manager
     */
    public boolean isOrganizationManager() {
        return hasRole("ORGANIZATION_MANAGER") || isOrganizationOwner();
    }

    /**
     * Check if user is a tenant
     */
    public boolean isTenant() {
        return hasRole("TENANT");
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String... rolesToCheck) {
        if (roles == null) return false;
        for (String role : rolesToCheck) {
            if (roles.contains(role)) return true;
        }
        return false;
    }

    /**
     * Check if user has all of the specified roles
     */
    public boolean hasAllRoles(String... rolesToCheck) {
        if (roles == null) return false;
        for (String role : rolesToCheck) {
            if (!roles.contains(role)) return false;
        }
        return true;
    }

    /**
     * Check if user has any of the specified permissions
     */
    public boolean hasAnyPermission(String... permissionsToCheck) {
        for (String permission : permissionsToCheck) {
            if (hasPermission(permission)) return true;
        }
        return false;
    }

    /**
     * Check if user has all of the specified permissions
     */
    public boolean hasAllPermissions(String... permissionsToCheck) {
        for (String permission : permissionsToCheck) {
            if (!hasPermission(permission)) return false;
        }
        return true;
    }

    /**
     * Check if user belongs to the specified organization
     */
    public boolean belongsToOrganization(String orgId) {
        return organizationId != null && organizationId.equals(orgId);
    }

    /**
     * Create an anonymous (unauthenticated) context
     */
    public static SecurityContext anonymous() {
        return SecurityContext.builder()
            .userId("anonymous")
            .username("anonymous")
            .roles(Collections.emptySet())
            .permissions(Collections.emptySet())
            .isAuthenticated(false)
            .build();
    }

    /**
     * Create a system context for internal operations
     */
    public static SecurityContext system() {
        return SecurityContext.builder()
            .userId("system")
            .username("system")
            .roles(Set.of("SYSTEM"))
            .permissions(Set.of("*"))
            .isAuthenticated(true)
            .authenticatedAt(System.currentTimeMillis())
            .build();
    }

    @Override
    public String toString() {
        return "SecurityContext{" +
            "userId='" + userId + '\'' +
            ", username='" + username + '\'' +
            ", organizationId='" + organizationId + '\'' +
            ", roles=" + roles +
            ", isAuthenticated=" + isAuthenticated +
            '}';
    }
}
