package com.propertize.platform.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

/**
 * Authentication response containing JWT tokens and user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private boolean success;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String username;
    private Set<String> roles;
    private Set<String> permissions;
    private String organizationId;
    private String organizationCode;
    private String sessionId;
    private long expiresIn;
    private String message;
    private String timestamp;

    /**
     * Create successful auth response
     */
    public static AuthResponse success(
            String accessToken,
            String refreshToken,
            String username,
            Set<String> roles,
            Set<String> permissions,
            String organizationId,
            String organizationCode,
            long expiresIn) {

        return AuthResponse.builder()
            .success(true)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .username(username)
            .roles(roles)
            .permissions(permissions)
            .organizationId(organizationId)
            .organizationCode(organizationCode)
            .expiresIn(expiresIn)
            .message("Authentication successful")
            .timestamp(Instant.now().toString())
            .build();
    }

    /**
     * Create error response
     */
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
            .success(false)
            .message(message)
            .roles(Collections.emptySet())
            .permissions(Collections.emptySet())
            .timestamp(Instant.now().toString())
            .build();
    }
}
