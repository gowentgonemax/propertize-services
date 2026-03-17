package com.propertize.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Delegation Rule Entity
 *
 * Defines the rules governing which roles can delegate which permissions
 * to which target roles, along with constraints on duration and chain depth.
 * These rules are pre-configured (seeded via Flyway) and control what
 * delegation operations are allowed in the system.
 *
 * @version 1.0 - Phase 3: Permission Delegation
 */
@Entity
@Table(name = "delegation_rules", indexes = {
        @Index(name = "idx_delegation_rule_delegator_role", columnList = "delegator_role"),
        @Index(name = "idx_delegation_rule_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelegationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The role that is allowed to delegate permissions (e.g., "PROPERTY_MANAGER").
     */
    @Column(name = "delegator_role", nullable = false, length = 100)
    private String delegatorRole;

    /**
     * Comma-separated list of permissions that this role can delegate
     * (e.g., "maintenance:assign,maintenance:view,tenant:view").
     */
    @Column(name = "delegatable_permissions", nullable = false, length = 1000)
    private String delegatablePermissions;

    /**
     * Comma-separated list of roles that can receive delegated permissions
     * (e.g., "LEASING_AGENT,MAINTENANCE_TECHNICIAN").
     */
    @Column(name = "allowed_delegate_roles", nullable = false, length = 500)
    private String allowedDelegateRoles;

    /**
     * Maximum duration in hours for a delegation under this rule.
     * Default: 168 hours (7 days).
     */
    @Column(name = "max_duration_hours", nullable = false)
    @Builder.Default
    private int maxDurationHours = 168;

    /**
     * Whether a reason must be provided when delegating.
     */
    @Column(name = "requires_reason", nullable = false)
    @Builder.Default
    private boolean requiresReason = true;

    /**
     * Whether the delegation requires approval from a higher authority before
     * activation.
     */
    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private boolean requiresApproval = false;

    /**
     * Maximum delegation chain depth. Default 1 means no re-delegation.
     * A value of 2 would allow A→B→C but not further.
     */
    @Column(name = "max_chain_depth", nullable = false)
    @Builder.Default
    private int maxChainDepth = 1;

    /**
     * Whether this delegation rule is currently active.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ---- Helper methods ----

    /**
     * Check if a specific permission is delegatable under this rule.
     */
    public boolean isPermissionDelegatable(String permission) {
        if (delegatablePermissions == null || delegatablePermissions.isBlank()) {
            return false;
        }
        for (String p : delegatablePermissions.split(",")) {
            if (p.trim().equalsIgnoreCase(permission.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the target role is allowed to receive delegated permissions.
     */
    public boolean isRoleAllowed(String delegateRole) {
        if (allowedDelegateRoles == null || allowedDelegateRoles.isBlank()) {
            return false;
        }
        for (String r : allowedDelegateRoles.split(",")) {
            if (r.trim().equalsIgnoreCase(delegateRole.trim())) {
                return true;
            }
        }
        return false;
    }
}
