package com.propertize.platform.auth.rbac.engine.evaluators;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.rbac.engine.ConditionEvaluator;
import com.propertize.platform.auth.rbac.engine.PolicyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Evaluates data scope constraints for row-level security.
 * Controls which resources a user can access based on their portfolio,
 * team, region, or direct assignment.
 *
 * Reads data_scopes from rbac.yml:
 * data_scopes:
 * PROPERTY_MANAGER:
 * property: "assigned_portfolio"
 * tenant: "own_properties"
 * REGIONAL_MANAGER:
 * property: "own_region"
 *
 * Attributes expected in the context/attributes map:
 * - resourceType: the resource being accessed (e.g., "property")
 * - assignedPortfolioIds: set of portfolio IDs the user is assigned to
 * - resourcePortfolioId: the target resource's portfolio ID
 * - assignedPropertyIds: set of property IDs the user manages
 * - resourcePropertyId: the target resource's property ID
 * - userRegion: user's region
 * - resourceRegion: resource's region
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeConditionEvaluator implements ConditionEvaluator {

    private final RbacConfig rbacConfig;

    @Override
    public boolean evaluate(PolicyContext context, String condition, Map<String, Object> attributes) {
        if (!"data_scope".equals(condition)) {
            return true;
        }

        Map<String, Map<String, String>> dataScopes = rbacConfig.getDataScopes();
        if (dataScopes == null || dataScopes.isEmpty()) {
            return true;
        }

        String resourceType = attributes != null ? Objects.toString(attributes.get("resourceType"), null) : null;
        if (resourceType == null) {
            return true; // No resource type specified, skip scope check
        }

        Set<String> roles = context.getRoles();
        if (roles == null || roles.isEmpty()) {
            return true;
        }

        for (String role : roles) {
            Map<String, String> roleScopes = dataScopes.get(role);
            if (roleScopes == null) {
                continue; // Role has no data scope constraints — allowed
            }

            String scopeRule = roleScopes.get(resourceType);
            if (scopeRule == null) {
                continue; // No scope rule for this resource type — allowed
            }

            if (evaluateScopeRule(scopeRule, context, attributes)) {
                return true; // At least one role grants access
            }
        }

        // If all roles with scope rules denied, check if any role had no scope rule
        for (String role : roles) {
            Map<String, String> roleScopes = dataScopes.get(role);
            if (roleScopes == null || !roleScopes.containsKey(resourceType)) {
                return true; // This role has no restrictions for this resource
            }
        }

        log.debug("Data scope check denied for user {} on resource type {}", context.getUserId(), resourceType);
        return false;
    }

    @Override
    public boolean supports(String condition) {
        return "data_scope".equals(condition);
    }

    /**
     * Resolve the effective data scope for a given role and resource type.
     * Returns the scope rule string, or null if unrestricted.
     */
    public String resolveScope(String role, String resourceType) {
        Map<String, Map<String, String>> dataScopes = rbacConfig.getDataScopes();
        if (dataScopes == null)
            return null;

        Map<String, String> roleScopes = dataScopes.get(role);
        if (roleScopes == null)
            return null;

        return roleScopes.get(resourceType);
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateScopeRule(String scopeRule, PolicyContext context, Map<String, Object> attributes) {
        return switch (scopeRule) {
            case "assigned_portfolio" -> {
                Collection<String> assignedPortfolioIds = getCollection(attributes, "assignedPortfolioIds");
                String resourcePortfolioId = Objects.toString(attributes.get("resourcePortfolioId"), null);
                yield resourcePortfolioId != null && assignedPortfolioIds.contains(resourcePortfolioId);
            }
            case "own_properties" -> {
                Collection<String> assignedPropertyIds = getCollection(attributes, "assignedPropertyIds");
                String resourcePropertyId = Objects.toString(attributes.get("resourcePropertyId"), null);
                yield resourcePropertyId != null && assignedPropertyIds.contains(resourcePropertyId);
            }
            case "own_region" -> {
                String userRegion = Objects.toString(attributes.get("userRegion"), null);
                String resourceRegion = Objects.toString(attributes.get("resourceRegion"), null);
                yield userRegion != null && userRegion.equals(resourceRegion);
            }
            case "own_organization" -> {
                // Check via PolicyContext's organizationId
                String resourceOrgId = Objects.toString(attributes.get("organizationId"), null);
                UUID contextOrgId = context.getOrganizationId();
                yield contextOrgId != null && contextOrgId.toString().equals(resourceOrgId);
            }
            case "self_only" -> {
                String resourceOwnerId = Objects.toString(attributes.get("ownerId"), null);
                yield context.getUserId() != null && context.getUserId().equals(resourceOwnerId);
            }
            case "all" -> true;
            default -> {
                log.warn("Unknown data scope rule: {}", scopeRule);
                yield false;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getCollection(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Collection) {
            return (Collection<String>) value;
        }
        return Collections.emptySet();
    }
}
