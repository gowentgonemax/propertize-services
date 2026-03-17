package com.propertize.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * User Session Information
 * Stores details about an active user session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionInfo implements Serializable {

    private String sessionId;
    private String username;
    private String organizationId;
    private String organizationCode;
    private List<String> roles;
    private Instant createdAt;
    private Instant lastAccessedAt;
    private String ipAddress;
    private String userAgent;
    private boolean isActive;
}
