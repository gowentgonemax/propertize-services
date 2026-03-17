package com.propertize.platform.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for service-to-service authentication in auth-service
 * Uses API keys for internal service communication
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "service.authentication")
public class ServiceAuthenticationConfig {

    /**
     * Enable/disable service authentication
     */
    private boolean enabled = true;

    /**
     * API key for this service (auth-service)
     */
    private String apiKey;

    /**
     * Trusted service API keys
     * Key: service name, Value: API key
     */
    private Map<String, String> trustedServices = new HashMap<>();

    /**
     * Header name for API key
     */
    private String headerName = "X-Service-Api-Key";

    /**
     * Header name for service identifier
     */
    private String serviceIdentifierHeader = "X-Service-Name";
}
