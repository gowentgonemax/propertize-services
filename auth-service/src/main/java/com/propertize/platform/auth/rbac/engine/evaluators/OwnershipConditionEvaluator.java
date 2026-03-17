package com.propertize.platform.auth.rbac.engine.evaluators;

import com.propertize.platform.auth.rbac.engine.ConditionEvaluator;
import com.propertize.platform.auth.rbac.engine.PolicyContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Evaluates ownership conditions.
 * Checks if user owns the resource or has org-level access.
 *
 * @version 2.0 - Centralized in auth-service
 */
@Slf4j
@Component
public class OwnershipConditionEvaluator implements ConditionEvaluator {

    @Override
    public boolean evaluate(PolicyContext context, String condition, Map<String, Object> attributes) {
        if (!"ownership".equals(condition)) {
            return true;
        }

        // Check if user owns the resource
        Object ownerIdObj = attributes.get("ownerId");
        if (ownerIdObj != null) {
            String ownerId = ownerIdObj.toString();
            if (context.getUserId().equals(ownerId)) {
                log.debug("Ownership check passed: user {} owns resource", context.getUserId());
                return true;
            }
        }

        // Check organizational ownership
        Object orgIdObj = attributes.get("organizationId");
        if (orgIdObj != null && context.getOrganizationId() != null) {
            UUID resourceOrgId = parseUUID(orgIdObj);
            if (context.getOrganizationId().equals(resourceOrgId)) {
                log.debug("Org ownership check passed: user {} in org {}",
                        context.getUserId(), context.getOrganizationId());
                return true;
            }
        }

        // Platform admin bypass
        if (context.hasRole("PLATFORM_OVERSIGHT") || context.hasRole("PLATFORM_OPERATIONS")) {
            log.debug("Platform admin check passed: user {} has admin access", context.getUserId());
            return true;
        }

        log.debug("Ownership check failed: user {} does not own resource", context.getUserId());
        return false;
    }

    @Override
    public boolean supports(String condition) {
        return "ownership".equals(condition);
    }

    private UUID parseUUID(Object obj) {
        if (obj instanceof UUID)
            return (UUID) obj;
        if (obj instanceof String) {
            try {
                return UUID.fromString((String) obj);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format: {}", obj);
                return null;
            }
        }
        return null;
    }
}
