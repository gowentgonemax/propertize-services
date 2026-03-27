package com.propertize.platform.auth.controller;

import com.propertize.platform.auth.dto.*;
import com.propertize.platform.auth.entity.CompositeRole;
import com.propertize.platform.auth.entity.Delegation;
import com.propertize.platform.auth.entity.DelegationRule;
import com.propertize.platform.auth.entity.IpAccessRule;
import com.propertize.platform.auth.entity.IpRuleScope;
import com.propertize.platform.auth.entity.TemporalPermission;
import com.propertize.platform.auth.entity.CustomRole;
import com.propertize.platform.auth.entity.PermissionAuditLog;
import com.propertize.platform.auth.rbac.engine.evaluators.ConditionalPermissionEvaluator;
import com.propertize.platform.auth.rbac.engine.evaluators.DataScopeConditionEvaluator;
import com.propertize.platform.auth.rbac.engine.evaluators.TimeBasedConditionEvaluator;
import com.propertize.platform.auth.service.AuthorizationService;
import com.propertize.platform.auth.service.CustomRoleService;
import com.propertize.platform.auth.service.DelegationService;
import com.propertize.platform.auth.service.FieldLevelPermissionService;
import com.propertize.platform.auth.service.IpAccessService;
import com.propertize.platform.auth.service.PermissionAuditService;
import com.propertize.platform.auth.service.RbacConfigService;
import com.propertize.platform.auth.service.RbacService;
import com.propertize.platform.auth.service.RoleCompositionService;
import com.propertize.platform.auth.service.TemporalPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized RBAC API Controller.
 * This is the REST API that all other services call for authorization
 * decisions.
 *
 * Endpoints:
 * - POST /api/v1/auth/authorize → Check authorization
 * - GET /api/v1/auth/permissions/{role} → Get permissions for a role
 * - GET /api/v1/auth/roles → List all roles
 * - GET /api/v1/auth/roles/{role} → Get role details
 * - POST /api/v1/auth/permissions/check → Batch permission check
 * - GET /api/v1/auth/rbac/config → Get full RBAC config
 * - GET /api/v1/auth/rbac/endpoints → Get endpoint permission mappings
 * - POST /api/v1/auth/cache/invalidate → Invalidate permission caches
 *
 * @version 3.0 - Enhanced RBAC with ABAC, field-level, time-based, data scope,
 *          conditional permissions
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class RbacController {

    private final AuthorizationService authorizationService;
    private final RbacConfigService rbacConfigService;
    private final RbacService rbacService;
    private final TemporalPermissionService temporalPermissionService;
    private final RoleCompositionService roleCompositionService;
    private final DelegationService delegationService;
    private final CustomRoleService customRoleService;
    private final PermissionAuditService permissionAuditService;
    private final IpAccessService ipAccessService;
    private final FieldLevelPermissionService fieldLevelPermissionService;
    private final TimeBasedConditionEvaluator timeBasedConditionEvaluator;
    private final DataScopeConditionEvaluator dataScopeConditionEvaluator;
    private final ConditionalPermissionEvaluator conditionalPermissionEvaluator;

    /**
     * Authorize a request — the main authorization endpoint.
     * Other services call this to check if a user can perform an action.
     */
    @PostMapping("/authorize")
    public ResponseEntity<AuthorizationResponse> authorize(@RequestBody AuthorizationRequest request) {
        log.debug("Authorization request: {}", request);
        AuthorizationResponse response = authorizationService.authorize(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all permissions for a specific role.
     */
    @GetMapping("/permissions/{role}")
    public ResponseEntity<Map<String, Object>> getPermissionsForRole(@PathVariable String role) {
        Set<String> permissions = rbacService.getPermissionsForRole(role);
        if (permissions.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("role", role);
        response.put("permissions", permissions);
        response.put("scope", rbacService.getScopeForRole(role));
        response.put("count", permissions.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get permissions for multiple roles at once.
     */
    @PostMapping("/permissions/resolve")
    public ResponseEntity<Map<String, Object>> resolvePermissions(@RequestBody Map<String, List<String>> request) {
        List<String> roles = request.get("roles");
        if (roles == null || roles.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Set<String> allPermissions = authorizationService.getPermissionsForRoles(roles);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("roles", roles);
        response.put("permissions", allPermissions);
        response.put("count", allPermissions.size());
        response.put("bypassAllChecks", rbacService.shouldBypassAllChecks(roles));
        return ResponseEntity.ok(response);
    }

    /**
     * Batch permission check — check multiple permissions at once.
     */
    @PostMapping("/permissions/check")
    public ResponseEntity<Map<String, Boolean>> checkPermissions(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.get("roles");
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) request.get("permissions");

        Map<String, Boolean> results = authorizationService.checkPermissions(userId, roles, permissions);
        return ResponseEntity.ok(results);
    }

    /**
     * List all available roles.
     */
    @GetMapping("/roles")
    public ResponseEntity<Map<String, Object>> getAllRoles() {
        Set<String> roles = rbacConfigService.getAllRoles();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("roles", roles);
        response.put("count", roles.size());
        response.put("version", rbacConfigService.getConfigVersion());
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed info about a specific role.
     */
    @GetMapping("/roles/{role}")
    public ResponseEntity<Map<String, Object>> getRoleDetails(@PathVariable String role) {
        Map<String, Object> details = rbacConfigService.getRoleDetails(role);
        if (details.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        details.put("role", role);
        return ResponseEntity.ok(details);
    }

    /**
     * Get roles filtered by scope.
     */
    @GetMapping("/roles/scope/{scope}")
    public ResponseEntity<Map<String, Object>> getRolesByScope(@PathVariable String scope) {
        Set<String> roles = rbacConfigService.getRolesByScope(scope);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scope", scope);
        response.put("roles", roles);
        response.put("count", roles.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get full RBAC config (role → permissions map).
     */
    @GetMapping("/rbac/config")
    public ResponseEntity<Map<String, Object>> getRbacConfig() {
        Map<String, Set<String>> config = rbacConfigService.getRbacConfig();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("version", rbacConfigService.getConfigVersion());
        response.put("totalRoles", config.size());
        response.put("roles", config);
        return ResponseEntity.ok(response);
    }

    /**
     * Get endpoint permission mappings for API Gateway.
     */
    @GetMapping("/rbac/endpoints")
    public ResponseEntity<Map<String, Map<String, String>>> getEndpointPermissions() {
        return ResponseEntity.ok(rbacConfigService.getEndpointPermissions());
    }

    /**
     * Invalidate permission caches.
     */
    @PostMapping("/cache/invalidate")
    public ResponseEntity<Map<String, String>> invalidateCache(
            @RequestParam(required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            authorizationService.invalidateUserCache(userId);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Cache invalidated for user: " + userId));
        } else {
            authorizationService.invalidateAllCaches();
            return ResponseEntity.ok(Map.of("status", "ok", "message", "All caches invalidated"));
        }
    }

    // ========================================================================
    // Temporal Permission Endpoints (Phase 1: Time-Based Access Control)
    // ========================================================================

    /**
     * Grant a temporary permission to a user.
     *
     * Creates a time-bound permission that will automatically expire after
     * the specified duration. Requires a reason for audit trail purposes.
     *
     * @param request   the temporal permission grant request
     * @param grantorId the ID of the user granting the permission (from auth
     *                  header/context)
     * @return the created temporal permission details
     */
    @PostMapping("/temporal-permissions")
    public ResponseEntity<?> grantTemporalPermission(
            @Valid @RequestBody TemporalPermissionDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long grantorId) {
        log.info("Temporal permission grant request: permission={}, userId={}, durationMinutes={}",
                request.getPermission(), request.getUserId(), request.getDurationMinutes());

        if (grantorId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required to identify the grantor"));
        }

        try {
            Duration duration = Duration.ofMinutes(request.getDurationMinutes());
            TemporalPermission granted = temporalPermissionService.grantTemporaryPermission(
                    request.getUserId(),
                    request.getPermission(),
                    duration,
                    request.getReason(),
                    grantorId);

            TemporalPermissionResponse response = toResponse(granted);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid temporal permission request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all active temporal permissions for a specific user.
     *
     * @param userId the ID of the user to query permissions for
     * @return list of active temporal permissions
     */
    @GetMapping("/temporal-permissions/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserTemporalPermissions(@PathVariable Long userId) {
        log.debug("Fetching temporal permissions for user {}", userId);

        List<TemporalPermission> permissions = temporalPermissionService.getActivePermissionsForUser(userId);

        List<TemporalPermissionResponse> responseList = permissions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("activePermissions", responseList);
        response.put("count", responseList.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke a temporal permission.
     *
     * @param id        the ID of the temporal permission to revoke
     * @param revokedBy the ID of the user performing the revocation
     * @return success message or error
     */
    @DeleteMapping("/temporal-permissions/{id}")
    public ResponseEntity<Map<String, String>> revokeTemporalPermission(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long revokedBy) {
        log.info("Revoke request for temporal permission id={} by user {}", id, revokedBy);

        if (revokedBy == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required to identify the revoker"));
        }

        try {
            temporalPermissionService.revokePermission(id, revokedBy);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "Temporal permission " + id + " revoked successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to revoke temporal permission {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all active temporal permissions granted by a specific user.
     *
     * @param grantorId the ID of the user who granted the permissions
     * @return list of temporal permissions granted by the user
     */
    @GetMapping("/temporal-permissions/granted-by/{grantorId}")
    public ResponseEntity<Map<String, Object>> getPermissionsGrantedBy(@PathVariable Long grantorId) {
        log.debug("Fetching temporal permissions granted by user {}", grantorId);

        List<TemporalPermission> permissions = temporalPermissionService.getPermissionsGrantedBy(grantorId);

        List<TemporalPermissionResponse> responseList = permissions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("grantedBy", grantorId);
        response.put("permissions", responseList);
        response.put("count", responseList.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Convert a TemporalPermission entity to a TemporalPermissionResponse DTO.
     */
    private TemporalPermissionResponse toResponse(TemporalPermission entity) {
        return TemporalPermissionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .permission(entity.getPermission())
                .grantedBy(entity.getGrantedBy())
                .grantedAt(entity.getGrantedAt())
                .expiresAt(entity.getExpiresAt())
                .reason(entity.getReason())
                .isActive(entity.isActive())
                .build();
    }

    // ========================================================================
    // Composite Role Endpoints (Phase 3: Dynamic Role Composition)
    // ========================================================================

    /**
     * Create a new composite role.
     *
     * Composite roles combine multiple base roles into a single assignable
     * unit. The effective permission set is the union of all component
     * role permissions.
     *
     * @param request   the composite role creation request
     * @param creatorId the ID of the user creating the composite role (from auth
     *                  header)
     * @return the created composite role with resolved permissions
     */
    @PostMapping("/composite-roles")
    public ResponseEntity<?> createCompositeRole(
            @Valid @RequestBody CompositeRoleDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long creatorId) {
        log.info("Create composite role request: name={}, components={}",
                request.getName(), request.getComponentRoles());

        if (creatorId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required to identify the creator"));
        }

        try {
            CompositeRole created = roleCompositionService.createCompositeRole(
                    request.getName(),
                    request.getDescription(),
                    request.getComponentRoles(),
                    creatorId,
                    request.getOrganizationId());

            Set<String> effectivePermissions = roleCompositionService
                    .resolveEffectivePermissions(new LinkedHashSet<>(created.getComponentRoles()));

            CompositeRoleResponse response = toCompositeRoleResponse(created, effectivePermissions);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid composite role request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List all active composite roles.
     * Optionally filter by organization ID using the query parameter.
     *
     * @param orgId optional organization ID to filter by
     * @return list of active composite roles
     */
    @GetMapping("/composite-roles")
    public ResponseEntity<Map<String, Object>> listCompositeRoles(
            @RequestParam(value = "orgId", required = false) Long orgId) {
        log.debug("Listing composite roles (orgId={})", orgId);

        List<CompositeRole> compositeRoles = roleCompositionService.getCompositeRoles(orgId);

        List<CompositeRoleResponse> responseList = compositeRoles.stream()
                .map(cr -> toCompositeRoleResponse(cr, null))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("compositeRoles", responseList);
        response.put("count", responseList.size());
        if (orgId != null) {
            response.put("organizationId", orgId);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific composite role with its resolved effective permissions.
     *
     * @param id the composite role ID
     * @return the composite role with effective permissions, or 404
     */
    @GetMapping("/composite-roles/{id}")
    public ResponseEntity<?> getCompositeRole(@PathVariable Long id) {
        log.debug("Fetching composite role id={}", id);

        Optional<CompositeRole> optionalRole = roleCompositionService.getCompositeRoleById(id);
        if (optionalRole.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Composite role not found: " + id));
        }

        CompositeRole compositeRole = optionalRole.get();
        Set<String> effectivePermissions = roleCompositionService
                .resolveEffectivePermissions(new LinkedHashSet<>(compositeRole.getComponentRoles()));

        CompositeRoleResponse response = toCompositeRoleResponse(compositeRole, effectivePermissions);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate (soft-delete) a composite role.
     *
     * @param id the composite role ID
     * @return success message or error
     */
    @DeleteMapping("/composite-roles/{id}")
    public ResponseEntity<Map<String, String>> deactivateCompositeRole(@PathVariable Long id) {
        log.info("Deactivate request for composite role id={}", id);

        try {
            roleCompositionService.deactivateCompositeRole(id);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "Composite role " + id + " deactivated successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to deactivate composite role {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage()));
        }
    }

    /**
     * Resolve effective permissions for an ad-hoc set of roles.
     *
     * <p>
     * Accepts a JSON body containing a list of role names and returns the
     * union of all their permissions, along with the effective scope and
     * conflict resolution results.
     * </p>
     *
     * @param request body containing "roles" list
     * @return resolved permissions, scope, and conflict info
     */
    @PostMapping("/resolve-permissions")
    public ResponseEntity<?> resolveComposedPermissions(@RequestBody Map<String, List<String>> request) {
        List<String> roles = request.get("roles");
        if (roles == null || roles.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "A non-empty 'roles' list is required"));
        }

        Set<String> roleSet = new LinkedHashSet<>(roles);
        Set<String> effectivePermissions = roleCompositionService.resolveEffectivePermissions(roleSet);
        Set<String> resolvedRoles = roleCompositionService.resolveRoleConflicts(roleSet);
        String effectiveScope = roleCompositionService.getEffectiveScope(roleSet);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestedRoles", roles);
        response.put("resolvedRoles", resolvedRoles);
        response.put("effectivePermissions", effectivePermissions);
        response.put("permissionCount", effectivePermissions.size());
        response.put("effectiveScope", effectiveScope);
        return ResponseEntity.ok(response);
    }

    /**
     * Convert a CompositeRole entity to a CompositeRoleResponse DTO.
     */
    private CompositeRoleResponse toCompositeRoleResponse(CompositeRole entity, Set<String> effectivePermissions) {
        return CompositeRoleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .componentRoles(entity.getComponentRoles())
                .effectivePermissions(effectivePermissions)
                .createdAt(entity.getCreatedAt())
                .isActive(entity.isActive())
                .build();
    }

    // ========================================================================
    // Permission Delegation Endpoints (Phase 3: Permission Delegation)
    // ========================================================================

    /**
     * Delegate a permission to another user.
     *
     * The delegator (identified by X-User-Id header) grants a specific
     * permission to a delegate user for a limited duration. The delegation
     * is validated against configured delegation rules.
     *
     * @param request     the delegation request details
     * @param delegatorId the ID of the delegating user (from auth header)
     * @return the created delegation details
     */
    @PostMapping("/delegations")
    public ResponseEntity<?> delegatePermission(
            @Valid @RequestBody DelegationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long delegatorId) {
        log.info("Delegation request: delegator={}, delegate={}, permission={}, durationHours={}",
                delegatorId, request.getDelegateUserId(), request.getPermission(), request.getDurationHours());

        if (delegatorId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required to identify the delegator"));
        }

        try {
            Delegation delegation = delegationService.delegatePermission(
                    delegatorId,
                    request.getDelegateUserId(),
                    request.getPermission(),
                    request.getDurationHours(),
                    request.getReason(),
                    request.getDelegatorRole(),
                    request.getDelegateRole(),
                    request.getParentDelegationId(),
                    request.getOrganizationId());

            DelegationResponse response = toDelegationResponse(delegation);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid delegation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all delegations created by the current user (outgoing).
     *
     * @param userId the current user's ID (from auth header)
     * @return list of delegations the user has created
     */
    @GetMapping("/delegations/by-me")
    public ResponseEntity<?> getMyDelegations(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required"));
        }

        List<Delegation> delegations = delegationService.getMyDelegations(userId);
        List<DelegationResponse> responseList = delegations.stream()
                .map(this::toDelegationResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("direction", "outgoing");
        response.put("delegations", responseList);
        response.put("count", responseList.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all delegations granted to the current user (incoming).
     *
     * @param userId the current user's ID (from auth header)
     * @return list of delegations granted to the user
     */
    @GetMapping("/delegations/to-me")
    public ResponseEntity<?> getDelegationsToMe(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required"));
        }

        List<Delegation> delegations = delegationService.getDelegationsToMe(userId);
        List<DelegationResponse> responseList = delegations.stream()
                .map(this::toDelegationResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("direction", "incoming");
        response.put("delegations", responseList);
        response.put("count", responseList.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke an active delegation.
     *
     * @param id        the delegation ID to revoke
     * @param revokedBy the ID of the user performing the revocation
     * @return success or error message
     */
    @DeleteMapping("/delegations/{id}")
    public ResponseEntity<Map<String, String>> revokeDelegation(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long revokedBy) {
        log.info("Revoke delegation request: id={}, revokedBy={}", id, revokedBy);

        if (revokedBy == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required to identify the revoker"));
        }

        try {
            delegationService.revokeDelegation(id, revokedBy);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "Delegation " + id + " revoked successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to revoke delegation {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get available delegation rules.
     *
     * Returns all active delegation rules, optionally filtered by the
     * caller's role. Useful for UI to show what permissions a user can delegate.
     *
     * @param role optional role filter — if provided, returns only the rule for
     *             that role
     * @return list of delegation rules
     */
    @GetMapping("/delegation-rules")
    public ResponseEntity<Map<String, Object>> getDelegationRules(
            @RequestParam(value = "role", required = false) String role) {
        log.debug("Fetching delegation rules (role={})", role);

        Map<String, Object> response = new LinkedHashMap<>();

        if (role != null && !role.isBlank()) {
            DelegationRule rule = delegationService.getDelegationRuleForRole(role);
            if (rule == null) {
                response.put("role", role);
                response.put("rules", List.of());
                response.put("count", 0);
                response.put("message", "No delegation rules found for role: " + role);
            } else {
                response.put("role", role);
                response.put("rules", List.of(toDelegationRuleMap(rule)));
                response.put("count", 1);
            }
        } else {
            List<DelegationRule> rules = delegationService.getActiveDelegationRules();
            List<Map<String, Object>> ruleList = rules.stream()
                    .map(this::toDelegationRuleMap)
                    .collect(Collectors.toList());
            response.put("rules", ruleList);
            response.put("count", ruleList.size());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Convert a Delegation entity to a DelegationResponse DTO.
     */
    private DelegationResponse toDelegationResponse(Delegation entity) {
        return DelegationResponse.builder()
                .id(entity.getId())
                .delegatorUserId(entity.getDelegatorUserId())
                .delegateUserId(entity.getDelegateUserId())
                .permission(entity.getPermission())
                .grantedAt(entity.getGrantedAt())
                .expiresAt(entity.getExpiresAt())
                .status(entity.getStatus())
                .reason(entity.getReason())
                .parentDelegationId(entity.getParentDelegationId())
                .organizationId(entity.getOrganizationId())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .revokedBy(entity.getRevokedBy())
                .revokedAt(entity.getRevokedAt())
                .build();
    }

    /**
     * Convert a DelegationRule entity to a Map for JSON response.
     */
    private Map<String, Object> toDelegationRuleMap(DelegationRule rule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId());
        map.put("delegatorRole", rule.getDelegatorRole());
        map.put("delegatablePermissions", List.of(rule.getDelegatablePermissions().split(",")));
        map.put("allowedDelegateRoles", List.of(rule.getAllowedDelegateRoles().split(",")));
        map.put("maxDurationHours", rule.getMaxDurationHours());
        map.put("requiresReason", rule.isRequiresReason());
        map.put("requiresApproval", rule.isRequiresApproval());
        map.put("maxChainDepth", rule.getMaxChainDepth());
        return map;
    }

    // ========================================================================
    // Custom Role Endpoints (Phase 4a: Custom Role Builder)
    // ========================================================================

    /**
     * Create a new custom role for an organization.
     *
     * <p>
     * Custom roles allow organizations to create tailored permission profiles
     * scoped to their org. The creator's permissions and privilege level are used
     * as upper bounds for what the custom role can include.
     * </p>
     *
     * @param request   the custom role creation request
     * @param creatorId the ID of the user creating the custom role (from auth
     *                  header)
     * @return the created custom role with resolved permissions
     */
    @PostMapping("/custom-roles")
    public ResponseEntity<?> createCustomRole(
            @Valid @RequestBody CustomRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long creatorId) {
        log.info("Create custom role request: roleName={}, orgId={}, inheritsFrom={}",
                request.getRoleName(), request.getOrganizationId(), request.getInheritsFrom());

        if (creatorId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required to identify the creator"));
        }

        try {
            CustomRole created = customRoleService.createCustomRole(request, creatorId);
            Set<String> effectivePermissions = customRoleService.resolveCustomRolePermissions(created.getId());
            CustomRoleResponse response = toCustomRoleResponse(created, effectivePermissions);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid custom role request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List all active custom roles for an organization.
     *
     * @param orgId the organization ID to filter by (required)
     * @return list of active custom roles for the organization
     */
    @GetMapping("/custom-roles")
    public ResponseEntity<?> listCustomRoles(
            @RequestParam(value = "orgId") Long orgId) {
        log.debug("Listing custom roles for org {}", orgId);

        List<CustomRole> customRoles = customRoleService.getCustomRoles(orgId);

        List<CustomRoleResponse> responseList = customRoles.stream()
                .map(cr -> toCustomRoleResponse(cr, null))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("organizationId", orgId);
        response.put("customRoles", responseList);
        response.put("count", responseList.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific custom role with its resolved effective permissions.
     *
     * @param id the custom role ID
     * @return the custom role with effective permissions, or 404
     */
    @GetMapping("/custom-roles/{id}")
    public ResponseEntity<?> getCustomRole(@PathVariable Long id) {
        log.debug("Fetching custom role id={}", id);

        try {
            CustomRole customRole = customRoleService.getCustomRole(id);
            Set<String> effectivePermissions = customRoleService.resolveCustomRolePermissions(id);
            CustomRoleResponse response = toCustomRoleResponse(customRole, effectivePermissions);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update an existing custom role.
     *
     * <p>
     * Same validation rules as creation apply. System roles cannot be updated.
     * </p>
     *
     * @param id        the custom role ID
     * @param request   the update request
     * @param updaterId the ID of the user performing the update (from auth header)
     * @return the updated custom role with resolved permissions
     */
    @PutMapping("/custom-roles/{id}")
    public ResponseEntity<?> updateCustomRole(
            @PathVariable Long id,
            @Valid @RequestBody CustomRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long updaterId) {
        log.info("Update custom role request: id={}, roleName={}", id, request.getRoleName());

        if (updaterId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required to identify the updater"));
        }

        try {
            CustomRole updated = customRoleService.updateCustomRole(id, request, updaterId);
            Set<String> effectivePermissions = customRoleService.resolveCustomRolePermissions(updated.getId());
            CustomRoleResponse response = toCustomRoleResponse(updated, effectivePermissions);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Failed to update custom role {}: {}", id, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Soft-delete a custom role.
     *
     * <p>
     * System roles cannot be deleted.
     * </p>
     *
     * @param id the custom role ID
     * @return success message or error
     */
    @DeleteMapping("/custom-roles/{id}")
    public ResponseEntity<Map<String, String>> deleteCustomRole(@PathVariable Long id) {
        log.info("Delete custom role request: id={}", id);

        try {
            customRoleService.deleteCustomRole(id);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "Custom role " + id + " deleted successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete custom role {}: {}", id, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Convert a CustomRole entity to a CustomRoleResponse DTO.
     */
    private CustomRoleResponse toCustomRoleResponse(CustomRole entity, Set<String> effectivePermissions) {
        List<String> permissionList = (entity.getPermissions() != null && !entity.getPermissions().isBlank())
                ? Arrays.stream(entity.getPermissions().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                : List.of();

        return CustomRoleResponse.builder()
                .id(entity.getId())
                .roleName(entity.getRoleName())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .organizationId(entity.getOrganizationId())
                .permissions(permissionList)
                .effectivePermissions(effectivePermissions)
                .inheritsFrom(entity.getInheritsFrom())
                .maxLevel(entity.getMaxLevel())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isActive(entity.isActive())
                .build();
    }

    // ========================================================================
    // Permission Audit Trail Endpoints (Phase 4b: Permission Audit Trail)
    // ========================================================================

    /**
     * Query audit logs for a specific user within a date range.
     *
     * <p>
     * Supports compliance queries such as "Show me all activity for
     * user X between date A and date B".
     * </p>
     *
     * @param userId the user ID to query audit logs for
     * @param from   start of the date range (ISO 8601)
     * @param to     end of the date range (ISO 8601)
     * @return list of matching audit log entries
     */
    @GetMapping("/rbac/audit-logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.debug("Fetching audit logs for userId={} from={} to={}", userId, from, to);

        List<PermissionAuditLog> logs = permissionAuditService.getAuditLogs(userId, from, to);
        List<AuditLogResponse> responseList = logs.stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("from", from);
        response.put("to", to);
        response.put("logs", responseList);
        response.put("count", responseList.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get recent access denials for a specific user.
     *
     * <p>
     * Supports compliance queries such as "Show me all access denials
     * for user X in the last 7 days".
     * </p>
     *
     * @param userId the user ID
     * @param since  only return denials after this timestamp (ISO 8601)
     * @return list of denied audit log entries
     */
    @GetMapping("/rbac/audit-logs/denials")
    public ResponseEntity<Map<String, Object>> getAuditDenials(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        log.debug("Fetching audit denials for userId={} since={}", userId, since);

        List<PermissionAuditLog> denials = permissionAuditService.getDenials(userId, since);
        List<AuditLogResponse> responseList = denials.stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("since", since);
        response.put("denials", responseList);
        response.put("count", responseList.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get an audit summary for an organization within a date range.
     *
     * <p>
     * Supports compliance reports such as "Who accessed financial data
     * this month?" and executive dashboards showing allow/deny ratios.
     * </p>
     *
     * @param orgId the organization ID
     * @param from  start of the reporting period (ISO 8601)
     * @param to    end of the reporting period (ISO 8601)
     * @return audit summary with totals and top-N lists
     */
    @GetMapping("/rbac/audit-logs/summary")
    public ResponseEntity<AuditSummaryDTO> getAuditSummary(
            @RequestParam Long orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.debug("Fetching audit summary for orgId={} from={} to={}", orgId, from, to);

        AuditSummaryDTO summary = permissionAuditService.getAuditSummary(orgId, from, to);
        return ResponseEntity.ok(summary);
    }

    /**
     * Convert a PermissionAuditLog entity to an AuditLogResponse DTO.
     */
    private AuditLogResponse toAuditLogResponse(PermissionAuditLog entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .action(entity.getAction())
                .permission(entity.getPermission())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .result(entity.getResult())
                .reason(entity.getReason())
                .ipAddress(entity.getIpAddress())
                .requestPath(entity.getRequestPath())
                .requestMethod(entity.getRequestMethod())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // ========================================================================
    // IP/Geo-Location Access Control Endpoints (Phase 4c)
    // ========================================================================

    /**
     * Create a new IP access rule.
     *
     * <p>
     * Supports whitelist/blacklist rules at global, organization, role,
     * or user scope. CIDR notation, exact IPs, and wildcards are supported.
     * </p>
     *
     * @param request   the IP access rule creation request
     * @param createdBy the ID of the user creating the rule (from auth header)
     * @return the created IP access rule
     */
    @PostMapping("/rbac/ip-rules")
    public ResponseEntity<?> createIpAccessRule(
            @Valid @RequestBody IpAccessRuleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long createdBy) {
        log.info("Create IP access rule request: type={}, scope={}, pattern={}",
                request.getRuleType(), request.getScope(), request.getIpPattern());

        if (createdBy == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "X-User-Id header is required to identify the creator"));
        }

        try {
            IpAccessRule created = ipAccessService.createRule(request, createdBy);
            IpAccessRuleResponse response = toIpAccessRuleResponse(created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid IP access rule request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List IP access rules, optionally filtered by scope and scope value.
     *
     * @param scope      the rule scope to filter by
     * @param scopeValue the scope value to filter by (optional)
     * @return list of matching IP access rules
     */
    @GetMapping("/rbac/ip-rules")
    public ResponseEntity<Map<String, Object>> listIpAccessRules(
            @RequestParam IpRuleScope scope,
            @RequestParam(required = false) String scopeValue) {
        log.debug("Listing IP access rules: scope={}, scopeValue={}", scope, scopeValue);

        List<IpAccessRule> rules = ipAccessService.getRules(scope, scopeValue);
        List<IpAccessRuleResponse> responseList = rules.stream()
                .map(this::toIpAccessRuleResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scope", scope);
        response.put("scopeValue", scopeValue);
        response.put("rules", responseList);
        response.put("count", responseList.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete (deactivate) an IP access rule.
     *
     * @param id the ID of the rule to delete
     * @return success message or error
     */
    @DeleteMapping("/rbac/ip-rules/{id}")
    public ResponseEntity<Map<String, String>> deleteIpAccessRule(@PathVariable Long id) {
        log.info("Delete IP access rule request: id={}", id);

        try {
            ipAccessService.deleteRule(id);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "IP access rule " + id + " deleted successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete IP access rule {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if an IP address is allowed for a given user context.
     *
     * <p>
     * Useful for testing rules before applying them or for
     * integrating IP checks from other services.
     * </p>
     *
     * @param request the IP access check request
     * @return whether the IP is allowed and the matching rule
     */
    @PostMapping("/rbac/ip-rules/check")
    public ResponseEntity<IpAccessCheckResponse> checkIpAccess(
            @Valid @RequestBody IpAccessCheckRequest request) {
        log.debug("IP access check request: ip={}, userId={}, roles={}, orgId={}",
                request.getIpAddress(), request.getUserId(), request.getRoles(), request.getOrgId());

        boolean allowed = ipAccessService.isIpAllowed(
                request.getIpAddress(),
                request.getUserId(),
                request.getRoles(),
                request.getOrgId());

        IpAccessCheckResponse response = IpAccessCheckResponse.builder()
                .ipAddress(request.getIpAddress())
                .allowed(allowed)
                .reason(allowed ? "IP is permitted" : "IP is blocked by an active rule")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Convert an IpAccessRule entity to an IpAccessRuleResponse DTO.
     */
    private IpAccessRuleResponse toIpAccessRuleResponse(IpAccessRule entity) {
        return IpAccessRuleResponse.builder()
                .id(entity.getId())
                .ruleType(entity.getRuleType())
                .ipPattern(entity.getIpPattern())
                .description(entity.getDescription())
                .scope(entity.getScope())
                .scopeValue(entity.getScopeValue())
                .isActive(entity.isActive())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }

    // ========================================================================
    // Field-Level Permission Endpoints (Phase 1: Field-Level Security)
    // ========================================================================

    /**
     * Get field-level permissions for a resource and role.
     * Returns visible and hidden fields.
     */
    @GetMapping("/fields/{resource}/{role}")
    public ResponseEntity<Map<String, Object>> getFieldPermissions(
            @PathVariable String resource, @PathVariable String role) {
        Set<String> visible = fieldLevelPermissionService.getVisibleFields(resource, role);
        Set<String> hidden = fieldLevelPermissionService.getHiddenFields(resource, role);

        if (visible == null && hidden.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "resource", resource, "role", role,
                    "unrestricted", true,
                    "message", "No field-level restrictions defined"));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resource", resource);
        response.put("role", role);
        response.put("unrestricted", false);
        response.put("visibleFields", visible != null ? visible : Collections.emptySet());
        response.put("hiddenFields", hidden);
        return ResponseEntity.ok(response);
    }

    /**
     * Get field access for a resource given multiple roles.
     * Uses most-permissive union of visible / intersection of hidden.
     */
    @PostMapping("/fields/resolve")
    public ResponseEntity<Map<String, Object>> resolveFieldAccess(
            @RequestBody Map<String, Object> request) {
        String resource = (String) request.get("resource");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.get("roles");

        if (resource == null || roles == null || roles.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "resource and roles are required"));
        }

        FieldLevelPermissionService.FieldAccessResult result = fieldLevelPermissionService.getFieldAccess(resource,
                roles);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resource", resource);
        response.put("roles", roles);
        response.put("unrestricted", result.unrestricted());
        response.put("visibleFields", result.visibleFields());
        response.put("hiddenFields", result.hiddenFields());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all resources that have field-level permissions configured.
     */
    @GetMapping("/fields/resources")
    public ResponseEntity<Map<String, Object>> getFieldPermissionResources() {
        Set<String> resources = fieldLevelPermissionService.getConfiguredResources();
        return ResponseEntity.ok(Map.of("resources", resources, "count", resources.size()));
    }

    // ========================================================================
    // Data Scope Resolution Endpoints (Phase 2: Row-Level Security)
    // ========================================================================

    /**
     * Resolve the effective data scope for a role and resource type.
     * Returns the scope rule for the requested role/resource combination.
     */
    @PostMapping("/scope/resolve")
    public ResponseEntity<Map<String, Object>> resolveDataScope(
            @RequestBody Map<String, Object> request) {
        String role = (String) request.get("role");
        String resourceType = (String) request.get("resourceType");

        if (role == null || resourceType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "role and resourceType are required"));
        }

        String scopeRule = dataScopeConditionEvaluator.resolveScope(role, resourceType);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("role", role);
        response.put("resourceType", resourceType);
        response.put("scopeRule", scopeRule != null ? scopeRule : "unrestricted");
        response.put("restricted", scopeRule != null);
        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // Conditional Permission Endpoints (Phase 2: Amount/Value Limits)
    // ========================================================================

    /**
     * Get conditional permission limits for a role and permission.
     */
    @GetMapping("/conditions/{role}/{permission}")
    public ResponseEntity<Map<String, Object>> getConditionalPermissions(
            @PathVariable String role, @PathVariable String permission) {
        Map<String, Object> conditions = conditionalPermissionEvaluator.getConditions(role, permission);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("role", role);
        response.put("permission", permission);
        if (conditions != null) {
            response.put("conditions", conditions);
            response.put("hasConditions", true);
        } else {
            response.put("hasConditions", false);
            response.put("message", "No conditional restrictions defined");
        }
        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // Time Restriction Endpoints (Phase 1: Time-Based Access Control)
    // ========================================================================

    /**
     * Check if access is currently allowed for a set of roles based on time
     * restrictions.
     */
    @PostMapping("/time/check")
    public ResponseEntity<Map<String, Object>> checkTimeAccess(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.get("roles");

        if (roles == null || roles.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "roles are required"));
        }

        boolean allowed = timeBasedConditionEvaluator.isAccessAllowed(new LinkedHashSet<>(roles));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("roles", roles);
        response.put("allowed", allowed);
        response.put("checkedAt", java.time.Instant.now().toString());
        if (!allowed) {
            response.put("reason", "Access denied: outside allowed time window for one or more roles");
        }
        return ResponseEntity.ok(response);
    }
}
