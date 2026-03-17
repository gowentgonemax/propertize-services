package com.propertize.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Authorization request DTO for the centralized RBAC API.
 * Other services send this to auth-service for permission checks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequest {
    private String userId;
    private String resource;
    private String action;
    private String permission;
    private List<String> roles;
    private String organizationId;
    private Map<String, Object> attributes;
}
