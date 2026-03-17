package com.propertize.platform.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Logout request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    /**
     * Optional refresh token to revoke
     */
    private String refreshToken;

    /**
     * Logout reason (manual, inactivity, session_expired, etc.)
     */
    private String reason;
}
