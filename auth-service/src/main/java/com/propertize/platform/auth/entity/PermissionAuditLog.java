package com.propertize.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Permission Audit Log Entity
 *
 * <p>
 * Records every permission check, grant, denial, and access decision
 * for compliance auditing. Supports queries such as "show me all access
 * denials for user X in the last 7 days" and "who accessed financial
 * data this month?".
 * </p>
 *
 * <p>
 * Indexes are designed for efficient range scans on {@code createdAt}
 * combined with common filter columns, making the table partition-ready
 * on {@code created_at} if needed.
 * </p>
 *
 * @version 1.0 - Phase 4b: Permission Audit Trail
 */
@Entity
@Table(name = "permission_audit_logs", indexes = {
        @Index(name = "idx_audit_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_audit_permission_created", columnList = "permission, created_at"),
        @Index(name = "idx_audit_result_created", columnList = "result, created_at"),
        @Index(name = "idx_audit_org_created", columnList = "organization_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user whose action is being audited.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The username of the user (denormalized for fast audit reads).
     */
    @Column(nullable = false, length = 255)
    private String username;

    /**
     * The type of auditable action (e.g., PERMISSION_CHECK, ROLE_ASSIGN).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    /**
     * The permission string involved (e.g., "PROPERTY_WRITE").
     */
    @Column(nullable = false, length = 255)
    private String permission;

    /**
     * Optional resource type that the action relates to (e.g., "PROPERTY",
     * "LEASE").
     */
    @Column(name = "resource_type", length = 100)
    private String resourceType;

    /**
     * Optional resource identifier (e.g., a property ID or lease number).
     */
    @Column(name = "resource_id", length = 255)
    private String resourceId;

    /**
     * The outcome of the action (ALLOWED, DENIED, ERROR, NOT_APPLICABLE).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditResult result;

    /**
     * Optional human-readable reason for the result (e.g., "insufficient privilege
     * level").
     */
    @Column(length = 500)
    private String reason;

    /**
     * IP address of the client that originated the request.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User-Agent header from the originating request.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * The HTTP request path (e.g., "/api/v1/properties/123").
     */
    @Column(name = "request_path", length = 500)
    private String requestPath;

    /**
     * The HTTP method (GET, POST, PUT, DELETE, etc.).
     */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /**
     * The organization context for multi-tenant audit queries.
     */
    @Column(name = "organization_id")
    private Long organizationId;

    /**
     * The session identifier for correlating actions within a user session.
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    /**
     * JSON string for arbitrary extra context data (e.g., changed fields,
     * before/after values).
     */
    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData;

    /**
     * Timestamp when this audit log entry was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically set the creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
