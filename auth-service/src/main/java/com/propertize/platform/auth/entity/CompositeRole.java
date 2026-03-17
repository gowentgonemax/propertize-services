package com.propertize.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Composite Role Entity
 *
 * Represents a named combination of multiple base roles that can be assigned
 * to users as a single unit. The effective permission set is computed as the
 * union of all component role permissions, with conflict resolution and
 * priority-based scope escalation.
 *
 * <p>
 * Composite roles enable organizations to create reusable role bundles
 * (e.g., "Property Manager + Accountant") without modifying the core RBAC
 * configuration.
 * </p>
 *
 * @version 1.0 - Phase 3: Dynamic Role Composition
 */
@Entity
@Table(name = "composite_roles", indexes = {
        @Index(name = "idx_composite_role_name", columnList = "name"),
        @Index(name = "idx_composite_role_org_id", columnList = "organization_id"),
        @Index(name = "idx_composite_role_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompositeRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique name for the composite role (e.g., "PM_ACCOUNTANT").
     */
    @Column(nullable = false, unique = true, length = 150)
    private String name;

    /**
     * Human-readable description of what this composite role represents.
     */
    @Column(length = 500)
    private String description;

    /**
     * The list of base role names that make up this composite role.
     * Stored as a comma-separated string in the database.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "component_roles", nullable = false, length = 2000)
    private List<String> componentRoles;

    /**
     * The ID of the user who created this composite role.
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * Timestamp when this composite role was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Whether this composite role is currently active.
     * Supports soft-delete semantics.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /**
     * Optional organization ID to scope this composite role to a specific org.
     * If null, the composite role is available platform-wide.
     */
    @Column(name = "organization_id")
    private Long organizationId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
