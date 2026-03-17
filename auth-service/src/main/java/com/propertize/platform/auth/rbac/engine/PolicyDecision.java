package com.propertize.platform.auth.rbac.engine;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Policy evaluation decision with detailed reasoning.
 * Immutable result of authorization check.
 *
 * @version 2.0 - Centralized in auth-service
 */
@Data
@Builder
public class PolicyDecision {

    private final boolean allowed;
    private final String reason;
    private final String policyId;
    private final String configVersion;

    @Singular("matchedPermission")
    private final List<String> matchedPermissions;

    @Singular("evaluatedRole")
    private final List<String> evaluatedRoles;

    private final long evaluationTimeMs;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    private final Map<String, Object> debugContext;
    private final String resource;
    private final String action;
    private final String userId;
    private final String organizationId;

    @Singular("conditionResult")
    private final Map<String, Boolean> conditionResults;

    private final boolean fromCache;

    public static PolicyDecision allow(String reason) {
        return PolicyDecision.builder().allowed(true).reason(reason).build();
    }

    public static PolicyDecision deny(String reason) {
        return PolicyDecision.builder().allowed(false).reason(reason).build();
    }

    public static PolicyDecision allowWithPermissions(String reason, List<String> permissions) {
        return PolicyDecision.builder()
                .allowed(true)
                .reason(reason)
                .matchedPermissions(permissions)
                .build();
    }

    public boolean isPlatformAdminBypass() {
        return allowed && matchedPermissions != null &&
                matchedPermissions.stream().anyMatch(p -> p.equals("*") || p.contains("admin:all"));
    }

    public String getSummary() {
        return String.format("[%s] %s:%s for user %s in org %s - %s (took %dms)",
                allowed ? "ALLOW" : "DENY", resource, action, userId, organizationId, reason, evaluationTimeMs);
    }

    public Map<String, Object> toAuditLog() {
        return Map.ofEntries(
                Map.entry("allowed", allowed),
                Map.entry("reason", reason != null ? reason : ""),
                Map.entry("resource", resource != null ? resource : ""),
                Map.entry("action", action != null ? action : ""),
                Map.entry("userId", userId != null ? userId : ""),
                Map.entry("organizationId", organizationId != null ? organizationId : ""),
                Map.entry("matchedPermissions", matchedPermissions != null ? matchedPermissions : List.of()),
                Map.entry("evaluatedRoles", evaluatedRoles != null ? evaluatedRoles : List.of()),
                Map.entry("evaluationTimeMs", evaluationTimeMs),
                Map.entry("timestamp", timestamp.toString()),
                Map.entry("configVersion", configVersion != null ? configVersion : ""),
                Map.entry("policyId", policyId != null ? policyId : ""),
                Map.entry("fromCache", fromCache));
    }
}
