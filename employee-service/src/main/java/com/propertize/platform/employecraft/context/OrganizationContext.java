package com.propertize.platform.employecraft.context;

import java.util.UUID;

/**
 * Thread-local organization context for multi-tenancy.
 * 
 * Set by TrustedGatewayHeaderFilter from gateway-forwarded headers.
 * Used throughout the service to scope queries to the current organization.
 */
public class OrganizationContext {

    private static final ThreadLocal<UUID> currentOrganizationId = new ThreadLocal<>();

    public static void setOrganizationId(UUID organizationId) {
        currentOrganizationId.set(organizationId);
    }

    public static UUID getOrganizationId() {
        return currentOrganizationId.get();
    }

    public static void clear() {
        currentOrganizationId.remove();
    }

    public static UUID requireOrganizationId() {
        UUID orgId = getOrganizationId();
        if (orgId == null) {
            throw new IllegalStateException("Organization context is not set");
        }
        return orgId;
    }
}
