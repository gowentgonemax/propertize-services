package com.propertize.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Temporal Permission Entity
 *
 * Represents a time-bound permission grant for a user. Temporal permissions
 * are automatically expired after a configurable duration and can also be
 * manually revoked. This enables fine-grained, temporary access control
 * for scenarios such as emergency access, temporary project assignments,
 * or time-limited administrative privileges.
 *
 * @version 1.0 - Phase 1: Time-Based Access Control
 */
@Entity
@Table(name = "temporal_permissions", indexes = {
        @Index(name = "idx_temporal_user_id", columnList = "user_id"),
        @Index(name = "idx_temporal_expires_at", columnList = "expires_at"),
        @Index(name = "idx_temporal_is_active", columnList = "is_active"),
        @Index(name = "idx_temporal_user_active_expires", columnList = "user_id, is_active, expires_at"),
        @Index(name = "idx_temporal_granted_by", columnList = "granted_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemporalPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user who is granted this temporal permission.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The permission string being granted (e.g., "PROPERTY_WRITE",
     * "TENANT_DELETE").
     */
    @Column(nullable = false, length = 255)
    private String permission;

    /**
     * Optional role associated with this temporal permission grant.
     */
    @Column(length = 100)
    private String role;

    /**
     * Optional resource type that this permission is scoped to (e.g., "PROPERTY",
     * "LEASE").
     */
    @Column(name = "resource_type", length = 100)
    private String resourceType;

    /**
     * Optional specific resource ID that this permission is scoped to.
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * The ID of the user who granted this temporal permission.
     */
    @Column(name = "granted_by", nullable = false)
    private Long grantedBy;

    /**
     * Timestamp when the permission was granted.
     */
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    /**
     * Timestamp when the permission will automatically expire.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Reason for granting this temporal permission (audit trail).
     */
    @Column(length = 500)
    private String reason;

    /**
     * Whether this temporal permission is currently active.
     * Set to false when expired or manually revoked.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /**
     * Timestamp when the permission was manually revoked (null if not revoked).
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * The ID of the user who revoked this permission (null if not revoked).
     */
    @Column(name = "revoked_by")
    private Long revokedBy;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }
}
