package com.propertize.platform.auth.rbac.engine;

import com.propertize.platform.auth.service.DynamicRoleComposer;
import com.propertize.platform.auth.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of PolicyEngine.
 * Integrates with RbacService for permission resolution.
 * Supports RBAC + ABAC (ownership, time, data scope, conditional) + dynamic
 * role composition.
 *
 * @version 3.0 - Enhanced with time, data scope, conditional evaluators +
 *          dynamic role composition
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultPolicyEngine implements PolicyEngine {

    private final RbacService rbacService;
    private final List<ConditionEvaluator> conditionEvaluators;
    private final DynamicRoleComposer dynamicRoleComposer;

    private static final String CONFIG_VERSION = "3.0.0";

    @Override
    public PolicyDecision evaluate(PolicyContext context, Resource resource, Action action,
            Map<String, Object> attributes) {
        long startTime = System.currentTimeMillis();

        try {
            // Step 0: Dynamic role composition — augment roles based on context
            PolicyContext effectiveContext = applyDynamicRoles(context);

            String permission = resource.getKey() + ":" + action.getKey();

            // Step 1: RBAC check — does the user have the base permission?
            boolean hasPermission = hasPermissionForContext(effectiveContext, permission);

            if (!hasPermission) {
                return buildDenyDecision(effectiveContext, resource, action,
                        "User does not have permission: " + permission, startTime);
            }

            // Step 2: ABAC checks — evaluate all condition evaluators
            Map<String, Object> enrichedAttributes = new HashMap<>(
                    attributes != null ? attributes : Collections.emptyMap());
            enrichedAttributes.put("permission", permission);

            for (ConditionEvaluator evaluator : conditionEvaluators) {
                // Ownership check
                if (evaluator.supports("ownership") &&
                        (enrichedAttributes.containsKey("ownerId")
                                || enrichedAttributes.containsKey("organizationId"))) {
                    if (!evaluator.evaluate(effectiveContext, "ownership", enrichedAttributes)) {
                        return buildDenyDecision(effectiveContext, resource, action,
                                "Ownership condition not met", startTime);
                    }
                }

                // Time-based check
                if (evaluator.supports("time_restriction")) {
                    if (!evaluator.evaluate(effectiveContext, "time_restriction", enrichedAttributes)) {
                        return buildDenyDecision(effectiveContext, resource, action,
                                "Access denied: outside allowed time window", startTime);
                    }
                }

                // Data scope check
                if (evaluator.supports("data_scope") && enrichedAttributes.containsKey("resourceType")) {
                    if (!evaluator.evaluate(effectiveContext, "data_scope", enrichedAttributes)) {
                        return buildDenyDecision(effectiveContext, resource, action,
                                "Data scope constraint not met", startTime);
                    }
                }

                // Conditional permission check (financial limits, etc.)
                if (evaluator.supports("conditional_permission") &&
                        (enrichedAttributes.containsKey("amount") || enrichedAttributes.containsKey("cost"))) {
                    if (!evaluator.evaluate(effectiveContext, "conditional_permission", enrichedAttributes)) {
                        return buildDenyDecision(effectiveContext, resource, action,
                                "Conditional permission constraint not met (e.g., amount limit exceeded)", startTime);
                    }
                }
            }

            return buildAllowDecision(effectiveContext, resource, action, permission, startTime);

        } catch (Exception e) {
            log.error("Error evaluating policy for user {} on {}:{}", context.getUserId(), resource, action, e);
            return buildDenyDecision(context, resource, action,
                    "Policy evaluation error: " + e.getMessage(), startTime);
        }
    }

    /**
     * Apply dynamic role composition to augment the context's roles.
     */
    private PolicyContext applyDynamicRoles(PolicyContext context) {
        Set<String> composedRoles = dynamicRoleComposer.composeRoles(context);
        if (composedRoles.size() == context.getRoles().size()) {
            return context; // No new roles added
        }

        // Build new context with composed roles
        PolicyContext.PolicyContextBuilder builder = PolicyContext.builder()
                .userId(context.getUserId())
                .organizationId(context.getOrganizationId())
                .tenantId(context.getTenantId())
                .requestPath(context.getRequestPath())
                .requestMethod(context.getRequestMethod())
                .ipAddress(context.getIpAddress())
                .sessionId(context.getSessionId())
                .userEmail(context.getUserEmail())
                .authenticated(context.isAuthenticated())
                .internalRequest(context.isInternalRequest())
                .timestamp(context.getTimestamp())
                .correlationId(context.getCorrelationId())
                .userAgent(context.getUserAgent());

        composedRoles.forEach(builder::role);
        if (context.getAttributes() != null) {
            context.getAttributes().forEach(builder::attribute);
        }

        return builder.build();
    }

    /**
     * Check permission using the effective (possibly composed) context.
     */
    private boolean hasPermissionForContext(PolicyContext context, String permission) {
        if (context.getRoles() == null || context.getRoles().isEmpty()) {
            return false;
        }
        for (String role : context.getRoles()) {
            if (rbacService.hasPermission(role, permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Cacheable(value = "permissions", key = "#context.userId + ':' + #permission")
    public boolean hasPermission(PolicyContext context, String permission) {
        if (context.getRoles() == null || context.getRoles().isEmpty()) {
            return false;
        }

        for (String role : context.getRoles()) {
            if (rbacService.hasPermission(role, permission)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Cacheable(value = "userPermissions", key = "#context.userId")
    public Set<String> listPermissions(PolicyContext context) {
        if (context.getRoles() == null || context.getRoles().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> allPermissions = new HashSet<>();
        for (String role : context.getRoles()) {
            allPermissions.addAll(rbacService.getPermissionsForRole(role));
        }

        return allPermissions;
    }

    @Override
    public Set<Resource> listAccessibleResources(PolicyContext context) {
        Set<String> permissions = listPermissions(context);

        return permissions.stream()
                .map(perm -> perm.split(":")[0])
                .filter(Resource::exists)
                .map(Resource::fromKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Action> getAllowedActions(PolicyContext context, Resource resource) {
        Set<String> permissions = listPermissions(context);
        String resourceKey = resource.getKey();

        return permissions.stream()
                .filter(perm -> perm.startsWith(resourceKey + ":"))
                .map(perm -> perm.split(":")[1])
                .filter(Action::exists)
                .map(Action::fromKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, PolicyDecision> evaluateBatch(PolicyContext context, Map<Resource, Set<Action>> checks) {
        Map<String, PolicyDecision> results = new ConcurrentHashMap<>();

        checks.forEach((resource, actions) -> actions.forEach(action -> {
            String key = resource.getKey() + ":" + action.getKey();
            PolicyDecision decision = evaluate(context, resource, action, Collections.emptyMap());
            results.put(key, decision);
        }));

        return results;
    }

    @Override
    public boolean hasAnyPermission(PolicyContext context, String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(context, permission))
                return true;
        }
        return false;
    }

    @Override
    public boolean hasAllPermissions(PolicyContext context, String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(context, permission))
                return false;
        }
        return true;
    }

    @Override
    @CacheEvict(value = { "permissions", "userPermissions" }, key = "#userId")
    public void invalidateCache(String userId) {
        log.debug("Invalidated permission cache for user: {}", userId);
    }

    @Override
    @CacheEvict(value = { "permissions", "userPermissions" }, allEntries = true)
    public void invalidateAllCache() {
        log.info("Invalidated all permission caches");
    }

    @Override
    public String getConfigVersion() {
        return CONFIG_VERSION;
    }

    private PolicyDecision buildAllowDecision(PolicyContext context, Resource resource, Action action,
            String permission, long startTime) {
        PolicyDecision.PolicyDecisionBuilder builder = PolicyDecision.builder()
                .allowed(true)
                .reason("Permission granted: " + permission)
                .policyId("policy-v3")
                .configVersion(CONFIG_VERSION)
                .matchedPermission(permission)
                .resource(resource.getKey())
                .action(action.getKey())
                .userId(context.getUserId())
                .organizationId(context.getOrganizationId() != null ? context.getOrganizationId().toString() : null)
                .evaluationTimeMs(System.currentTimeMillis() - startTime)
                .fromCache(false);

        if (context.getRoles() != null) {
            context.getRoles().forEach(builder::evaluatedRole);
        }

        return builder.build();
    }

    private PolicyDecision buildDenyDecision(PolicyContext context, Resource resource, Action action,
            String reason, long startTime) {
        PolicyDecision.PolicyDecisionBuilder builder = PolicyDecision.builder()
                .allowed(false)
                .reason(reason)
                .policyId("policy-v3")
                .configVersion(CONFIG_VERSION)
                .resource(resource.getKey())
                .action(action.getKey())
                .userId(context.getUserId())
                .organizationId(context.getOrganizationId() != null ? context.getOrganizationId().toString() : null)
                .evaluationTimeMs(System.currentTimeMillis() - startTime)
                .fromCache(false);

        if (context.getRoles() != null) {
            context.getRoles().forEach(builder::evaluatedRole);
        }

        return builder.build();
    }
}
