package com.propertize.platform.auth.rbac.engine;

import java.util.Map;

/**
 * Evaluates conditional expressions for attribute-based access control (ABAC).
 * Supports ownership checks, org scoping, feature flags, etc.
 *
 * @version 2.0 - Centralized in auth-service
 */
public interface ConditionEvaluator {

    /**
     * Evaluate a condition expression
     */
    boolean evaluate(PolicyContext context, String condition, Map<String, Object> attributes);

    /**
     * Check if this evaluator supports the given condition type
     */
    boolean supports(String condition);
}
