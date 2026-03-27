package com.propertize.platform.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for a single RBAC role — returned by the public /api/v1/rbac/roles
 * endpoint.
 *
 * <ul>
 * <li>{@code name} — lowercase underscore identifier matching JWT claims, e.g.
 * {@code platform_oversight}</li>
 * <li>{@code label} — human-readable title, e.g.
 * {@code Platform Oversight}</li>
 * <li>{@code description} — from rbac.yml</li>
 * <li>{@code scope} — from rbac.yml: {@code platform}, {@code organization},
 * {@code self}, …</li>
 * <li>{@code level} — numeric privilege level from rbac.yml (higher = more
 * powerful)</li>
 * <li>{@code category} — from rbac.yml: {@code platform}, {@code operations},
 * {@code external}, …</li>
 * <li>{@code permissions} — fully-expanded permission set (includes inherited +
 * wildcard expansion)</li>
 * </ul>
 */
@Data
@Builder
public class RoleDto {
    private String name;
    private String label;
    private String description;
    private String scope;
    private int level;
    private String category;
    private List<String> permissions;
}
