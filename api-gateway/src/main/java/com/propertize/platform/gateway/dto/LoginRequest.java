package com.propertize.platform.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login request DTO
 * Supports both username and email login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * Username or email for login
     */
    private String username;

    /**
     * Email for login (alternative to username)
     */
    private String email;

    /**
     * User password
     */
    private String password;

    /**
     * Get the identifier (username or email, whichever is provided)
     */
    public String getIdentifier() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return username;
    }
}
