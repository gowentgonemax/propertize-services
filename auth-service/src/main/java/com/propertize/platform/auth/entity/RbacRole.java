package com.propertize.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unified RBAC role catalog.
 *
 * <p>
 * A single table ({@code rbac_roles}) stores both:
 * </p>
 * <ul>
 * <li><b>System roles</b> ({@code isSystem=true, organizationId=NULL}) — seeded
 * from
 * {@code rbac.yml} at every startup by
 * {@link com.propertize.platform.auth.service.RbacSeederService}.</li>
 * <li><b>Custom roles</b> ({@code isSystem=false, organizationId != NULL}) —
 * created at runtime by
 * organisation admins via the custom-role API.</li>
 * </ul>
 *
 * <p>
 * Permissions are stored as a comma-separated CSV string and exposed as a
 * {@code Set<String>} via {@link #getPermissionSet()}.
 * </p>
 */
@Entity
@Table(name = "rbac_roles", indexes = {
        @Index(name = "idx_rbac_role_is_system", columnList = "is_system"),
        @Index(name = "idx_rbac_role_org_id", columnList = "organization_id"),
        @Index(name = "idx_rbac_role_is_active", columnList = "is_active"),
        @Index(name = "idx_rbac_role_name", columnList = "role_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RbacRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, length = 150)
    private String roleName;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(length = 500)
    private String description;

    /** e.g. "platform", "portfolio", "organization", "team", "self" */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String scope = "self";

    /** Numeric privilege level — higher = more powerful. */
    @Column(nullable = false)
    @Builder.Default
    private int level = 0;

    /** e.g. "platform", "portfolio", "organization", "operational", "external" */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String category = "";

    /** Comma-separated list of permission strings. */
    @Column(columnDefinition = "TEXT")
    private String permissions;

    /** Base role name this role inherits from (comma-separated for multiple). */
    @Column(name = "inherits_from", length = 500)
    private String inheritsFrom;

    /**
     * Comma-separated list of org-type names this role is designed for
     * (e.g. "INDIVIDUAL_PROPERTY_OWNER,REAL_ESTATE_INVESTOR").
     * Advisory only — not enforced at DB level.
     */
    @Column(name = "applicable_org_types", length = 500)
    private String applicableOrgTypes;

    /**
     * Comma-separated list of permissions explicitly denied for this role,
     * even if inherited from a parent or group.
     */
    @Column(name = "explicit_denials", columnDefinition = "TEXT")
    private String explicitDenials;

    /**
     * {@code true} for roles seeded from rbac.yml; {@code false} for runtime
     * custom roles. System roles cannot be deleted.
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    /**
     * {@code null} for platform-wide system roles; non-null for org-scoped
     * custom roles.
     */
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -----------------------------------------------------------------------
    // Convenience helpers
    // -----------------------------------------------------------------------

    /** Returns the permissions CSV as an immutable {@code Set<String>}. */
    public Set<String> getPermissionSet() {
        if (permissions == null || permissions.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(permissions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Updates the permissions CSV from a Set. */
    public void setPermissionSet(Set<String> perms) {
        this.permissions = (perms == null || perms.isEmpty())
                ? ""
                : String.join(",", perms);
    }
}
