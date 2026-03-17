package com.propertize.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IP Access Rule Entity
 *
 * Represents an IP-based access control rule that can whitelist or blacklist
 * IP addresses, CIDR ranges, or wildcard patterns. Rules can be scoped
 * globally, per organization, per role, or per user.
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
@Entity
@Table(name = "ip_access_rules", indexes = {
        @Index(name = "idx_ip_rule_scope", columnList = "scope"),
        @Index(name = "idx_ip_rule_scope_value", columnList = "scope, scope_value"),
        @Index(name = "idx_ip_rule_is_active", columnList = "is_active"),
        @Index(name = "idx_ip_rule_scope_active", columnList = "scope, is_active"),
        @Index(name = "idx_ip_rule_scope_value_active", columnList = "scope, scope_value, is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpAccessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Whether this is a whitelist or blacklist rule.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private IpRuleType ruleType;

    /**
     * IP address pattern: exact IP, CIDR notation (e.g., 192.168.1.0/24),
     * or wildcard (e.g., 192.168.*).
     */
    @Column(name = "ip_pattern", nullable = false, length = 255)
    private String ipPattern;

    /**
     * Human-readable description of this rule.
     */
    @Column(length = 500)
    private String description;

    /**
     * The scope at which this rule applies.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IpRuleScope scope;

    /**
     * The value associated with the scope:
     * - GLOBAL: null
     * - ORGANIZATION: organization ID
     * - ROLE: role name
     * - USER: user ID
     */
    @Column(name = "scope_value", length = 255)
    private String scopeValue;

    /**
     * Whether this rule is currently active.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /**
     * The ID of the user who created this rule.
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * Timestamp when this rule was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional expiration timestamp. Null means the rule never expires.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
