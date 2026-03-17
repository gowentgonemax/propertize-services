package com.propertize.platform.auth.rbac.engine;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Context for policy evaluation containing user, org, and request information.
 * Immutable context passed to PolicyEngine.
 *
 * @version 2.0 - Centralized in auth-service
 */
@Data
@Builder
public class PolicyContext {

    private final String userId;

    @Singular
    private final Set<String> roles;

    private final UUID organizationId;
    private final UUID tenantId;
    private final String requestPath;
    private final String requestMethod;
    private final String ipAddress;
    private final String sessionId;
    private final String userEmail;

    @Singular
    private final Map<String, Object> attributes;

    @Builder.Default
    private final boolean authenticated = true;

    @Builder.Default
    private final boolean internalRequest = false;

    @Builder.Default
    private final long timestamp = System.currentTimeMillis();

    private final String correlationId;
    private final String userAgent;

    public boolean hasAttribute(String key) {
        return attributes != null && attributes.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        if (attributes == null)
            return null;
        Object value = attributes.get(key);
        if (value == null)
            return null;
        if (type.isInstance(value))
            return (T) value;
        throw new ClassCastException("Attribute " + key + " is not of type " + type.getName());
    }

    public <T> T getAttribute(String key, T defaultValue) {
        if (attributes == null || !attributes.containsKey(key))
            return defaultValue;
        try {
            @SuppressWarnings("unchecked")
            T value = (T) attributes.get(key);
            return value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... rolesToCheck) {
        if (roles == null || rolesToCheck == null)
            return false;
        for (String role : rolesToCheck) {
            if (roles.contains(role))
                return true;
        }
        return false;
    }

    public boolean hasAllRoles(String... rolesToCheck) {
        if (roles == null || rolesToCheck == null)
            return false;
        for (String role : rolesToCheck) {
            if (!roles.contains(role))
                return false;
        }
        return true;
    }

    public String getDisplayName() {
        if (userEmail != null)
            return userEmail;
        if (userId != null)
            return userId;
        return "anonymous";
    }

    public static PolicyContext anonymous() {
        return PolicyContext.builder()
                .userId("anonymous")
                .authenticated(false)
                .build();
    }

    public static PolicyContext service(String serviceName) {
        return PolicyContext.builder()
                .userId("service:" + serviceName)
                .authenticated(true)
                .internalRequest(true)
                .build();
    }
}
