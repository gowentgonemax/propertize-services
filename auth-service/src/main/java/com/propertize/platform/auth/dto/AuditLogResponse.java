package com.propertize.platform.auth.dto;

import com.propertize.platform.auth.entity.AuditAction;
import com.propertize.platform.auth.entity.AuditResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for individual audit log entries.
 *
 * <p>
 * Provides a clean API representation of a {@code PermissionAuditLog}
 * entity, hiding internal fields such as user-agent and context data
 * from the standard response.
 * </p>
 *
 * @version 1.0 - Phase 4b: Permission Audit Trail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    /**
     * Unique audit log entry ID.
     */
    private Long id;

    /**
     * The ID of the user whose action was audited.
     */
    private Long userId;

    /**
     * The username of the user.
     */
    private String username;

    /**
     * The type of auditable action.
     */
    private AuditAction action;

    /**
     * The permission string involved.
     */
    private String permission;

    /**
     * The resource type, if applicable.
     */
    private String resourceType;

    /**
     * The resource identifier, if applicable.
     */
    private String resourceId;

    /**
     * The outcome of the action.
     */
    private AuditResult result;

    /**
     * Reason for the result, if applicable.
     */
    private String reason;

    /**
     * IP address of the originating client.
     */
    private String ipAddress;

    /**
     * The HTTP request path.
     */
    private String requestPath;

    /**
     * The HTTP method (GET, POST, etc.).
     */
    private String requestMethod;

    /**
     * Timestamp when the audit entry was created.
     */
    private LocalDateTime createdAt;
}
