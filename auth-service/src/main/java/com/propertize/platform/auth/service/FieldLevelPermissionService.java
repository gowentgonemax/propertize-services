package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Field-Level Permission Service.
 * Controls which fields of a resource are visible/hidden per role.
 *
 * Reads field_level_permissions from rbac.yml:
 * field_level_permissions:
 * tenant:
 * PROPERTY_MANAGER:
 * visible: [name, email, phone, lease_info, payment_status]
 * hidden: [ssn, bank_account, credit_score]
 * LEASING_AGENT:
 * visible: [name, email, phone, rental_application]
 * hidden: [ssn, bank_account, payment_history]
 *
 * @version 2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldLevelPermissionService {

    private final RbacConfig rbacConfig;

    /**
     * Get the set of visible fields for a given resource and role.
     * Returns null if no field-level restrictions are defined (=all fields
     * visible).
     */
    public Set<String> getVisibleFields(String resourceType, String role) {
        Map<String, Map<String, RbacConfig.FieldPermission>> fieldPerms = rbacConfig.getFieldLevelPermissions();
        if (fieldPerms == null)
            return null;

        Map<String, RbacConfig.FieldPermission> resourcePerms = fieldPerms.get(resourceType);
        if (resourcePerms == null)
            return null;

        RbacConfig.FieldPermission rolePerms = resourcePerms.get(role);
        if (rolePerms == null)
            return null;

        if (rolePerms.getVisible() != null) {
            return new LinkedHashSet<>(rolePerms.getVisible());
        }
        return null;
    }

    /**
     * Get the set of hidden fields for a given resource and role.
     * Returns empty set if no field-level restrictions are defined.
     */
    public Set<String> getHiddenFields(String resourceType, String role) {
        Map<String, Map<String, RbacConfig.FieldPermission>> fieldPerms = rbacConfig.getFieldLevelPermissions();
        if (fieldPerms == null)
            return Collections.emptySet();

        Map<String, RbacConfig.FieldPermission> resourcePerms = fieldPerms.get(resourceType);
        if (resourcePerms == null)
            return Collections.emptySet();

        RbacConfig.FieldPermission rolePerms = resourcePerms.get(role);
        if (rolePerms == null)
            return Collections.emptySet();

        if (rolePerms.getHidden() != null) {
            return new LinkedHashSet<>(rolePerms.getHidden());
        }
        return Collections.emptySet();
    }

    /**
     * Get computed field permissions for a resource given multiple roles.
     * Uses union of visible and intersection of hidden (most permissive).
     */
    public FieldAccessResult getFieldAccess(String resourceType, Collection<String> roles) {
        Map<String, Map<String, RbacConfig.FieldPermission>> fieldPerms = rbacConfig.getFieldLevelPermissions();
        if (fieldPerms == null || !fieldPerms.containsKey(resourceType)) {
            return FieldAccessResult.allAccess();
        }

        Map<String, RbacConfig.FieldPermission> resourcePerms = fieldPerms.get(resourceType);
        Set<String> allVisible = new LinkedHashSet<>();
        Set<String> commonHidden = null;

        for (String role : roles) {
            RbacConfig.FieldPermission perms = resourcePerms.get(role);
            if (perms == null) {
                // Role with no restrictions → grant all
                return FieldAccessResult.allAccess();
            }

            if (perms.getVisible() != null) {
                allVisible.addAll(perms.getVisible());
            }

            if (perms.getHidden() != null) {
                if (commonHidden == null) {
                    commonHidden = new LinkedHashSet<>(perms.getHidden());
                } else {
                    commonHidden.retainAll(perms.getHidden()); // Intersection
                }
            }
        }

        return new FieldAccessResult(false, allVisible, commonHidden != null ? commonHidden : Collections.emptySet());
    }

    /**
     * Check if a specific field is visible for a resource given roles.
     */
    public boolean isFieldVisible(String resourceType, String fieldName, Collection<String> roles) {
        FieldAccessResult access = getFieldAccess(resourceType, roles);
        if (access.unrestricted())
            return true;
        if (access.hiddenFields().contains(fieldName))
            return false;
        if (access.visibleFields().isEmpty())
            return true; // No visible list means all visible
        return access.visibleFields().contains(fieldName);
    }

    /**
     * Get all configured resource types that have field-level permissions.
     */
    public Set<String> getConfiguredResources() {
        Map<String, Map<String, RbacConfig.FieldPermission>> fieldPerms = rbacConfig.getFieldLevelPermissions();
        if (fieldPerms == null)
            return Collections.emptySet();
        return fieldPerms.keySet();
    }

    /**
     * Result of field access computation.
     */
    public record FieldAccessResult(
            boolean unrestricted,
            Set<String> visibleFields,
            Set<String> hiddenFields) {
        public static FieldAccessResult allAccess() {
            return new FieldAccessResult(true, Collections.emptySet(), Collections.emptySet());
        }
    }
}
