package com.propertize.platform.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for EnhancedJwtTokenProvider
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnhancedJwtTokenProvider Tests")
class EnhancedJwtTokenProviderTest {

    @Mock
    private RsaKeyProvider rsaKeyProvider;

    private EnhancedJwtTokenProvider jwtTokenProvider;

    private Key hmacKey;
    private KeyPair rsaKeyPair;

    private static final String TEST_SECRET = "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS10aGF0LWlzLWxvbmctZW5vdWdoLWZvci1obWFjLTI1Ng==";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 900000; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604800000; // 7 days
    private static final long SERVICE_TOKEN_EXPIRATION_MS = 300000; // 5 minutes

    @BeforeEach
    void setUp() throws Exception {
        // Generate RSA key pair for testing
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        rsaKeyPair = generator.generateKeyPair();

        // Generate HMAC key
        hmacKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));

        // Create provider
        jwtTokenProvider = new EnhancedJwtTokenProvider(rsaKeyProvider);

        // Set configuration values
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMs", ACCESS_TOKEN_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", REFRESH_TOKEN_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtTokenProvider, "serviceTokenExpirationMs", SERVICE_TOKEN_EXPIRATION_MS);
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize with RSA keys when available")
        void shouldInitializeWithRsaKeys() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(true);
            when(rsaKeyProvider.getPublicKey()).thenReturn(rsaKeyPair.getPublic());
            when(rsaKeyProvider.hasPrivateKey()).thenReturn(true);
            when(rsaKeyProvider.getPrivateKey()).thenReturn(rsaKeyPair.getPrivate());

            jwtTokenProvider.init();

            verify(rsaKeyProvider).isRsaEnabled();
            verify(rsaKeyProvider).getPublicKey();
        }

        @Test
        @DisplayName("Should fall back to HMAC when RSA not available")
        void shouldFallBackToHmac() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(false);

            jwtTokenProvider.init();

            verify(rsaKeyProvider).isRsaEnabled();
            verify(rsaKeyProvider, never()).getPublicKey();
        }
    }

    @Nested
    @DisplayName("Token Validation Tests (HMAC)")
    class HmacTokenValidationTests {

        @BeforeEach
        void initHmac() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(false);
            jwtTokenProvider.init();
        }

        @Test
        @DisplayName("Should validate a valid HMAC token")
        void shouldValidateValidToken() {
            String token = createHmacToken("testuser", "access");

            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();

            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            assertThat(jwtTokenProvider.validateToken("invalid.token.here")).isFalse();
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void shouldRejectInvalidSignature() {
            Key differentKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(differentKey, SignatureAlgorithm.HS256)
                .compact();

            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Validation Tests (RSA)")
    class RsaTokenValidationTests {

        @BeforeEach
        void initRsa() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(true);
            when(rsaKeyProvider.getPublicKey()).thenReturn(rsaKeyPair.getPublic());
            when(rsaKeyProvider.hasPrivateKey()).thenReturn(true);
            when(rsaKeyProvider.getPrivateKey()).thenReturn(rsaKeyPair.getPrivate());
            jwtTokenProvider.init();
        }

        @Test
        @DisplayName("Should validate a valid RSA token")
        void shouldValidateValidRsaToken() {
            String token = createRsaToken("testuser", "access");

            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should reject RSA token signed with different key")
        void shouldRejectDifferentRsaKey() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair differentKeyPair = generator.generateKeyPair();

            String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(differentKeyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("Claims Extraction Tests")
    class ClaimsExtractionTests {

        @BeforeEach
        void initHmac() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(false);
            jwtTokenProvider.init();
        }

        @Test
        @DisplayName("Should extract username from token")
        void shouldExtractUsername() {
            String token = createHmacToken("testuser", "access");

            Optional<String> username = jwtTokenProvider.getUsername(token);

            assertThat(username).isPresent().contains("testuser");
        }

        @Test
        @DisplayName("Should extract organization ID from token")
        void shouldExtractOrganizationId() {
            String orgId = "org-123";
            String token = Jwts.builder()
                .setSubject("testuser")
                .claim("organizationId", orgId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();

            Optional<String> extracted = jwtTokenProvider.getOrganizationId(token);

            assertThat(extracted).isPresent().contains(orgId);
        }

        @Test
        @DisplayName("Should extract roles from token (List format)")
        void shouldExtractRolesFromList() {
            List<String> roles = Arrays.asList("ADMIN", "USER");
            String token = Jwts.builder()
                .setSubject("testuser")
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();

            Set<String> extracted = jwtTokenProvider.getRoles(token);

            assertThat(extracted).containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("Should extract roles from token (String format)")
        void shouldExtractRolesFromString() {
            String token = Jwts.builder()
                .setSubject("testuser")
                .claim("roles", "ADMIN,USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();

            Set<String> extracted = jwtTokenProvider.getRoles(token);

            assertThat(extracted).containsExactlyInAnyOrder("ADMIN", "USER");
        }
    }

    @Nested
    @DisplayName("Token Type Tests")
    class TokenTypeTests {

        @BeforeEach
        void initHmac() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(false);
            jwtTokenProvider.init();
        }

        @Test
        @DisplayName("Should identify access token")
        void shouldIdentifyAccessToken() {
            String token = createHmacToken("testuser", "access");

            assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
            assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
            assertThat(jwtTokenProvider.isServiceToken(token)).isFalse();
        }

        @Test
        @DisplayName("Should identify refresh token")
        void shouldIdentifyRefreshToken() {
            String token = createHmacToken("testuser", "refresh");

            assertThat(jwtTokenProvider.isAccessToken(token)).isFalse();
            assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
            assertThat(jwtTokenProvider.isServiceToken(token)).isFalse();
        }

        @Test
        @DisplayName("Should validate refresh token")
        void shouldValidateRefreshToken() {
            String token = createHmacToken("testuser", "refresh");

            assertThat(jwtTokenProvider.validateRefreshToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should reject access token as refresh token")
        void shouldRejectAccessAsRefresh() {
            String token = createHmacToken("testuser", "access");

            assertThat(jwtTokenProvider.validateRefreshToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("Service Token Tests")
    class ServiceTokenTests {

        @BeforeEach
        void initRsa() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(true);
            when(rsaKeyProvider.getPublicKey()).thenReturn(rsaKeyPair.getPublic());
            when(rsaKeyProvider.hasPrivateKey()).thenReturn(true);
            when(rsaKeyProvider.getPrivateKey()).thenReturn(rsaKeyPair.getPrivate());
            jwtTokenProvider.init();
        }

        @Test
        @DisplayName("Should generate service token")
        void shouldGenerateServiceToken() {
            Optional<String> token = jwtTokenProvider.generateServiceToken("api-gateway", "propertize");

            assertThat(token).isPresent();
            assertThat(jwtTokenProvider.validateToken(token.get())).isTrue();
            assertThat(jwtTokenProvider.isServiceToken(token.get())).isTrue();
        }

        @Test
        @DisplayName("Should validate service token with correct target")
        void shouldValidateServiceTokenWithTarget() {
            String token = jwtTokenProvider.generateServiceToken("api-gateway", "propertize").orElseThrow();

            assertThat(jwtTokenProvider.validateServiceToken(token, "propertize")).isTrue();
            assertThat(jwtTokenProvider.validateServiceToken(token, "employecraft")).isFalse();
        }

        @Test
        @DisplayName("Should validate service token without target check")
        void shouldValidateServiceTokenWithoutTarget() {
            String token = jwtTokenProvider.generateServiceToken("api-gateway", "propertize").orElseThrow();

            assertThat(jwtTokenProvider.validateServiceToken(token, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("Token Metadata Tests")
    class TokenMetadataTests {

        @BeforeEach
        void initHmac() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(false);
            jwtTokenProvider.init();
        }

        @Test
        @DisplayName("Should return expiration date")
        void shouldReturnExpirationDate() {
            String token = createHmacToken("testuser", "access");

            Optional<Date> expiration = jwtTokenProvider.getExpirationDate(token);

            assertThat(expiration).isPresent();
            assertThat(expiration.get()).isAfter(new Date());
        }

        @Test
        @DisplayName("Should return issued at date")
        void shouldReturnIssuedAt() {
            String token = createHmacToken("testuser", "access");

            Optional<Date> issuedAt = jwtTokenProvider.getIssuedAt(token);

            assertThat(issuedAt).isPresent();
            assertThat(issuedAt.get()).isBefore(new Date(System.currentTimeMillis() + 1000));
        }

        @Test
        @DisplayName("Should return correct expiration configuration")
        void shouldReturnExpirationConfig() {
            assertThat(jwtTokenProvider.getAccessTokenExpirationMs()).isEqualTo(ACCESS_TOKEN_EXPIRATION_MS);
            assertThat(jwtTokenProvider.getRefreshTokenExpirationMs()).isEqualTo(REFRESH_TOKEN_EXPIRATION_MS);
        }
    }

    // Helper methods
    private String createHmacToken(String subject, String type) {
        return Jwts.builder()
            .setSubject(subject)
            .claim("type", type)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(hmacKey, SignatureAlgorithm.HS256)
            .compact();
    }

    private String createRsaToken(String subject, String type) {
        return Jwts.builder()
            .setSubject(subject)
            .claim("type", type)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(rsaKeyPair.getPrivate(), SignatureAlgorithm.RS256)
            .compact();
    }
}
