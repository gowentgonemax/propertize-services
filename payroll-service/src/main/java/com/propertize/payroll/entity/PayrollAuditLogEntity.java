package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.PayrollAuditActionStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for audit trail of all payroll-related actions.
 */
@Entity
@Table(name = "payroll_audit_logs", indexes = {
    @Index(name = "idx_audit_client", columnList = "client_id"),
    @Index(name = "idx_audit_action", columnList = "actionType"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "actionTimestamp"),
    @Index(name = "idx_audit_entity", columnList = "entityType, entityId")
})
@Getter
@Setter
public class PayrollAuditLogEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "user_id", length = 100)
    private String user;

    @Column(nullable = false, length = 100)
    private String actionType;

    @Column(nullable = false)
    private LocalDateTime actionTimestamp;

    /**
     * Type of entity being acted upon
     */
    @Column(nullable = false, length = 50)
    private String entityType;

    /**
     * ID of the entity being acted upon
     */
    @Column(length = 36)
    private String entityId;

    /**
     * Brief description of the action
     */
    @Column(nullable = false, length = 500)
    private String description;

    /**
     * Previous value (for update actions)
     */
    @Column(columnDefinition = "TEXT")
    private String previousValue;

    /**
     * New value (for update actions)
     */
    @Column(columnDefinition = "TEXT")
    private String newValue;

    /**
     * Field that was changed
     */
    @Column(length = 100)
    private String changedField;

    /**
     * IP address of the user
     */
    @Column(length = 45)
    private String ipAddress;

    /**
     * User agent/browser info
     */
    @Column(length = 500)
    private String userAgent;

    /**
     * Session ID if applicable
     */
    @Column(length = 100)
    private String sessionId;

    /**
     * Correlation ID for tracking related actions
     */
    @Column(length = 36)
    private String correlationId;

    /**
     * Monetary amount involved (if applicable)
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Outcome status of the audit action.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PayrollAuditActionStatusEnum actionStatus = PayrollAuditActionStatusEnum.SUCCESS;

    /**
     * Error message if action failed
     */
    @Column(length = 1000)
    private String errorMessage;

    /**
     * Additional metadata as JSON
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
