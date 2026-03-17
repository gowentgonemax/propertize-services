package com.propertize.platform.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Rate limiting configuration
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Data
@Validated
public class RateLimitConfig {

    private boolean enabled = true;

    @NotNull
    private LoginConfig login = new LoginConfig();

    @NotNull
    private PasswordResetConfig passwordReset = new PasswordResetConfig();

    @NotNull
    private IpConfig ip = new IpConfig();

    @NotNull
    private LockoutConfig lockout = new LockoutConfig();

    @Data
    public static class LoginConfig {
        @Min(1)
        private int maxAttemptsPerMinute = 5;

        @Min(1)
        private int maxAttemptsPerHour = 20;
    }

    @Data
    public static class PasswordResetConfig {
        @Min(1)
        private int maxAttemptsPerHour = 3;
    }

    @Data
    public static class IpConfig {
        @Min(1)
        private int maxRequestsPerMinute = 100;
    }

    @Data
    public static class LockoutConfig {
        @Min(1)
        private int maxFailedAttempts = 5;

        @Min(1)
        private int durationMinutes = 30;
    }
}
