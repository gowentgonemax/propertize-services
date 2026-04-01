package com.propertize.platform.employecraft.acl;

import com.propertize.platform.employecraft.acl.dto.AuthUserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Anti-Corruption Layer for Auth Service integration.
 * 
 * DDD Pattern: Translates Auth Service domain concepts into
 * Employecraft's bounded context. Prevents Auth Service's domain
 * model from leaking into Employecraft's domain.
 * 
 * All communication with Auth Service MUST go through this layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceAntiCorruptionLayer {

    private final AuthServiceClient authServiceClient;

    /**
     * Translate Auth Service user into Employecraft's employee context.
     * Auth Service speaks "users" — Employecraft speaks "employees".
     */
    public Optional<AuthUserDTO> resolveEmployee(String userId) {
        try {
            var authUser = authServiceClient.getUserById(userId);
            if (authUser == null) {
                log.warn("ACL: User {} not found in Auth Service", userId);
                return Optional.empty();
            }
            return Optional.of(translateToLocalContext(authUser));
        } catch (Exception e) {
            log.error("ACL: Failed to resolve employee {} from Auth Service: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validate that a user has the required role in Auth Service context,
     * translated to Employecraft's permission model.
     */
    public boolean hasEmployeePermission(String userId, String permission) {
        try {
            var authUser = authServiceClient.getUserById(userId);
            if (authUser == null)
                return false;
            return mapAuthRolesToEmployeePermissions(
                    authUser.getRoles() != null ? authUser.getRoles() : java.util.Collections.emptySet())
                    .contains(permission);
        } catch (Exception e) {
            log.error("ACL: Permission check failed for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Translate Auth Service roles to Employecraft permissions.
     * Auth Service uses: SUPER_ADMIN, ORGANIZATION_OWNER, ORGANIZATION_ADMIN, etc.
     * Employecraft uses: MANAGE_EMPLOYEES, VIEW_PAYROLL, APPROVE_LEAVE, etc.
     */
    private Set<String> mapAuthRolesToEmployeePermissions(Set<String> authRoles) {
        var permissions = new java.util.HashSet<String>();
        for (String role : authRoles) {
            switch (role) {
                case "SUPER_ADMIN", "ORGANIZATION_OWNER" -> permissions.addAll(Set.of(
                        "MANAGE_EMPLOYEES", "VIEW_PAYROLL", "APPROVE_LEAVE",
                        "MANAGE_DEPARTMENTS", "VIEW_REPORTS", "MANAGE_ATTENDANCE"));
                case "ORGANIZATION_ADMIN" -> permissions.addAll(Set.of(
                        "MANAGE_EMPLOYEES", "VIEW_PAYROLL", "APPROVE_LEAVE",
                        "MANAGE_DEPARTMENTS", "VIEW_REPORTS"));
                case "PROPERTY_MANAGER" -> permissions.addAll(Set.of(
                        "VIEW_PAYROLL", "APPROVE_LEAVE", "VIEW_REPORTS"));
                case "ACCOUNTANT" -> permissions.addAll(Set.of(
                        "VIEW_PAYROLL", "VIEW_REPORTS"));
                default -> permissions.add("VIEW_SELF");
            }
        }
        return permissions;
    }

    private AuthUserDTO translateToLocalContext(AuthUserDTO authUser) {
        // Translation layer: map Auth Service fields to Employecraft context
        // Currently 1:1 but this layer allows divergence without breaking changes
        return authUser;
    }
}
