package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.AuthorizationRequest;
import com.propertize.platform.auth.dto.AuthorizationResponse;
import com.propertize.platform.auth.rbac.engine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Centralized Authorization Service.
 * This is the main entry point for all authorization decisions across the
 * platform.
 * Other services call this via REST API through the auth-service.
 *
 * @version 2.0 - Full implementation (was stub)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final RbacService rbacService;
    private final PolicyEngine policyEngine;

    /**
     * Authorize a request using the Policy Engine.
     */
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        log.debug("Authorization request: userId={}, resource={}, action={}, roles={}",
                request.getUserId(), request.getResource(), request.getAction(), request.getRoles());

        try {
            // Build policy context
            PolicyContext.PolicyContextBuilder contextBuilder = PolicyContext.builder()
                    .userId(request.getUserId());

            // Add roles
            if (request.getRoles() != null) {
                request.getRoles().forEach(contextBuilder::role);
            }

            // Add organization context
            if (request.getOrganizationId() != null) {
                try {
                    contextBuilder.organizationId(UUID.fromString(request.getOrganizationId()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid organizationId format: {}", request.getOrganizationId());
                }
            }

            PolicyContext context = contextBuilder.build();

            // Check bypass
            if (request.getRoles() != null && rbacService.shouldBypassAllChecks(request.getRoles())) {
                log.info("Platform admin bypass for user: {}", request.getUserId());
                return AuthorizationResponse.builder()
                        .authorized(true)
                        .reason("Platform admin bypass")
                        .build();
            }

            // Use policy engine if resource/action are typed enums
            if (request.getResource() != null && request.getAction() != null) {
                try {
                    Resource resource = Resource.fromKey(request.getResource());
                    Action action = Action.fromKey(request.getAction());

                    Map<String, Object> attributes = request.getAttributes() != null ? request.getAttributes()
                            : Collections.emptyMap();

                    PolicyDecision decision = policyEngine.evaluate(context, resource, action, attributes);

                    return AuthorizationResponse.builder()
                            .authorized(decision.isAllowed())
                            .reason(decision.getReason())
                            .matchedPermissions(decision.getMatchedPermissions())
                            .evaluatedRoles(decision.getEvaluatedRoles())
                            .evaluationTimeMs(decision.getEvaluationTimeMs())
                            .build();

                } catch (IllegalArgumentException e) {
                    // Resource/Action not in enum — fall through to permission string check
                    log.debug("Resource/Action not in enum, using permission string check: {}",
                            e.getMessage());
                }
            }

            // Fallback: check raw permission string
            if (request.getPermission() != null) {
                boolean hasPermission = policyEngine.hasPermission(context, request.getPermission());
                return AuthorizationResponse.builder()
                        .authorized(hasPermission)
                        .reason(hasPermission ? "Permission granted" : "Permission denied: " + request.getPermission())
                        .build();
            }

            // If resource:action provided as combined string
            if (request.getResource() != null && request.getAction() != null) {
                String permission = request.getResource() + ":" + request.getAction();
                boolean hasPermission = policyEngine.hasPermission(context, permission);
                return AuthorizationResponse.builder()
                        .authorized(hasPermission)
                        .reason(hasPermission ? "Permission granted" : "Permission denied: " + permission)
                        .build();
            }

            return AuthorizationResponse.builder()
                    .authorized(false)
                    .reason("Invalid authorization request: missing resource/action or permission")
                    .build();

        } catch (Exception e) {
            log.error("Authorization error for user {}: {}", request.getUserId(), e.getMessage(), e);
            return AuthorizationResponse.builder()
                    .authorized(false)
                    .reason("Authorization error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get all permissions for a set of roles.
     */
    public Set<String> getPermissionsForRoles(Collection<String> roles) {
        if (roles == null || roles.isEmpty())
            return Collections.emptySet();

        Set<String> allPermissions = new LinkedHashSet<>();
        for (String role : roles) {
            allPermissions.addAll(rbacService.getPermissionsForRole(role));
        }
        return allPermissions;
    }

    /**
     * Check multiple permissions at once (batch).
     */
    public Map<String, Boolean> checkPermissions(String userId, Collection<String> roles,
            Collection<String> permissions) {
        PolicyContext.PolicyContextBuilder builder = PolicyContext.builder().userId(userId);
        if (roles != null)
            roles.forEach(builder::role);
        PolicyContext context = builder.build();

        Map<String, Boolean> results = new LinkedHashMap<>();
        if (permissions != null) {
            for (String permission : permissions) {
                results.put(permission, policyEngine.hasPermission(context, permission));
            }
        }
        return results;
    }

    /**
     * Invalidate permission cache for a user.
     */
    public void invalidateUserCache(String userId) {
        policyEngine.invalidateCache(userId);
    }

    /**
     * Invalidate all caches (e.g., after config reload).
     */
    public void invalidateAllCaches() {
        policyEngine.invalidateAllCache();
    }
}
