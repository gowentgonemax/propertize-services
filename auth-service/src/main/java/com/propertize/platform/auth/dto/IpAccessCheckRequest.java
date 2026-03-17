package com.propertize.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for checking if an IP is allowed.
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpAccessCheckRequest {

    @NotBlank(message = "IP address is required")
    private String ipAddress;

    private Long userId;
    private Set<String> roles;
    private Long orgId;
}
