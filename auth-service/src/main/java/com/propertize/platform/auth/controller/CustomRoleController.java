package com.propertize.platform.auth.controller;

import com.propertize.platform.auth.dto.CustomRoleRequest;
import com.propertize.platform.auth.dto.CustomRoleResponse;
import com.propertize.platform.auth.entity.CustomRole;
import com.propertize.platform.auth.entity.RbacRole;
import com.propertize.platform.auth.entity.UserCustomRoleAssignment;
import com.propertize.platform.auth.repository.UserRepository;
import com.propertize.platform.auth.service.CustomRoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom Role Management API
 *
 * <p>
 * Exposes CRUD endpoints for org-scoped custom roles and role-assignment
 * operations.
 * All endpoints expect the API Gateway to have already validated the JWT and
 * injected
 * {@code X-User-Id} (username string) and {@code X-Organization-Id} headers.
 * </p>
 *
 * <pre>
 * POST   /api/v1/rbac/custom-roles                          — create custom role
 * GET    /api/v1/rbac/custom-roles?orgId={id}               — list org custom roles
 * GET    /api/v1/rbac/custom-roles/{id}                     — get single custom role
 * PUT    /api/v1/rbac/custom-roles/{id}                     — update custom role
 * DELETE /api/v1/rbac/custom-roles/{id}                     — soft-delete custom role
 * POST   /api/v1/rbac/custom-roles/{id}/assign              — assign role to user
 * DELETE /api/v1/rbac/custom-roles/{id}/assign/{userId}     — revoke role from user
 * GET    /api/v1/rbac/users/{userId}/custom-roles           — get user's custom roles
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/rbac")
@RequiredArgsConstructor
@Slf4j
public class CustomRoleController {

    private final CustomRoleService customRoleService;
    private final UserRepository userRepository;

    // =========================================================================
    // Custom role CRUD
    // =========================================================================

    @PostMapping("/custom-roles")
    public ResponseEntity<CustomRoleResponse> createCustomRole(
            @Valid @RequestBody CustomRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {

        Long creatorId = resolveNumericUserId(xUserId);
        CustomRole created = customRoleService.createCustomRole(request, creatorId);
        log.info("✅ Custom role '{}' created for org {} by user {}",
                created.getRoleName(), created.getOrganizationId(), creatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created, null));
    }

    @GetMapping("/custom-roles")
    public ResponseEntity<List<CustomRoleResponse>> listCustomRoles(
            @RequestParam("orgId") @NotNull Long orgId) {

        List<CustomRole> roles = customRoleService.getCustomRoles(orgId);
        List<CustomRoleResponse> body = roles.stream()
                .map(r -> toResponse(r, null))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/custom-roles/{id}")
    public ResponseEntity<CustomRoleResponse> getCustomRole(@PathVariable Long id) {
        CustomRole role = customRoleService.getCustomRole(id);
        var effective = customRoleService.resolveCustomRolePermissions(id);
        return ResponseEntity.ok(toResponse(role, effective));
    }

    @PutMapping("/custom-roles/{id}")
    public ResponseEntity<CustomRoleResponse> updateCustomRole(
            @PathVariable Long id,
            @Valid @RequestBody CustomRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {

        Long updaterId = resolveNumericUserId(xUserId);
        CustomRole updated = customRoleService.updateCustomRole(id, request, updaterId);
        return ResponseEntity.ok(toResponse(updated, null));
    }

    @DeleteMapping("/custom-roles/{id}")
    public ResponseEntity<Map<String, String>> deleteCustomRole(@PathVariable Long id) {
        customRoleService.deleteCustomRole(id);
        return ResponseEntity.ok(Map.of("message", "Custom role deleted successfully"));
    }

    // =========================================================================
    // Role assignment
    // =========================================================================

    /**
     * Assign a custom role to a user.
     *
     * <p>
     * Request body: {@code {"userId": 42, "organizationId": 7}}
     * </p>
     */
    @PostMapping("/custom-roles/{id}/assign")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {

        Long targetUserId = body.get("userId");
        Long orgId = body.get("organizationId");

        if (targetUserId == null || orgId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "userId and organizationId are required"));
        }

        Long assignerUserId = resolveNumericUserId(xUserId);
        UserCustomRoleAssignment assignment = customRoleService.assignCustomRole(id, targetUserId, orgId,
                assignerUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Role assigned successfully",
                "assignmentId", assignment.getId(),
                "userId", targetUserId,
                "roleId", id));
    }

    /**
     * Revoke a custom role from a user.
     */
    @DeleteMapping("/custom-roles/{id}/assign/{userId}")
    public ResponseEntity<Map<String, String>> unassignRole(
            @PathVariable Long id,
            @PathVariable Long userId) {

        customRoleService.unassignCustomRole(id, userId);
        return ResponseEntity.ok(Map.of("message", "Role revoked successfully"));
    }

    /**
     * List all active custom roles assigned to a specific user.
     */
    @GetMapping("/users/{userId}/custom-roles")
    public ResponseEntity<List<Map<String, Object>>> getUserCustomRoles(@PathVariable Long userId) {
        List<UserCustomRoleAssignment> assignments = customRoleService.getAssignmentsForUser(userId);
        List<Map<String, Object>> result = assignments.stream().map(a -> {
            RbacRole role = a.getRbacRole();
            return Map.<String, Object>of(
                    "assignmentId", a.getId(),
                    "roleId", role.getId(),
                    "roleName", role.getRoleName(),
                    "displayName", role.getDisplayName(),
                    "permissions", role.getPermissionSet(),
                    "organizationId", a.getOrganizationId());
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Converts a {@link CustomRole} to its response DTO.
     * {@code effectivePermissions} may be {@code null} for list responses.
     */
    private CustomRoleResponse toResponse(CustomRole role, java.util.Set<String> effectivePermissions) {
        List<String> perms = role.getPermissions() == null || role.getPermissions().isBlank()
                ? List.of()
                : Arrays.stream(role.getPermissions().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

        return CustomRoleResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .organizationId(role.getOrganizationId())
                .permissions(perms)
                .effectivePermissions(effectivePermissions != null
                        ? new LinkedHashSet<>(effectivePermissions)
                        : null)
                .inheritsFrom(role.getInheritsFrom())
                .maxLevel(role.getMaxLevel())
                .createdBy(role.getCreatedBy())
                .build();
    }

    /**
     * Resolves the numeric {@code users.id} from the {@code X-User-Id} header
     * (which contains the username string).
     * Returns {@code null} if the header is absent or the user is not found.
     */
    private Long resolveNumericUserId(String username) {
        if (username == null || username.isBlank())
            return null;
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElse(null);
    }
}
