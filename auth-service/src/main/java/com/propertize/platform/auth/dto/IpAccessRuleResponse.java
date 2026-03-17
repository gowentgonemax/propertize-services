package com.propertize.platform.auth.dto;

import com.propertize.platform.auth.entity.IpRuleScope;
import com.propertize.platform.auth.entity.IpRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for IP access rule responses.
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpAccessRuleResponse {

    private Long id;
    private IpRuleType ruleType;
    private String ipPattern;
    private String description;
    private IpRuleScope scope;
    private String scopeValue;
    private boolean isActive;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
