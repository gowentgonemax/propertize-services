package com.propertize.platform.gateway.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Security Configuration Properties for API Gateway
 *
 * Centralizes all security-related configuration for easy management.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private JwtProperties jwt = new JwtProperties();
    private RbacProperties rbac = new RbacProperties();
    private GatewayProperties gateway = new GatewayProperties();

    @Data
    public static class JwtProperties {
        /**
         * Location of the RSA public key file for JWT verification
         */
        private String publicKeyLocation;

        /**
         * Shared secret for JWT verification (fallback for development)
         */
        private String secret;

        /**
         * Token expiration time in milliseconds
         */
        private long expirationMs = 900000; // 15 minutes

        /**
         * Refresh token expiration time in milliseconds
         */
        private long refreshExpirationMs = 604800000; // 7 days
    }

    @Data
    public static class RbacProperties {
        /**
         * Location of the RBAC configuration file
         */
        private String configLocation = "classpath:rbac.yml";

        /**
         * Cache TTL in seconds
         */
        private int cacheTtl = 3600;

        /**
         * Enable RBAC authorization at gateway level
         */
        private boolean enabled = true;
    }

    @Data
    public static class GatewayProperties {
        /**
         * Header name for identifying gateway source
         */
        private String sourceHeader = "X-Gateway-Source";

        /**
         * Expected value of the gateway source header
         */
        private String sourceValue = "api-gateway";

        /**
         * Enable strict mode - reject requests without proper gateway headers
         */
        private boolean strictMode = false;
    }
}
