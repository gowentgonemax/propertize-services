package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.rbac.engine.PolicyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Dynamic Role Composer.
 * Composes additional roles at runtime based on context attributes
 * (e.g., subscription plan, org status, user verification).
 *
 * Reads dynamic_role_composition from rbac.yml:
 * dynamic_role_composition:
 * ORGANIZATION_OWNER:
 * activate_when:
 * org_status: "ACTIVE"
 * user_verified: true
 * additional_roles:
 * - "FINANCIAL_ANALYST"
 * conditions:
 * subscription_plan: ["PREMIUM", "ENTERPRISE"]
 *
 * @version 2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicRoleComposer {

    private final RbacConfig rbacConfig;

    /**
     * Compute the effective roles for a user, including any dynamically composed
     * roles
     * based on the policy context attributes.
     */
    public Set<String> composeRoles(PolicyContext context) {
        Set<String> effectiveRoles = new LinkedHashSet<>(context.getRoles());

        Map<String, RbacConfig.DynamicRoleComposition> compositions = rbacConfig.getDynamicRoleComposition();
        if (compositions == null || compositions.isEmpty()) {
            return effectiveRoles;
        }

        for (String baseRole : context.getRoles()) {
            RbacConfig.DynamicRoleComposition composition = compositions.get(baseRole);
            if (composition == null)
                continue;

            if (meetsActivationConditions(composition, context)) {
                if (composition.getAdditionalRoles() != null) {
                    effectiveRoles.addAll(composition.getAdditionalRoles());
                    log.debug("Dynamic role composition: added {} for base role {}",
                            composition.getAdditionalRoles(), baseRole);
                }
            }
        }

        return effectiveRoles;
    }

    /**
     * Check if a specific base role can be dynamically activated given the context.
     */
    public boolean canActivateRole(String baseRole, PolicyContext context) {
        Map<String, RbacConfig.DynamicRoleComposition> compositions = rbacConfig.getDynamicRoleComposition();
        if (compositions == null)
            return true;

        RbacConfig.DynamicRoleComposition composition = compositions.get(baseRole);
        if (composition == null)
            return true; // No composition rules = always active

        return meetsActivationConditions(composition, context);
    }

    private boolean meetsActivationConditions(RbacConfig.DynamicRoleComposition composition, PolicyContext context) {
        // Check activate_when conditions
        if (composition.getActivateWhen() != null) {
            for (Map.Entry<String, Object> entry : composition.getActivateWhen().entrySet()) {
                String key = entry.getKey();
                Object expectedValue = entry.getValue();
                Object actualValue = context.hasAttribute(key) ? context.getAttribute(key, Object.class) : null;

                if (actualValue == null || !matchesValue(expectedValue, actualValue)) {
                    log.debug("Dynamic composition activation failed: {} expected {} but got {}",
                            key, expectedValue, actualValue);
                    return false;
                }
            }
        }

        // Check conditions (list-based — value must be in allowed list)
        if (composition.getConditions() != null) {
            for (Map.Entry<String, Object> entry : composition.getConditions().entrySet()) {
                String key = entry.getKey();
                Object allowedValues = entry.getValue();
                Object actualValue = context.hasAttribute(key) ? context.getAttribute(key, Object.class) : null;

                if (actualValue == null) {
                    return false;
                }

                if (allowedValues instanceof List<?> allowedList) {
                    if (!allowedList.contains(actualValue) && !allowedList.contains(actualValue.toString())) {
                        log.debug("Dynamic composition condition failed: {} value {} not in {}",
                                key, actualValue, allowedList);
                        return false;
                    }
                } else if (!matchesValue(allowedValues, actualValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean matchesValue(Object expected, Object actual) {
        if (expected == null && actual == null)
            return true;
        if (expected == null || actual == null)
            return false;

        // Handle boolean comparisons
        if (expected instanceof Boolean) {
            if (actual instanceof Boolean)
                return expected.equals(actual);
            return expected.toString().equalsIgnoreCase(actual.toString());
        }

        return expected.toString().equals(actual.toString());
    }
}
