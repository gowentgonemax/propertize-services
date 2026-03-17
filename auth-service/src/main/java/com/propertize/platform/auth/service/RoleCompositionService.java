package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.entity.CompositeRole;
import com.propertize.platform.auth.repository.CompositeRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Role Composition Service — Phase 3: Dynamic Role Composition.
 *
 * <p>
 * Handles the composition of multiple base roles into a unified effective
 * permission set. Supports:
 * </p>
 * <ul>
 * <li>Computing the union of permissions across multiple roles</li>
 * <li>Resolving conflicts when roles have contradictory scope/access
 * levels</li>
 * <li>CRUD operations for named composite roles (persisted in DB)</li>
 * <li>Role inheritance — higher-level roles automatically include lower-level
 * permissions</li>
 * <li>Cached permission resolution for performance</li>
 * </ul>
 *
 * @version 1.0 - Phase 3: Dynamic Role Composition
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleCompositionService {

    private final RbacService rbacService;
    private final RbacConfig rbacConfig;
    private final CompositeRoleRepository compositeRoleRepository;

    /**
     * Scope priority order — higher index means broader scope.
     * Used by {@link #getEffectiveScope(Set)} to determine the winning scope.
     */
    private static final List<String> SCOPE_PRIORITY = List.of(
            "self", // 0 — most restricted
            "team", // 1
            "property", // 2
            "client", // 3
            "organization", // 4
            "platform" // 5 — broadest
    );

    // ========================================================================
    // Permission Resolution
    // ========================================================================

    /**
     * Resolve the effective (union) permission set for a collection of roles.
     *
     * <p>
     * The result is the deduplicated union of all permissions granted by each
     * role (including inherited permissions). Duplicate and invalid role names
     * are silently skipped.
     * </p>
     *
     * @param roles set of role names to resolve
     * @return the merged permission set, never null
     */
    @Cacheable(value = "compositePermissions", key = "#roles.toString()")
    public Set<String> resolveEffectivePermissions(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            log.debug("Empty role set provided — returning empty permissions");
            return Collections.emptySet();
        }

        Set<String> deduplicated = roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (deduplicated.isEmpty()) {
            return Collections.emptySet();
        }

        log.debug("Resolving effective permissions for roles: {}", deduplicated);

        Set<String> effectivePermissions = new LinkedHashSet<>();
        Set<String> invalidRoles = new LinkedHashSet<>();

        for (String role : deduplicated) {
            Set<String> rolePermissions = rbacService.getPermissionsForRole(role);
            if (rolePermissions.isEmpty() && !rbacService.getAllRoles().contains(role)) {
                invalidRoles.add(role);
            } else {
                effectivePermissions.addAll(rolePermissions);
            }
        }

        if (!invalidRoles.isEmpty()) {
            log.warn("Unknown roles ignored during permission resolution: {}", invalidRoles);
        }

        // Apply conflict resolution
        Set<String> resolved = resolveRoleConflicts(deduplicated);
        // resolved contains the "winning" set of roles after conflict handling;
        // but permissions are already the union — conflicts only affect metadata,
        // so the permission set stays as-is.

        log.info("Resolved {} effective permissions for {} role(s)", effectivePermissions.size(), deduplicated.size());
        return Collections.unmodifiableSet(effectivePermissions);
    }

    /**
     * Resolve the effective permission set for a named composite role.
     *
     * @param compositeRoleName the name of the persisted composite role
     * @return the merged permission set, or empty set if the composite role is not
     *         found
     */
    @Cacheable(value = "compositePermissions", key = "'composite:' + #compositeRoleName")
    public Set<String> resolveEffectivePermissions(String compositeRoleName) {
        if (compositeRoleName == null || compositeRoleName.isBlank()) {
            return Collections.emptySet();
        }

        Optional<CompositeRole> compositeRole = compositeRoleRepository
                .findByNameAndIsActiveTrue(compositeRoleName.trim());

        if (compositeRole.isEmpty()) {
            log.warn("Composite role '{}' not found or inactive", compositeRoleName);
            return Collections.emptySet();
        }

        List<String> componentRoles = compositeRole.get().getComponentRoles();
        return resolveEffectivePermissions(new LinkedHashSet<>(componentRoles));
    }

    // ========================================================================
    // Composite Role CRUD
    // ========================================================================

    /**
     * Create a new composite role.
     *
     * @param name           unique name for the composite role
     * @param description    human-readable description
     * @param componentRoles list of base role names to compose
     * @param createdBy      ID of the user creating this composite role
     * @param orgId          optional organization ID (null for platform-wide)
     * @return the persisted composite role entity
     * @throws IllegalArgumentException if the name already exists or component
     *                                  roles are invalid
     */
    @Transactional
    @CacheEvict(value = "compositePermissions", allEntries = true)
    public CompositeRole createCompositeRole(String name, String description,
            List<String> componentRoles,
            Long createdBy, Long orgId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Composite role name must not be blank");
        }
        if (componentRoles == null || componentRoles.isEmpty()) {
            throw new IllegalArgumentException("At least one component role is required");
        }

        String trimmedName = name.trim().toUpperCase(Locale.ROOT);

        if (compositeRoleRepository.existsByNameAndIsActiveTrue(trimmedName)) {
            throw new IllegalArgumentException("Composite role '" + trimmedName + "' already exists");
        }

        // Validate that all component roles are known
        Set<String> allKnownRoles = rbacService.getAllRoles();
        List<String> validatedRoles = componentRoles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        if (validatedRoles.isEmpty()) {
            throw new IllegalArgumentException("No valid component roles provided");
        }

        List<String> unknownRoles = validatedRoles.stream()
                .filter(r -> !allKnownRoles.contains(r))
                .collect(Collectors.toList());

        if (!unknownRoles.isEmpty()) {
            throw new IllegalArgumentException("Unknown roles: " + unknownRoles);
        }

        CompositeRole compositeRole = CompositeRole.builder()
                .name(trimmedName)
                .description(description)
                .componentRoles(validatedRoles)
                .createdBy(createdBy)
                .organizationId(orgId)
                .isActive(true)
                .build();

        CompositeRole saved = compositeRoleRepository.save(compositeRole);
        log.info("Created composite role '{}' with components {} (id={})",
                trimmedName, validatedRoles, saved.getId());
        return saved;
    }

    /**
     * Soft-delete a composite role by marking it as inactive.
     *
     * @param id the composite role ID
     * @throws IllegalArgumentException if the composite role is not found
     */
    @Transactional
    @CacheEvict(value = "compositePermissions", allEntries = true)
    public void deactivateCompositeRole(Long id) {
        CompositeRole compositeRole = compositeRoleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Composite role not found: " + id));

        compositeRole.setActive(false);
        compositeRoleRepository.save(compositeRole);
        log.info("Deactivated composite role '{}' (id={})", compositeRole.getName(), id);
    }

    /**
     * Get all active composite roles, optionally filtered by organization.
     *
     * @param orgId optional organization ID filter; pass null for all
     * @return list of active composite roles
     */
    @Transactional(readOnly = true)
    public List<CompositeRole> getCompositeRoles(Long orgId) {
        if (orgId != null) {
            return compositeRoleRepository.findByOrganizationIdAndIsActiveTrue(orgId);
        }
        return compositeRoleRepository.findByIsActiveTrue();
    }

    /**
     * Find a composite role by its ID.
     *
     * @param id the composite role ID
     * @return the composite role, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<CompositeRole> getCompositeRoleById(Long id) {
        return compositeRoleRepository.findById(id)
                .filter(CompositeRole::isActive);
    }

    // ========================================================================
    // Conflict Resolution
    // ========================================================================

    /**
     * Resolve role conflicts and return the winning set of roles.
     *
     * <p>
     * Conflict resolution rules:
     * </p>
     * <ol>
     * <li>If a role with {@code bypassAllChecks} (e.g., PLATFORM_OVERSIGHT) is
     * present,
     * it supersedes all others — return just that role.</li>
     * <li>Higher-level roles take precedence over lower-level roles in the same
     * category.
     * (e.g., PROPERTY_MANAGER level 700 supersedes ASSISTANT_PROPERTY_MANAGER level
     * 600).</li>
     * <li>Roles in different categories are kept in parallel — no conflict.</li>
     * </ol>
     *
     * @param roles the set of role names to resolve
     * @return the de-conflicted set of roles
     */
    public Set<String> resolveRoleConflicts(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        // Fast path: bypass role supersedes everything
        if (rbacService.shouldBypassAllChecks(roles)) {
            Optional<String> bypassRole = roles.stream()
                    .filter(rbacService::hasBypassAllChecks)
                    .findFirst();
            if (bypassRole.isPresent()) {
                log.debug("Bypass role '{}' supersedes all other roles", bypassRole.get());
                return Set.of(bypassRole.get());
            }
        }

        // Group by category, keep the highest level in each category
        Map<String, String> winnerByCategory = new LinkedHashMap<>();
        Map<String, Integer> levelByCategory = new LinkedHashMap<>();

        for (String role : roles) {
            RbacConfig.RoleConfig config = rbacConfig.getRoles() != null
                    ? rbacConfig.getRoles().get(role)
                    : null;

            if (config == null) {
                // Unknown role — keep it anyway so permission resolution can log a warning
                winnerByCategory.putIfAbsent("__unknown__:" + role, role);
                continue;
            }

            String category = config.getCategory() != null ? config.getCategory() : "uncategorized";
            int level = config.getLevel() != null ? config.getLevel() : 0;

            if (!levelByCategory.containsKey(category) || level > levelByCategory.get(category)) {
                winnerByCategory.put(category, role);
                levelByCategory.put(category, level);
            }
        }

        Set<String> resolved = new LinkedHashSet<>(winnerByCategory.values());
        if (!resolved.equals(roles)) {
            log.debug("Role conflict resolution: {} → {}", roles, resolved);
        }
        return resolved;
    }

    /**
     * Determine the effective (highest-priority) scope across a set of roles.
     *
     * <p>
     * If a user holds roles with scopes "self" and "organization", the effective
     * scope is "organization" (the broader one).
     * </p>
     *
     * @param roles the set of role names
     * @return the highest scope string, or "self" if none found
     */
    public String getEffectiveScope(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "self";
        }

        int maxPriority = -1;
        String effectiveScope = "self";

        for (String role : roles) {
            String scope = rbacService.getScopeForRole(role);
            if (scope != null) {
                int priority = SCOPE_PRIORITY.indexOf(scope.toLowerCase(Locale.ROOT));
                if (priority > maxPriority) {
                    maxPriority = priority;
                    effectiveScope = scope;
                }
            }
        }

        log.debug("Effective scope for roles {}: {}", roles, effectiveScope);
        return effectiveScope;
    }
}
