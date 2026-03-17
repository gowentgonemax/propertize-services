package com.propertize.platform.auth.dto;

import com.propertize.platform.auth.entity.DelegationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for delegation responses.
 *
 * Returned by delegation API endpoints to provide details about
 * a permission delegation, including its lifecycle state and
 * audit information.
 *
 * @version 1.0 - Phase 3: Permission Delegation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelegationResponse {

    private Long id;
    private Long delegatorUserId;
    private Long delegateUserId;
    private String permission;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private DelegationStatus status;
    private String reason;
    private Long parentDelegationId;
    private Long organizationId;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long revokedBy;
    private LocalDateTime revokedAt;
}
