package com.propertize.platform.auth.dto;

import com.propertize.platform.auth.entity.IpRuleScope;
import com.propertize.platform.auth.entity.IpRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for creating an IP access rule.
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpAccessRuleRequest {

    @NotNull(message = "Rule type is required")
    private IpRuleType ruleType;

    @NotBlank(message = "IP pattern is required")
    private String ipPattern;

    private String description;

    @NotNull(message = "Scope is required")
    private IpRuleScope scope;

    /** Organization ID, role name, or user ID depending on scope */
    private String scopeValue;

    /** Optional expiration for temporary rules */
    private LocalDateTime expiresAt;
}
