package com.propertize.platform.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

/**
 * User information response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private boolean authenticated;
    private String username;
    private String email;
    private Set<String> roles;
    private String primaryRole;
    private String organizationId;
    private String organizationCode;
    private String organizationName;
    private String message;
    private String timestamp;

    /**
     * Create authenticated user response
     */
    public static UserInfoResponse authenticated(
            String username,
            String email,
            Set<String> roles,
            String organizationId,
            String organizationCode,
            String organizationName) {

        return UserInfoResponse.builder()
            .authenticated(true)
            .username(username)
            .email(email)
            .roles(roles)
            .primaryRole(roles.isEmpty() ? null : roles.iterator().next())
            .organizationId(organizationId)
            .organizationCode(organizationCode)
            .organizationName(organizationName)
            .timestamp(Instant.now().toString())
            .build();
    }

    /**
     * Create unauthorized response
     */
    public static UserInfoResponse unauthorized() {
        return UserInfoResponse.builder()
            .authenticated(false)
            .roles(Collections.emptySet())
            .message("Not authenticated")
            .timestamp(Instant.now().toString())
            .build();
    }
}
