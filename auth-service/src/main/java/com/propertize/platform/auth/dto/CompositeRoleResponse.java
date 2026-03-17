package com.propertize.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Response DTO for composite role queries.
 *
 * <p>
 * Provides a clean API response representation of a composite role entity,
 * including the resolved effective permission set computed as the union of
 * all component role permissions.
 * </p>
 *
 * @version 1.0 - Phase 3: Dynamic Role Composition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompositeRoleResponse {

    /**
     * The unique identifier of the composite role.
     */
    private Long id;

    /**
     * Unique name of the composite role.
     */
    private String name;

    /**
     * Human-readable description of the composite role.
     */
    private String description;

    /**
     * The list of base role names that make up this composite role.
     */
    private List<String> componentRoles;

    /**
     * The resolved effective permission set (union of all component role
     * permissions).
     * Populated on read operations; may be null for list responses.
     */
    private Set<String> effectivePermissions;

    /**
     * Timestamp when this composite role was created.
     */
    private LocalDateTime createdAt;

    /**
     * Whether this composite role is currently active.
     */
    private boolean isActive;
}
