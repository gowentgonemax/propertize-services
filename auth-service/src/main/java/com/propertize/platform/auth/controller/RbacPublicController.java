package com.propertize.platform.auth.controller;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.dto.RoleDto;
import com.propertize.platform.auth.entity.RbacRole;
import com.propertize.platform.auth.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Public RBAC endpoint — no authentication required.
 *
 * <p>
 * Exposes the role catalogue and full permission list so the frontend can
 * load them once at startup instead of relying on hardcoded constants.
 * </p>
 *
 * <ul>
 * <li>GET /api/v1/rbac/roles — full list of {@link RoleDto}, sorted by level
 * desc</li>
 * <li>GET /api/v1/rbac/permissions — deduplicated, sorted list of every
 * permission string</li>
 * </ul>
 *
 * Security: these endpoints are declared {@code permitAll()} in
 * {@link com.propertize.platform.auth.config.SecurityConfig}.
 * They expose no personal data — only the static RBAC schema.
 */
@RestController
@RequestMapping("/api/v1/rbac")
@RequiredArgsConstructor
@Slf4j
public class RbacPublicController {

    private final RbacConfig rbacConfig;
    private final RbacService rbacService;

    /**
     * Returns all roles defined in {@code rbac.yml}, enriched with labels and
     * fully-expanded permission sets, sorted by privilege level (highest first).
     *
     * <p>
     * Role names are lowercased to match the format stored in JWTs
     * (e.g. YAML key {@code PLATFORM_OVERSIGHT} → name {@code platform_oversight}).
     * </p>
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> getRoles() {
        // Prefer DB-backed system roles (populated by RbacSeederService at startup)
        List<RbacRole> dbRoles = rbacService.getSystemRolesFromDb();
        if (!dbRoles.isEmpty()) {
            List<RoleDto> roles = dbRoles.stream()
                    .map(r -> RoleDto.builder()
                            .name(r.getRoleName().toLowerCase())
                            .label(r.getDisplayName())
                            .description(r.getDescription())
                            .scope(r.getScope())
                            .level(r.getLevel())
                            .category(r.getCategory())
                            .permissions(new ArrayList<>(
                                    rbacService.getPermissionsForRole(r.getRoleName())))
                            .build())
                    .sorted(Comparator.comparingInt(RoleDto::getLevel).reversed())
                    .collect(Collectors.toList());
            log.debug("📋 /api/v1/rbac/roles — returning {} roles (DB)", roles.size());
            return ResponseEntity.ok(roles);
        }

        // Fallback: read directly from YAML config (e.g. first boot before seeder runs)
        if (rbacConfig.getRoles() == null) {
            log.warn("⚠️ RBAC roles config is null — returning empty list");
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<RoleDto> roles = rbacConfig.getRoles().entrySet().stream()
                .map(entry -> {
                    String yamlKey = entry.getKey();
                    String name = yamlKey.toLowerCase();
                    String label = toTitleCase(name);
                    RbacConfig.RoleConfig cfg = entry.getValue();
                    return RoleDto.builder()
                            .name(name)
                            .label(label)
                            .description(cfg.getDescription())
                            .scope(cfg.getScope())
                            .level(cfg.getLevel() != null ? cfg.getLevel() : 0)
                            .category(cfg.getCategory())
                            .permissions(new ArrayList<>(rbacService.getPermissionsForRole(yamlKey)))
                            .build();
                })
                .sorted(Comparator.comparingInt(RoleDto::getLevel).reversed())
                .collect(Collectors.toList());

        log.debug("📋 /api/v1/rbac/roles — returning {} roles (YAML fallback)", roles.size());
        return ResponseEntity.ok(roles);
    }

    /**
     * Returns a sorted, deduplicated list of every permission string that exists
     * across all roles (after wildcard expansion and inheritance resolution).
     */
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> getAllPermissions() {
        Set<String> allPermissions = new TreeSet<>();

        if (rbacConfig.getRoles() != null) {
            rbacConfig.getRoles().keySet()
                    .forEach(role -> allPermissions.addAll(rbacService.getPermissionsForRole(role)));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("permissions", allPermissions);
        response.put("count", allPermissions.size());

        log.debug("📋 /api/v1/rbac/permissions — returning {} permissions", allPermissions.size());
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a lowercase_underscore string to Title Case with spaces.
     *
     * <p>
     * Examples:
     * 
     * <pre>
     *   "platform_oversight"      → "Platform Oversight"
     *   "maintenance_technician"  → "Maintenance Technician"
     * </pre>
     */
    private String toTitleCase(String underscoreName) {
        return Arrays.stream(underscoreName.split("_"))
                .map(word -> word.isEmpty()
                        ? ""
                        : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
