package com.propertize.platform.auth.rbac.engine;

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
 *
 * @version 2.0 - Centralized in auth-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultPolicyEngine implements PolicyEngine {

    private final RbacService rbacService;
    private final List<ConditionEvaluator> conditionEvaluators;

    private static final String CONFIG_VERSION = "2.0.0";

    @Override
    public PolicyDecision evaluate(PolicyContext context, Resource resource, Action action,
            Map<String, Object> attributes) {
        long startTime = System.currentTimeMillis();

        try {
            String permission = resource.getKey() + ":" + action.getKey();

            boolean hasPermission = hasPermission(context, permission);

            if (!hasPermission) {
                return buildDenyDecision(context, resource, action,
                        "User does not have permission: " + permission, startTime);
            }

            // Evaluate ABAC conditions
            if (attributes != null && !attributes.isEmpty()) {
                for (ConditionEvaluator evaluator : conditionEvaluators) {
                    if (attributes.containsKey("ownerId") || attributes.containsKey("organizationId")) {
                        if (!evaluator.evaluate(context, "ownership", attributes)) {
                            return buildDenyDecision(context, resource, action,
                                    "Ownership condition not met", startTime);
                        }
                    }
                }
            }

            return buildAllowDecision(context, resource, action, permission, startTime);

        } catch (Exception e) {
            log.error("Error evaluating policy for user {} on {}:{}", context.getUserId(), resource, action, e);
            return buildDenyDecision(context, resource, action,
                    "Policy evaluation error: " + e.getMessage(), startTime);
        }
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
                .policyId("policy-v2")
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
                .policyId("policy-v2")
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
