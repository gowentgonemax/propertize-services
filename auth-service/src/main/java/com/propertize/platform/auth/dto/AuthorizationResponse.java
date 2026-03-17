package com.propertize.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Authorization response DTO from the centralized RBAC API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {
    private boolean authorized;
    private String reason;
    private List<String> matchedPermissions;
    private List<String> evaluatedRoles;
    private long evaluationTimeMs;
}
