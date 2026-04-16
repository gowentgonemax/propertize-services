package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RateLimitConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitService Tests")
class RateLimitServiceTest {

    @Mock
    private RateLimitConfig config;

    @InjectMocks
    private RateLimitService rateLimitService;

    private RateLimitConfig.LoginConfig loginConfig;
    private RateLimitConfig.PasswordResetConfig pwResetConfig;
    private RateLimitConfig.IpConfig ipConfig;
    private RateLimitConfig.LockoutConfig lockoutConfig;

    @BeforeEach
    void setUp() {
        loginConfig = new RateLimitConfig.LoginConfig();
        loginConfig.setMaxAttemptsPerMinute(5);
        loginConfig.setMaxAttemptsPerHour(20);

        pwResetConfig = new RateLimitConfig.PasswordResetConfig();
        pwResetConfig.setMaxAttemptsPerHour(3);

        ipConfig = new RateLimitConfig.IpConfig();
        ipConfig.setMaxRequestsPerMinute(100);

        lockoutConfig = new RateLimitConfig.LockoutConfig();
        lockoutConfig.setMaxFailedAttempts(5);
        lockoutConfig.setDurationMinutes(30);

        when(config.getLogin()).thenReturn(loginConfig);
        when(config.getPasswordReset()).thenReturn(pwResetConfig);
        when(config.getIp()).thenReturn(ipConfig);
        when(config.getLockout()).thenReturn(lockoutConfig);
    }

    // ── isLoginAllowed ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("isLoginAllowed()")
    class IsLoginAllowed {

        @Test
        @DisplayName("Allows login for new username")
        void allowsNewUser() {
            assertThat(rateLimitService.isLoginAllowed("newuser")).isTrue();
        }

        @Test
        @DisplayName("Returns false for null username")
        void rejectNullUsername() {
            assertThat(rateLimitService.isLoginAllowed(null)).isFalse();
        }

        @Test
        @DisplayName("Returns false for blank username")
        void rejectBlankUsername() {
            assertThat(rateLimitService.isLoginAllowed("   ")).isFalse();
        }

        @Test
        @DisplayName("Normalizes username to lowercase")
        void normalizesUsername() {
            // Both should draw from the same bucket
            boolean first = rateLimitService.isLoginAllowed("Alice");
            boolean second = rateLimitService.isLoginAllowed("ALICE");
            // Both should be allowed (first two attempts of a 5/min bucket)
            assertThat(first).isTrue();
            assertThat(second).isTrue();
        }

        @Test
        @DisplayName("Rejects login when account is locked out")
        void rejectsLockedAccount() {
            // Exhaust failed attempts to trigger lock
            for (int i = 0; i < 5; i++) {
                rateLimitService.recordFailedLogin("locked.user");
            }
            assertThat(rateLimitService.isLoginAllowed("locked.user")).isFalse();
        }
    }

    // ── isPasswordResetAllowed ───────────────────────────────────────────────

    @Nested
    @DisplayName("isPasswordResetAllowed()")
    class IsPasswordResetAllowed {

        @Test
        @DisplayName("Allows reset for new email")
        void allowsNewEmail() {
            assertThat(rateLimitService.isPasswordResetAllowed("user@example.com")).isTrue();
        }

        @Test
        @DisplayName("Returns false for null email")
        void rejectsNullEmail() {
            assertThat(rateLimitService.isPasswordResetAllowed(null)).isFalse();
        }

        @Test
        @DisplayName("Returns false for blank email")
        void rejectsBlankEmail() {
            assertThat(rateLimitService.isPasswordResetAllowed("  ")).isFalse();
        }
    }

    // ── isIpAllowed ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isIpAllowed()")
    class IsIpAllowed {

        @Test
        @DisplayName("Allows first request from an IP")
        void allowsFirstRequest() {
            assertThat(rateLimitService.isIpAllowed("10.0.0.1")).isTrue();
        }

        @Test
        @DisplayName("Returns false for null IP")
        void rejectsNullIp() {
            assertThat(rateLimitService.isIpAllowed(null)).isFalse();
        }

        @Test
        @DisplayName("Returns false for blank IP")
        void rejectsBlankIp() {
            assertThat(rateLimitService.isIpAllowed("")).isFalse();
        }
    }

    // ── recordFailedLogin / resetFailedAttempts ──────────────────────────────

    @Nested
    @DisplayName("recordFailedLogin() & resetFailedAttempts()")
    class FailedLogins {

        @Test
        @DisplayName("Lock triggers after maxFailedAttempts")
        void lockTriggersAfterMaxAttempts() {
            for (int i = 0; i < 5; i++) {
                rateLimitService.recordFailedLogin("victim");
            }
            assertThat(rateLimitService.isLoginAllowed("victim")).isFalse();
        }

        @Test
        @DisplayName("Unlock after resetFailedAttempts")
        void unlockAfterReset() {
            for (int i = 0; i < 5; i++) {
                rateLimitService.recordFailedLogin("user2");
            }
            rateLimitService.resetFailedAttempts("user2");
            assertThat(rateLimitService.isLoginAllowed("user2")).isTrue();
        }

        @Test
        @DisplayName("recordFailedLogin silently ignores null username")
        void ignoresNullUsername() {
            assertThatCode(() -> rateLimitService.recordFailedLogin(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("resetFailedAttempts silently ignores null username")
        void ignoresNullUsernameOnReset() {
            assertThatCode(() -> rateLimitService.resetFailedAttempts(null))
                    .doesNotThrowAnyException();
        }
    }
}
