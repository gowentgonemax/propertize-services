package com.propertize.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Maps a user to a custom (org-scoped) RBAC role.
 *
 * <p>
 * System role assignments are stored in {@code user_roles} (enum-backed).
 * This table covers only the runtime custom roles created by organisation
 * admins.
 * </p>
 *
 * <p>
 * An active entry means the user currently holds that custom role. Setting
 * {@code isActive=false} revokes the assignment without losing audit history.
 * </p>
 */
@Entity
@Table(name = "user_custom_role_assignments", indexes = {
        @Index(name = "idx_ucra_user_id", columnList = "user_id"),
        @Index(name = "idx_ucra_role_id", columnList = "rbac_role_id"),
        @Index(name = "idx_ucra_org_id", columnList = "organization_id"),
        @Index(name = "idx_ucra_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCustomRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numeric user ID from the {@code users} table. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The custom role being assigned. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rbac_role_id", nullable = false)
    private RbacRole rbacRole;

    /** Organisation context in which this role assignment is valid. */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** The user who made the assignment (audit). */
    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    /** Optional — {@code null} means the assignment never expires. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
