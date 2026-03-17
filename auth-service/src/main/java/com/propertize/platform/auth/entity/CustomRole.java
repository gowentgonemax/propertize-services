package com.propertize.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Custom Role Entity
 *
 * Represents an organization-scoped custom role with a curated set of
 * permissions. Custom roles allow organizations to create tailored access
 * profiles (e.g., "Front Desk Agent") without modifying the platform-wide
 * RBAC configuration.
 *
 * <p>
 * Key constraints:
 * </p>
 * <ul>
 * <li>Scoped to a single organization — not platform-wide.</li>
 * <li>Permissions must be a <em>subset</em> of the creator's effective
 * permissions.</li>
 * <li>{@code maxLevel} cannot exceed the creator's own privilege level.</li>
 * <li>Optionally inherits from a standard base role — the effective permission
 * set is the union of inherited + custom permissions.</li>
 * <li>System roles ({@code isSystem = true}) cannot be deleted.</li>
 * </ul>
 *
 * @version 1.0 - Phase 4a: Custom Role Builder
 */
@Entity
@Table(name = "custom_roles", indexes = {
        @Index(name = "idx_custom_role_org_id", columnList = "organization_id"),
        @Index(name = "idx_custom_role_name", columnList = "role_name"),
        @Index(name = "idx_custom_role_inherits", columnList = "inherits_from"),
        @Index(name = "idx_custom_role_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique role name within the organization (e.g., "FRONT_DESK_AGENT").
     */
    @Column(name = "role_name", nullable = false, length = 150)
    private String roleName;

    /**
     * Human-readable display name (e.g., "Front Desk Agent").
     */
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    /**
     * Optional description of the custom role's purpose.
     */
    @Column(length = 500)
    private String description;

    /**
     * The organization this custom role belongs to.
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Comma-separated list of permissions granted by this custom role.
     */
    @Column(nullable = false, length = 4000)
    private String permissions;

    /**
     * Optional base role name to inherit permissions from.
     * When set, the effective permission set is the union of inherited + custom.
     */
    @Column(name = "inherits_from", length = 150)
    private String inheritsFrom;

    /**
     * Maximum privilege level for this custom role.
     * Cannot exceed the creator's own privilege level.
     */
    @Column(name = "max_level", nullable = false)
    private int maxLevel;

    /**
     * The ID of the user who created this custom role.
     */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /**
     * Timestamp when this custom role was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this custom role was last updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Whether this custom role is currently active.
     * Supports soft-delete semantics.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /**
     * Whether this is a system-managed role that cannot be deleted by users.
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
