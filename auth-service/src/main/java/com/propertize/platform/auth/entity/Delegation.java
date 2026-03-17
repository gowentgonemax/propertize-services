package com.propertize.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Delegation Entity
 *
 * Represents an actual delegation of a specific permission from one user
 * (the delegator) to another user (the delegate). Delegations are time-bound,
 * auditable, and support optional approval workflows and chain tracking.
 *
 * Each active delegation creates a corresponding TemporalPermission for the
 * delegate user, ensuring that the delegated permission is resolved during
 * authorization checks.
 *
 * @version 1.0 - Phase 3: Permission Delegation
 */
@Entity
@Table(name = "delegations", indexes = {
        @Index(name = "idx_delegation_delegator", columnList = "delegator_user_id"),
        @Index(name = "idx_delegation_delegate", columnList = "delegate_user_id"),
        @Index(name = "idx_delegation_status", columnList = "status"),
        @Index(name = "idx_delegation_delegate_status", columnList = "delegate_user_id, status"),
        @Index(name = "idx_delegation_delegator_status", columnList = "delegator_user_id, status"),
        @Index(name = "idx_delegation_expires_at", columnList = "expires_at"),
        @Index(name = "idx_delegation_org", columnList = "organization_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who is delegating the permission.
     */
    @Column(name = "delegator_user_id", nullable = false)
    private Long delegatorUserId;

    /**
     * The user who is receiving the delegated permission.
     */
    @Column(name = "delegate_user_id", nullable = false)
    private Long delegateUserId;

    /**
     * The permission string being delegated (e.g., "maintenance:assign").
     */
    @Column(nullable = false, length = 255)
    private String permission;

    /**
     * Timestamp when the delegation was created.
     */
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    /**
     * Timestamp when the delegation will automatically expire.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Reason for the delegation (audit trail).
     */
    @Column(length = 500)
    private String reason;

    /**
     * Current status of the delegation.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DelegationStatus status;

    /**
     * Reference to a parent delegation, for tracking delegation chains.
     * Null if this is a top-level delegation (not a re-delegation).
     */
    @Column(name = "parent_delegation_id")
    private Long parentDelegationId;

    /**
     * The ID of the user who approved this delegation (if approval was required).
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    /**
     * Timestamp when the delegation was approved.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * The ID of the user who revoked this delegation (if revoked).
     */
    @Column(name = "revoked_by")
    private Long revokedBy;

    /**
     * Timestamp when the delegation was revoked.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * The organization context for this delegation.
     */
    @Column(name = "organization_id")
    private Long organizationId;

    /**
     * The ID of the underlying TemporalPermission created for this delegation.
     * Used for revoking the permission when the delegation is revoked.
     */
    @Column(name = "temporal_permission_id")
    private Long temporalPermissionId;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DelegationStatus.ACTIVE;
        }
    }
}
