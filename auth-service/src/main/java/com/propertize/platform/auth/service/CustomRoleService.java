package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.CustomRoleRequest;
import com.propertize.platform.auth.entity.CustomRole;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.CustomRoleRepository;
import com.propertize.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom Role Service — Phase 4a: Custom Role Builder.
 *
 * <p>
 * Manages the lifecycle of organization-scoped custom roles. Custom roles
 * allow organizations to create tailored permission profiles without modifying
 * the platform-wide RBAC configuration.
 * </p>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>CRUD operations for custom roles</li>
 * <li>Permission validation — requested permissions must be a subset of the
 * creator's</li>
 * <li>Privilege level enforcement — maxLevel cannot exceed the creator's
 * level</li>
 * <li>Effective permission resolution (inherited + custom)</li>
 * </ul>
 *
 * @version 1.0 - Phase 4a: Custom Role Builder
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomRoleService {

    private final CustomRoleRepository customRoleRepository;
    private final UserRepository userRepository;
    private final RbacService rbacService;

    /**
     * Create a new custom role for an organization.
     *
     * <p>
     * Validates that:
     * </p>
     * <ul>
     * <li>The role name is unique within the organization</li>
     * <li>The creator has all of the requested permissions</li>
     * <li>The maxLevel does not exceed the creator's privilege level</li>
     * <li>If inheritsFrom is specified, the base role exists</li>
     * </ul>
     *
     * @param request       the custom role creation request
     * @param creatorUserId the ID of the user creating the role
     * @return the created custom role
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    @CacheEvict(value = "customRolePermissions", allEntries = true)
    public CustomRole createCustomRole(CustomRoleRequest request, Long creatorUserId) {
        log.info("Creating custom role '{}' for org {} by user {}",
                request.getRoleName(), request.getOrganizationId(), creatorUserId);

        // Uniqueness check within organization
        if (customRoleRepository.existsByRoleNameAndOrganizationIdAndIsActiveTrue(
                request.getRoleName(), request.getOrganizationId())) {
            throw new IllegalArgumentException(
                    "A custom role named '" + request.getRoleName()
                            + "' already exists in organization " + request.getOrganizationId());
        }

        // Resolve creator's effective permissions
        Set<String> creatorPermissions = resolveCreatorPermissions(creatorUserId);
        int creatorMaxLevel = resolveCreatorMaxLevel(creatorUserId);

        // Validate maxLevel
        if (request.getMaxLevel() > creatorMaxLevel) {
            throw new IllegalArgumentException(
                    "maxLevel (" + request.getMaxLevel()
                            + ") exceeds creator's privilege level (" + creatorMaxLevel + ")");
        }

        // Build effective requested permissions (inherited + custom)
        Set<String> requestedPermissions = new LinkedHashSet<>(request.getPermissions());

        // Validate inheritsFrom base role exists
        if (request.getInheritsFrom() != null && !request.getInheritsFrom().isBlank()) {
            Set<String> basePermissions = rbacService.getPermissionsForRole(request.getInheritsFrom());
            if (basePermissions.isEmpty() && !rbacService.getAllRoles().contains(request.getInheritsFrom())) {
                throw new IllegalArgumentException(
                        "Base role '" + request.getInheritsFrom() + "' does not exist");
            }
            // The union of base + custom must still be a subset of creator's permissions
            requestedPermissions.addAll(basePermissions);
        }

        // Validate permissions are a subset of creator's
        if (!validatePermissions(requestedPermissions, creatorPermissions)) {
            Set<String> missing = new LinkedHashSet<>(requestedPermissions);
            missing.removeAll(creatorPermissions);
            throw new IllegalArgumentException(
                    "Requested permissions exceed creator's permissions. Missing: " + missing);
        }

        // Persist the custom role
        String permissionsCsv = request.getPermissions().stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(","));

        CustomRole customRole = CustomRole.builder()
                .roleName(request.getRoleName())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .organizationId(request.getOrganizationId())
                .permissions(permissionsCsv)
                .inheritsFrom(request.getInheritsFrom())
                .maxLevel(request.getMaxLevel())
                .createdBy(creatorUserId)
                .isActive(true)
                .isSystem(false)
                .build();

        CustomRole saved = customRoleRepository.save(customRole);
        log.info("Custom role '{}' created with id={} for org {}",
                saved.getRoleName(), saved.getId(), saved.getOrganizationId());
        return saved;
    }

    /**
     * Update an existing custom role.
     *
     * <p>
     * Applies the same validation rules as creation. System roles cannot be
     * updated.
     * </p>
     *
     * @param id            the custom role ID
     * @param request       the update request
     * @param updaterUserId the ID of the user performing the update
     * @return the updated custom role
     * @throws IllegalArgumentException if validation fails or the role is not found
     */
    @Transactional
    @CacheEvict(value = "customRolePermissions", allEntries = true)
    public CustomRole updateCustomRole(Long id, CustomRoleRequest request, Long updaterUserId) {
        log.info("Updating custom role id={} by user {}", id, updaterUserId);

        CustomRole existing = customRoleRepository.findById(id)
                .filter(CustomRole::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Custom role not found: " + id));

        if (existing.isSystem()) {
            throw new IllegalArgumentException("System roles cannot be modified");
        }

        // If the name is changing, check uniqueness within the org
        if (!existing.getRoleName().equals(request.getRoleName())) {
            if (customRoleRepository.existsByRoleNameAndOrganizationIdAndIsActiveTrue(
                    request.getRoleName(), request.getOrganizationId())) {
                throw new IllegalArgumentException(
                        "A custom role named '" + request.getRoleName()
                                + "' already exists in organization " + request.getOrganizationId());
            }
        }

        // Resolve updater's effective permissions
        Set<String> updaterPermissions = resolveCreatorPermissions(updaterUserId);
        int updaterMaxLevel = resolveCreatorMaxLevel(updaterUserId);

        // Validate maxLevel
        if (request.getMaxLevel() > updaterMaxLevel) {
            throw new IllegalArgumentException(
                    "maxLevel (" + request.getMaxLevel()
                            + ") exceeds updater's privilege level (" + updaterMaxLevel + ")");
        }

        // Build effective requested permissions (inherited + custom)
        Set<String> requestedPermissions = new LinkedHashSet<>(request.getPermissions());

        if (request.getInheritsFrom() != null && !request.getInheritsFrom().isBlank()) {
            Set<String> basePermissions = rbacService.getPermissionsForRole(request.getInheritsFrom());
            if (basePermissions.isEmpty() && !rbacService.getAllRoles().contains(request.getInheritsFrom())) {
                throw new IllegalArgumentException(
                        "Base role '" + request.getInheritsFrom() + "' does not exist");
            }
            requestedPermissions.addAll(basePermissions);
        }

        // Validate permissions are a subset of updater's
        if (!validatePermissions(requestedPermissions, updaterPermissions)) {
            Set<String> missing = new LinkedHashSet<>(requestedPermissions);
            missing.removeAll(updaterPermissions);
            throw new IllegalArgumentException(
                    "Requested permissions exceed updater's permissions. Missing: " + missing);
        }

        // Apply updates
        String permissionsCsv = request.getPermissions().stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(","));

        existing.setRoleName(request.getRoleName());
        existing.setDisplayName(request.getDisplayName());
        existing.setDescription(request.getDescription());
        existing.setOrganizationId(request.getOrganizationId());
        existing.setPermissions(permissionsCsv);
        existing.setInheritsFrom(request.getInheritsFrom());
        existing.setMaxLevel(request.getMaxLevel());

        CustomRole updated = customRoleRepository.save(existing);
        log.info("Custom role '{}' (id={}) updated successfully", updated.getRoleName(), updated.getId());
        return updated;
    }

    /**
     * Soft-delete a custom role.
     *
     * <p>
     * System roles ({@code isSystem = true}) cannot be deleted.
     * </p>
     *
     * @param id the custom role ID
     * @throws IllegalArgumentException if the role is not found or is a system role
     */
    @Transactional
    @CacheEvict(value = "customRolePermissions", allEntries = true)
    public void deleteCustomRole(Long id) {
        log.info("Soft-deleting custom role id={}", id);

        CustomRole existing = customRoleRepository.findById(id)
                .filter(CustomRole::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Custom role not found: " + id));

        if (existing.isSystem()) {
            throw new IllegalArgumentException("System roles cannot be deleted");
        }

        existing.setActive(false);
        customRoleRepository.save(existing);
        log.info("Custom role '{}' (id={}) soft-deleted", existing.getRoleName(), id);
    }

    /**
     * Get all active custom roles for an organization.
     *
     * @param organizationId the organization ID
     * @return list of active custom roles
     */
    @Transactional(readOnly = true)
    public List<CustomRole> getCustomRoles(Long organizationId) {
        log.debug("Fetching custom roles for org {}", organizationId);
        return customRoleRepository.findByOrganizationIdAndIsActiveTrue(organizationId);
    }

    /**
     * Get a single custom role by ID.
     *
     * @param id the custom role ID
     * @return the custom role
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public CustomRole getCustomRole(Long id) {
        return customRoleRepository.findById(id)
                .filter(CustomRole::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Custom role not found: " + id));
    }

    /**
     * Resolve the effective (union) permission set for a custom role.
     *
     * <p>
     * If the custom role inherits from a base role, the result is the union
     * of the base role's permissions and the custom role's directly assigned
     * permissions.
     * </p>
     *
     * @param customRoleId the custom role ID
     * @return the resolved effective permission set
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "customRolePermissions", key = "#customRoleId")
    public Set<String> resolveCustomRolePermissions(Long customRoleId) {
        CustomRole customRole = getCustomRole(customRoleId);
        return resolvePermissionsForEntity(customRole);
    }

    /**
     * Validate that the requested permissions are a subset of the creator's
     * effective permissions.
     *
     * @param requestedPerms the requested permission set
     * @param creatorPerms   the creator's effective permission set
     * @return true if requested is a subset of creator's permissions
     */
    public boolean validatePermissions(Set<String> requestedPerms, Set<String> creatorPerms) {
        if (requestedPerms == null || requestedPerms.isEmpty()) {
            return true;
        }
        if (creatorPerms == null || creatorPerms.isEmpty()) {
            return false;
        }
        return creatorPerms.containsAll(requestedPerms);
    }

    // ========================================================================
    // Internal Helpers
    // ========================================================================

    /**
     * Resolve effective permissions for a CustomRole entity (inherited + direct).
     */
    private Set<String> resolvePermissionsForEntity(CustomRole customRole) {
        Set<String> effective = new LinkedHashSet<>();

        // Add inherited permissions from base role
        if (customRole.getInheritsFrom() != null && !customRole.getInheritsFrom().isBlank()) {
            Set<String> basePermissions = rbacService.getPermissionsForRole(customRole.getInheritsFrom());
            effective.addAll(basePermissions);
        }

        // Add directly assigned permissions
        if (customRole.getPermissions() != null && !customRole.getPermissions().isBlank()) {
            Arrays.stream(customRole.getPermissions().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(effective::add);
        }

        return Collections.unmodifiableSet(effective);
    }

    /**
     * Resolve the effective permissions of a user from their assigned roles.
     */
    private Set<String> resolveCreatorPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Set<String> allPermissions = new LinkedHashSet<>();
        user.getRoles().forEach(roleEnum -> {
            String roleName = roleEnum.name();
            allPermissions.addAll(rbacService.getPermissionsForRole(roleName));
        });

        return allPermissions;
    }

    /**
     * Resolve the maximum privilege level for a user based on their roles.
     *
     * <p>
     * Uses the RBAC config's level property for each role. If a role does
     * not declare a level, it defaults to 0. The creator's max level is the
     * highest level across all of their assigned roles.
     * </p>
     */
    private int resolveCreatorMaxLevel(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return user.getRoles().stream()
                .map(roleEnum -> {
                    var config = rbacService.getRoleConfig(roleEnum.name());
                    if (config != null && config.getLevel() != null) {
                        return config.getLevel();
                    }
                    return 0;
                })
                .max(Integer::compareTo)
                .orElse(0);
    }
}
