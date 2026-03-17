package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RateLimitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service using token bucket algorithm
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitConfig config;
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordResetBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> accountLockouts = new ConcurrentHashMap<>();

    public boolean isLoginAllowed(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String normalizedUsername = username.toLowerCase().trim();

        if (isAccountLockedOut(normalizedUsername)) {
            log.warn("Login attempt rejected - account locked out: {}", normalizedUsername);
            return false;
        }

        Bucket bucket = loginBuckets.computeIfAbsent(normalizedUsername, k -> createLoginBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Login rate limit exceeded for username: {}", normalizedUsername);
        }

        return allowed;
    }

    public boolean isPasswordResetAllowed(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String normalizedEmail = email.toLowerCase().trim();
        Bucket bucket = passwordResetBuckets.computeIfAbsent(normalizedEmail, k -> createPasswordResetBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Password reset rate limit exceeded for email: {}", normalizedEmail);
        }

        return allowed;
    }

    public boolean isIpAllowed(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }

        Bucket bucket = ipBuckets.computeIfAbsent(ipAddress, k -> createIpBucket());
        return bucket.tryConsume(1);
    }

    public void recordFailedLogin(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        String normalizedUsername = username.toLowerCase().trim();
        int attempts = failedAttempts.merge(normalizedUsername, 1, Integer::sum);

        if (attempts >= config.getLockout().getMaxFailedAttempts()) {
            long lockoutUntil = System.currentTimeMillis() +
                    (config.getLockout().getDurationMinutes() * 60 * 1000L);
            accountLockouts.put(normalizedUsername, lockoutUntil);
            log.warn("Account locked out due to failed login attempts: {} (attempts: {})",
                    normalizedUsername, attempts);
        }
    }

    public void resetFailedAttempts(String username) {
        if (username != null && !username.trim().isEmpty()) {
            String normalizedUsername = username.toLowerCase().trim();
            failedAttempts.remove(normalizedUsername);
            accountLockouts.remove(normalizedUsername);
        }
    }

    private boolean isAccountLockedOut(String username) {
        Long lockoutUntil = accountLockouts.get(username);
        if (lockoutUntil == null) {
            return false;
        }

        if (System.currentTimeMillis() > lockoutUntil) {
            accountLockouts.remove(username);
            failedAttempts.remove(username);
            return false;
        }

        return true;
    }

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(
                config.getLogin().getMaxAttemptsPerMinute(),
                Refill.intervally(config.getLogin().getMaxAttemptsPerMinute(), Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createPasswordResetBucket() {
        Bandwidth limit = Bandwidth.classic(
                config.getPasswordReset().getMaxAttemptsPerHour(),
                Refill.intervally(config.getPasswordReset().getMaxAttemptsPerHour(), Duration.ofHours(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createIpBucket() {
        Bandwidth limit = Bandwidth.classic(
                config.getIp().getMaxRequestsPerMinute(),
                Refill.intervally(config.getIp().getMaxRequestsPerMinute(), Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
