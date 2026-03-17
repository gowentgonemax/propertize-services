package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC Configuration Service — provides RBAC config data to other services.
 *
 * @version 2.0 - Full implementation (was stub)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacConfigService {

    private final RbacConfig rbacConfig;
    private final RbacService rbacService;

    /**
     * Get role → permissions map for all roles.
     */
    public Map<String, Set<String>> getRbacConfig() {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        if (rbacConfig.getRoles() == null)
            return result;

        for (String role : rbacConfig.getRoles().keySet()) {
            result.put(role, rbacService.getPermissionsForRole(role));
        }
        return result;
    }

    /**
     * Get role details including scope, level, restrictions, capabilities.
     */
    public Map<String, Object> getRoleDetails(String role) {
        RbacConfig.RoleConfig roleConfig = rbacConfig.getRoles() != null ? rbacConfig.getRoles().get(role) : null;

        if (roleConfig == null)
            return Collections.emptyMap();

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("description", roleConfig.getDescription());
        details.put("scope", roleConfig.getScope());
        details.put("level", roleConfig.getLevel());
        details.put("category", roleConfig.getCategory());
        details.put("bypassAllChecks", roleConfig.isBypassAllChecks());
        details.put("inherits", roleConfig.getInherits());
        details.put("permissions", rbacService.getPermissionsForRole(role));

        if (roleConfig.getRestrictions() != null) {
            details.put("restrictions", roleConfig.getRestrictions());
        }
        if (roleConfig.getCapabilities() != null) {
            details.put("capabilities", roleConfig.getCapabilities());
        }
        if (roleConfig.getFeatures() != null) {
            details.put("features", roleConfig.getFeatures());
        }

        return details;
    }

    /**
     * Get all roles.
     */
    public Set<String> getAllRoles() {
        return rbacService.getAllRoles();
    }

    /**
     * Get roles by scope (platform, organization, team, self).
     */
    public Set<String> getRolesByScope(String scope) {
        if (rbacConfig.getRoles() == null)
            return Collections.emptySet();

        return rbacConfig.getRoles().entrySet().stream()
                .filter(entry -> scope.equalsIgnoreCase(entry.getValue().getScope()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Get endpoint permission mappings.
     */
    public Map<String, Map<String, String>> getEndpointPermissions() {
        return rbacService.getEndpointPermissions();
    }

    /**
     * Get RBAC config version.
     */
    public String getConfigVersion() {
        return rbacConfig.getVersion();
    }
}
