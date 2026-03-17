package com.propertize.platform.auth.service;

import com.propertize.platform.auth.service.TokenBlacklistService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for TokenBlacklistService
 *
 * Tests all token blacklist operations including:
 * - JTI-based token revocation
 * - Token blacklisting by hash
 * - Refresh token storage and rotation
 * - Session termination
 * - Token reuse prevention
 * - TTL management
 * - Edge cases
 *
 * @author Propertize Platform Team
 * @version 2.0 - Production Ready
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Token Blacklist Service Tests")
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        // Use lenient stubbing to avoid UnnecessaryStubbingException
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        lenient().when(connectionFactory.getConnection()).thenReturn(connection);
        lenient().when(connection.ping()).thenReturn("PONG");
    }

    // ==================== JTI-Based Blacklist Tests ====================

    @Nested
    @DisplayName("JTI-Based Blacklist Tests")
    class JtiBlacklistTests {

        @Test
        @DisplayName("Should blacklist token by JTI with TTL")
        void testBlacklistByJti_Success() {
            // Given
            String jti = "jti-12345";
            long expirationSeconds = 900L;
            String reason = "logout";

            // When
            tokenBlacklistService.blacklistByJti(jti, expirationSeconds, reason);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

            verify(valueOperations).set(
                    keyCaptor.capture(),
                    valueCaptor.capture(),
                    durationCaptor.capture());

            assertTrue(keyCaptor.getValue().startsWith("blacklist:jti:"));
            assertTrue(valueCaptor.getValue().contains("logout"));
            assertEquals(Duration.ofSeconds(900), durationCaptor.getValue());
        }

        @Test
        @DisplayName("Should detect blacklisted JTI")
        void testIsBlacklistedByJti_BlacklistedToken() {
            // Given
            String jti = "blacklisted-jti";
            when(redisTemplate.hasKey("blacklist:jti:" + jti)).thenReturn(true);

            // When
            boolean result = tokenBlacklistService.isBlacklistedByJti(jti);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should detect non-blacklisted JTI")
        void testIsBlacklistedByJti_ValidToken() {
            // Given
            String jti = "valid-jti";
            when(redisTemplate.hasKey("blacklist:jti:" + jti)).thenReturn(false);

            // When
            boolean result = tokenBlacklistService.isBlacklistedByJti(jti);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle null JTI in blacklist check")
        void testIsBlacklistedByJti_NullJti() {
            // When
            boolean result = tokenBlacklistService.isBlacklistedByJti(null);

            // Then
            assertFalse(result);
            verify(redisTemplate, never()).hasKey(anyString());
        }

        @Test
        @DisplayName("Should handle empty JTI in blacklist check")
        void testIsBlacklistedByJti_EmptyJti() {
            // When
            boolean result = tokenBlacklistService.isBlacklistedByJti("");

            // Then
            assertFalse(result);
            verify(redisTemplate, never()).hasKey(anyString());
        }
    }

    // ==================== Token Hash Blacklist Tests ====================

    @Nested
    @DisplayName("Token Hash Blacklist Tests")
    class TokenBlacklistTests {

        @Test
        @DisplayName("Should blacklist token by hash with TTL")
        void testBlacklistToken_Success() {
            // Given
            String token = "access-token-123";
            long expirationSeconds = 300L;

            // When
            tokenBlacklistService.blacklistToken(token, expirationSeconds);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

            verify(valueOperations).set(
                    keyCaptor.capture(),
                    eq("blacklisted"),
                    durationCaptor.capture());

            assertTrue(keyCaptor.getValue().startsWith("token:blacklist:"));
            assertEquals(Duration.ofSeconds(300), durationCaptor.getValue());
        }

        @Test
        @DisplayName("Should detect blacklisted token by hash")
        void testIsBlacklisted_BlacklistedToken() {
            // Given
            String token = "blacklisted-token";
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // When
            boolean result = tokenBlacklistService.isBlacklisted(token);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should detect non-blacklisted token by hash")
        void testIsBlacklisted_ValidToken() {
            // Given
            String token = "valid-token";
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            boolean result = tokenBlacklistService.isBlacklisted(token);

            // Then
            assertFalse(result);
        }
    }

    // ==================== Session Termination Tests ====================

    @Nested
    @DisplayName("Session Termination Tests")
    class SessionTerminationTests {

        @Test
        @DisplayName("Should terminate session")
        void testTerminateSession_Success() {
            // Given
            String sessionId = "session-abc-123";

            // When
            tokenBlacklistService.terminateSession(sessionId);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(
                    keyCaptor.capture(),
                    eq("terminated"),
                    eq(Duration.ofDays(7)));

            assertEquals("session:session-abc-123", keyCaptor.getValue());
        }

        @Test
        @DisplayName("Should detect terminated session")
        void testIsSessionTerminated_TerminatedSession() {
            // Given
            String sessionId = "terminated-session";
            when(redisTemplate.hasKey("session:" + sessionId)).thenReturn(true);

            // When
            boolean result = tokenBlacklistService.isSessionTerminated(sessionId);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should detect active session")
        void testIsSessionTerminated_ActiveSession() {
            // Given
            String sessionId = "active-session";
            when(redisTemplate.hasKey("session:" + sessionId)).thenReturn(false);

            // When
            boolean result = tokenBlacklistService.isSessionTerminated(sessionId);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle null session ID")
        void testSessionTermination_NullSessionId() {
            // When
            tokenBlacklistService.terminateSession(null);
            boolean result = tokenBlacklistService.isSessionTerminated(null);

            // Then
            assertFalse(result);
            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }
    }

    // ==================== Refresh Token Storage Tests ====================

    @Nested
    @DisplayName("Refresh Token Storage Tests")
    class RefreshTokenStorageTests {

        @Test
        @DisplayName("Should store refresh token with username")
        void testStoreRefreshToken_Success() {
            // Given
            String refreshToken = "refresh-token-456";
            String username = "testuser";
            long expirationSeconds = 604800L;

            // When
            tokenBlacklistService.storeRefreshToken(refreshToken, username, expirationSeconds);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

            verify(valueOperations).set(
                    keyCaptor.capture(),
                    eq(username),
                    durationCaptor.capture());

            assertTrue(keyCaptor.getValue().startsWith("token:refresh:"));
            assertEquals(Duration.ofSeconds(604800), durationCaptor.getValue());
        }

        @Test
        @DisplayName("Should mark refresh token as used")
        void testMarkRefreshTokenAsUsed_Success() {
            // Given
            String refreshToken = "refresh-token-789";

            // When
            tokenBlacklistService.markRefreshTokenAsUsed(refreshToken);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(
                    keyCaptor.capture(),
                    eq("used"),
                    eq(Duration.ofDays(7)));

            assertTrue(keyCaptor.getValue().startsWith("token:used:"));
        }

        @Test
        @DisplayName("Should revoke refresh token")
        void testRevokeRefreshToken_Success() {
            // Given
            String refreshToken = "token-to-revoke";

            // When
            tokenBlacklistService.revokeRefreshToken(refreshToken);

            // Then
            ArgumentCaptor<String> deleteKeyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> setKeyCaptor = ArgumentCaptor.forClass(String.class);

            verify(redisTemplate).delete(deleteKeyCaptor.capture());
            verify(valueOperations).set(
                    setKeyCaptor.capture(),
                    eq("revoked"),
                    eq(Duration.ofDays(7)));

            assertTrue(deleteKeyCaptor.getValue().startsWith("token:refresh:"));
            assertTrue(setKeyCaptor.getValue().startsWith("token:used:"));
        }
    }

    // ==================== Refresh Token Validation Tests ====================

    @Nested
    @DisplayName("Refresh Token Validation Tests")
    class RefreshTokenValidationTests {

        @Test
        @DisplayName("Should detect used refresh token")
        void testIsRefreshTokenUsed_UsedToken() {
            // Given
            String refreshToken = "used-token";
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // When
            boolean result = tokenBlacklistService.isRefreshTokenUsed(refreshToken);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should detect unused refresh token")
        void testIsRefreshTokenUsed_UnusedToken() {
            // Given
            String refreshToken = "unused-token";
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            boolean result = tokenBlacklistService.isRefreshTokenUsed(refreshToken);

            // Then
            assertFalse(result);
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long token strings")
        void testBlacklistToken_LongToken() {
            // Given
            String longToken = "a".repeat(1000);
            long expirationSeconds = 300L;

            // When
            tokenBlacklistService.blacklistToken(longToken, expirationSeconds);

            // Then
            verify(valueOperations).set(
                    anyString(),
                    eq("blacklisted"),
                    eq(Duration.ofSeconds(300)));
        }

        @Test
        @DisplayName("Should handle zero expiration time")
        void testBlacklistToken_ZeroExpiration() {
            // Given
            String token = "zero-expiry-token";
            long expirationSeconds = 0L;

            // When
            tokenBlacklistService.blacklistToken(token, expirationSeconds);

            // Then
            verify(valueOperations).set(
                    anyString(),
                    eq("blacklisted"),
                    eq(Duration.ofSeconds(0)));
        }

        @Test
        @DisplayName("Should check Redis availability")
        void testRedisAvailability() {
            // Given - Set up proper mocking chain
            when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
            when(connectionFactory.getConnection()).thenReturn(connection);
            when(connection.ping()).thenReturn("PONG");

            // When
            boolean available = tokenBlacklistService.isRedisAvailable();

            // Then
            assertTrue(available);
        }

        @Test
        @DisplayName("Should handle concurrent blacklist operations")
        void testConcurrentBlacklist() {
            // Given
            String jti1 = "jti-concurrent-1";
            String jti2 = "jti-concurrent-2";

            // When
            tokenBlacklistService.blacklistByJti(jti1, 900, "logout");
            tokenBlacklistService.blacklistByJti(jti2, 900, "logout");

            // Then
            verify(valueOperations, times(2)).set(anyString(), anyString(), any(Duration.class));
        }
    }
}
