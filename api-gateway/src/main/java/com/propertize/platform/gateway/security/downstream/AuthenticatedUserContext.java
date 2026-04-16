package com.propertize.platform.gateway.security.downstream;

import lombok.Data;

import java.util.Set;

/**
 * Authenticated User Context
 *
 * This class represents the authenticated user context that is propagated
 * from the API Gateway to downstream services via HTTP headers.
 *
 * Downstream services should use this class to access user information
 * instead of parsing JWT tokens directly.
 */
@Data
public class AuthenticatedUserContext {

    private String userId;
    private String username;
    private String organizationId;
    private String organizationCode;
    private Set<String> roles;
    private String primaryRole;
    private String correlationId;

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user is platform admin
     */
    public boolean isPlatformAdmin() {
        return hasRole("PLATFORM_OVERSIGHT") || hasRole("PLATFORM_OPERATIONS");
    }

    /**
     * Check if user is organization owner
     */
    public boolean isOrganizationOwner() {
        return hasRole("ORGANIZATION_OWNER");
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String... rolesToCheck) {
        if (roles == null)
            return false;
        for (String role : rolesToCheck) {
            if (roles.contains(role))
                return true;
        }
        return false;
    }
}
