package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.AuditSummaryDTO;
import com.propertize.platform.auth.entity.AuditAction;
import com.propertize.platform.auth.entity.AuditResult;
import com.propertize.platform.auth.entity.PermissionAuditLog;
import com.propertize.platform.auth.repository.PermissionAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for recording and querying permission audit trail entries.
 *
 * <p>
 * All write operations are executed asynchronously via {@link Async}
 * so that audit logging does not block the request processing pipeline.
 * Read operations are performed with {@code readOnly} transactions for
 * optimal database performance.
 * </p>
 *
 * @version 1.0 - Phase 4b: Permission Audit Trail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionAuditService {

    private final PermissionAuditLogRepository auditLogRepository;

    // ========================================================================
    // Async log-write methods
    // ========================================================================

    /**
     * Log a permission check (authorization decision).
     *
     * @param userId       the user whose permission was checked
     * @param username     the username (denormalized for fast reads)
     * @param permission   the permission string that was checked
     * @param result       ALLOWED or DENIED
     * @param resourceType optional resource type (e.g., "PROPERTY")
     * @param resourceId   optional resource identifier
     * @param request      the originating HTTP request (may be {@code null} for
     *                     background tasks)
     */
    @Async
    public void logPermissionCheck(Long userId, String username, String permission,
            AuditResult result, String resourceType,
            String resourceId, HttpServletRequest request) {
        try {
            PermissionAuditLog entry = PermissionAuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(AuditAction.PERMISSION_CHECK)
                    .permission(permission)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .result(result)
                    .ipAddress(extractIpAddress(request))
                    .userAgent(extractHeader(request, "User-Agent"))
                    .requestPath(extractRequestPath(request))
                    .requestMethod(extractRequestMethod(request))
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write permission-check audit log for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    /**
     * Log a permission grant event.
     *
     * @param userId     the user receiving the permission
     * @param username   the username
     * @param permission the permission being granted
     * @param grantedBy  the ID of the user who granted the permission
     */
    @Async
    public void logPermissionGrant(Long userId, String username,
            String permission, Long grantedBy) {
        try {
            PermissionAuditLog entry = PermissionAuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(AuditAction.PERMISSION_GRANT)
                    .permission(permission)
                    .result(AuditResult.NOT_APPLICABLE)
                    .reason("Granted by user " + grantedBy)
                    .contextData("{\"grantedBy\":" + grantedBy + "}")
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write permission-grant audit log for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    /**
     * Log a permission revocation event.
     *
     * @param userId     the user whose permission was revoked
     * @param username   the username
     * @param permission the permission being revoked
     * @param revokedBy  the ID of the user who revoked the permission
     */
    @Async
    public void logPermissionRevoke(Long userId, String username,
            String permission, Long revokedBy) {
        try {
            PermissionAuditLog entry = PermissionAuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(AuditAction.PERMISSION_REVOKE)
                    .permission(permission)
                    .result(AuditResult.NOT_APPLICABLE)
                    .reason("Revoked by user " + revokedBy)
                    .contextData("{\"revokedBy\":" + revokedBy + "}")
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write permission-revoke audit log for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    /**
     * Log a role change event (assign or revoke).
     *
     * @param userId    the user whose role changed
     * @param username  the username
     * @param action    ROLE_ASSIGN or ROLE_REVOKE
     * @param roleName  the role that was assigned or revoked
     * @param changedBy the ID of the user who made the change
     */
    @Async
    public void logRoleChange(Long userId, String username, AuditAction action,
            String roleName, Long changedBy) {
        try {
            PermissionAuditLog entry = PermissionAuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .permission(roleName)
                    .result(AuditResult.NOT_APPLICABLE)
                    .reason("Changed by user " + changedBy)
                    .contextData("{\"changedBy\":" + changedBy + ",\"role\":\"" + roleName + "\"}")
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write role-change audit log for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    /**
     * Log a generic access attempt (e.g., hitting a protected endpoint).
     *
     * @param userId      the user attempting access
     * @param username    the username
     * @param requestPath the HTTP request path
     * @param method      the HTTP method
     * @param result      ALLOWED or DENIED
     * @param ipAddress   the client IP address
     */
    @Async
    public void logAccessAttempt(Long userId, String username, String requestPath,
            String method, AuditResult result, String ipAddress) {
        try {
            PermissionAuditLog entry = PermissionAuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(AuditAction.ACCESS_ATTEMPT)
                    .permission("ENDPOINT_ACCESS")
                    .requestPath(requestPath)
                    .requestMethod(method)
                    .result(result)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write access-attempt audit log for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    // ========================================================================
    // Read / query methods
    // ========================================================================

    /**
     * Get audit logs for a specific user within a date range.
     *
     * @param userId the user ID
     * @param from   start of the date range (inclusive)
     * @param to     end of the date range (inclusive)
     * @return list of audit log entries
     */
    @Transactional(readOnly = true)
    public List<PermissionAuditLog> getAuditLogs(Long userId, LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByUserIdAndCreatedAtBetween(userId, from, to);
    }

    /**
     * Get recent access denials for a specific user.
     *
     * @param userId the user ID
     * @param after  only return entries created after this timestamp
     * @return list of denied audit log entries
     */
    @Transactional(readOnly = true)
    public List<PermissionAuditLog> getDenials(Long userId, LocalDateTime after) {
        return auditLogRepository.findByUserIdAndResultAndCreatedAtAfter(
                userId, AuditResult.DENIED, after);
    }

    /**
     * Build a summary report of audit activity for an organization.
     *
     * @param orgId the organization ID
     * @param from  start of the reporting period
     * @param to    end of the reporting period
     * @return the audit summary DTO
     */
    @Transactional(readOnly = true)
    public AuditSummaryDTO getAuditSummary(Long orgId, LocalDateTime from, LocalDateTime to) {
        long totalAllowed = auditLogRepository.countByResultAndCreatedAtBetween(
                AuditResult.ALLOWED, from, to);
        long totalDenied = auditLogRepository.countByResultAndCreatedAtBetween(
                AuditResult.DENIED, from, to);
        long totalChecks = totalAllowed + totalDenied;

        List<String> topDenied = auditLogRepository.findTopDeniedPermissions(from, 10);
        List<String> topResources = auditLogRepository.findTopAccessedResources(from, 10);

        return AuditSummaryDTO.builder()
                .totalChecks(totalChecks)
                .totalAllowed(totalAllowed)
                .totalDenied(totalDenied)
                .topDeniedPermissions(topDenied)
                .topAccessedResources(topResources)
                .periodStart(from)
                .periodEnd(to)
                .build();
    }

    // ========================================================================
    // Internal helpers
    // ========================================================================

    /**
     * Safely extract the client IP address from the request, handling
     * proxied requests via X-Forwarded-For.
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Safely extract a header value from the request.
     */
    private String extractHeader(HttpServletRequest request, String headerName) {
        return request != null ? request.getHeader(headerName) : null;
    }

    /**
     * Safely extract the request URI.
     */
    private String extractRequestPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : null;
    }

    /**
     * Safely extract the HTTP method.
     */
    private String extractRequestMethod(HttpServletRequest request) {
        return request != null ? request.getMethod() : null;
    }
}
