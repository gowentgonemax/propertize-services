package com.propertize.platform.auth.rbac.engine.evaluators;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.rbac.engine.ConditionEvaluator;
import com.propertize.platform.auth.rbac.engine.PolicyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Evaluates conditional permission constraints (e.g., financial limits).
 * Allows fine-grained control like "can approve expenses up to $5000".
 *
 * Reads conditional_permissions from rbac.yml:
 * conditional_permissions:
 * PROPERTY_MANAGER:
 * "payment:approve":
 * max_amount: 5000
 * "maintenance:approve":
 * max_cost: 2500
 * REGIONAL_MANAGER:
 * "payment:approve":
 * max_amount: 25000
 * requires_secondary_approval_above: 10000
 *
 * Attributes expected:
 * - permission: the permission being checked
 * - amount / cost: numeric value of the transaction
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionalPermissionEvaluator implements ConditionEvaluator {

    private final RbacConfig rbacConfig;

    @Override
    public boolean evaluate(PolicyContext context, String condition, Map<String, Object> attributes) {
        if (!"conditional_permission".equals(condition)) {
            return true;
        }

        Map<String, Map<String, Map<String, Object>>> conditionalPerms = rbacConfig.getConditionalPermissions();
        if (conditionalPerms == null || conditionalPerms.isEmpty()) {
            return true;
        }

        String permission = attributes != null ? Objects.toString(attributes.get("permission"), null) : null;
        if (permission == null) {
            return true;
        }

        Set<String> roles = context.getRoles();
        if (roles == null || roles.isEmpty()) {
            return true;
        }

        for (String role : roles) {
            Map<String, Map<String, Object>> roleConditions = conditionalPerms.get(role);
            if (roleConditions == null) {
                continue;
            }

            Map<String, Object> permConditions = roleConditions.get(permission);
            if (permConditions == null) {
                continue; // No conditions for this permission on this role
            }

            if (evaluateConditions(permConditions, attributes)) {
                return true;
            }
        }

        // If no role had conditions for this permission, allow (the base permission
        // check handles access)
        boolean anyRoleHadConditions = roles.stream().anyMatch(role -> {
            Map<String, Map<String, Object>> rc = conditionalPerms.get(role);
            return rc != null && rc.containsKey(permission);
        });

        if (!anyRoleHadConditions) {
            return true; // No conditional restrictions defined
        }

        log.debug("Conditional permission check denied for user {} on permission {}", context.getUserId(), permission);
        return false;
    }

    @Override
    public boolean supports(String condition) {
        return "conditional_permission".equals(condition);
    }

    /**
     * Get the conditions map for a specific role and permission.
     * Returns null if no conditions are defined.
     */
    public Map<String, Object> getConditions(String role, String permission) {
        Map<String, Map<String, Map<String, Object>>> conditionalPerms = rbacConfig.getConditionalPermissions();
        if (conditionalPerms == null)
            return null;

        Map<String, Map<String, Object>> roleConditions = conditionalPerms.get(role);
        if (roleConditions == null)
            return null;

        return roleConditions.get(permission);
    }

    private boolean evaluateConditions(Map<String, Object> conditions, Map<String, Object> attributes) {
        // Check max_amount
        if (conditions.containsKey("max_amount")) {
            BigDecimal maxAmount = toBigDecimal(conditions.get("max_amount"));
            BigDecimal actualAmount = toBigDecimal(attributes.get("amount"));
            if (actualAmount != null && maxAmount != null && actualAmount.compareTo(maxAmount) > 0) {
                log.debug("Conditional check failed: amount {} exceeds max_amount {}", actualAmount, maxAmount);
                return false;
            }
        }

        // Check max_cost
        if (conditions.containsKey("max_cost")) {
            BigDecimal maxCost = toBigDecimal(conditions.get("max_cost"));
            BigDecimal actualCost = toBigDecimal(attributes.get("cost"));
            if (actualCost != null && maxCost != null && actualCost.compareTo(maxCost) > 0) {
                log.debug("Conditional check failed: cost {} exceeds max_cost {}", actualCost, maxCost);
                return false;
            }
        }

        // Check requires_secondary_approval_above
        if (conditions.containsKey("requires_secondary_approval_above")) {
            BigDecimal threshold = toBigDecimal(conditions.get("requires_secondary_approval_above"));
            BigDecimal actualAmount = toBigDecimal(attributes.get("amount"));
            if (actualAmount != null && threshold != null && actualAmount.compareTo(threshold) > 0) {
                Boolean hasSecondaryApproval = (Boolean) attributes.get("secondaryApproval");
                if (hasSecondaryApproval == null || !hasSecondaryApproval) {
                    log.debug("Conditional check failed: amount {} requires secondary approval above {}", actualAmount,
                            threshold);
                    return false;
                }
            }
        }

        return true;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return null;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        if (value instanceof Number)
            return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
