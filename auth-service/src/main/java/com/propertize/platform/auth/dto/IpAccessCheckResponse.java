package com.propertize.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for IP access check results.
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpAccessCheckResponse {

    private String ipAddress;
    private boolean allowed;
    private String matchedRule;
    private String reason;
}
