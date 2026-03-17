package com.propertize.platform.auth.rbac.engine;

import java.util.Map;
import java.util.Set;

/**
 * Core Policy Engine interface for dynamic RBAC evaluation.
 * Replaces all hardcoded role checks with config-driven authorization.
 *
 * @version 2.0 - Centralized in auth-service
 */
public interface PolicyEngine {

    PolicyDecision evaluate(PolicyContext context, Resource resource, Action action, Map<String, Object> attributes);

    boolean hasPermission(PolicyContext context, String permission);

    Set<String> listPermissions(PolicyContext context);

    Set<Resource> listAccessibleResources(PolicyContext context);

    Set<Action> getAllowedActions(PolicyContext context, Resource resource);

    Map<String, PolicyDecision> evaluateBatch(PolicyContext context, Map<Resource, Set<Action>> checks);

    boolean hasAnyPermission(PolicyContext context, String... permissions);

    boolean hasAllPermissions(PolicyContext context, String... permissions);

    void invalidateCache(String userId);

    void invalidateAllCache();

    String getConfigVersion();
}
