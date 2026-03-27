package com.propertize.platform.gateway.integration;

import com.propertize.platform.gateway.security.EnhancedJwtTokenProvider;
import com.propertize.platform.gateway.security.RsaKeyProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.*;
import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration Tests for Cross-Service Authentication Flow
 *
 * Tests the complete authentication flow from token generation to validation
 * across different microservices in the platform.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cross-Service Authentication Integration Tests")
class CrossServiceAuthenticationIT {

    @Mock
    private RsaKeyProvider rsaKeyProvider;

    private EnhancedJwtTokenProvider jwtTokenProvider;
    private KeyPair rsaKeyPair;
    private SecretKey hmacKey;

    private static final String TEST_SECRET = "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS10aGF0LWlzLWxvbmctZW5vdWdoLWZvci1obWFjLTI1Ng==";

    @BeforeEach
    void setUp() throws Exception {
        // Generate RSA key pair
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        rsaKeyPair = generator.generateKeyPair();

        // Generate HMAC key
        hmacKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));

        // Create provider
        jwtTokenProvider = new EnhancedJwtTokenProvider(rsaKeyProvider);

        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", 604800000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "serviceTokenExpirationMs", 300000L);
    }

    @Nested
    @DisplayName("End-to-End Authentication Flow Tests")
    class EndToEndAuthenticationTests {

        @BeforeEach
        void initRsa() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(true);
            when(rsaKeyProvider.getPublicKey()).thenReturn(rsaKeyPair.getPublic());
            when(rsaKeyProvider.hasPrivateKey()).thenReturn(true);
            when(rsaKeyProvider.getPrivateKey()).thenReturn(rsaKeyPair.getPrivate());
            jwtTokenProvider.init();
        }

        @Test
        @DisplayName("Complete user authentication flow")
        void shouldCompleteUserAuthenticationFlow() {
            // Step 1: User login - propertize generates access token
            String userId = "user-" + UUID.randomUUID();
            String username = "testuser@example.com";
            String organizationId = "org-" + UUID.randomUUID();
            Set<String> roles = Set.of("ORGANIZATION_OWNER", "PROPERTY_MANAGER");

            String accessToken = createUserAccessToken(username, userId, organizationId, roles);

            // Step 2: Gateway receives request with token
            assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();

            // Step 3: Gateway extracts user info
            Optional<String> extractedUsername = jwtTokenProvider.getUsername(accessToken);
            Optional<String> extractedOrgId = jwtTokenProvider.getOrganizationId(accessToken);
            Set<String> extractedRoles = jwtTokenProvider.getRoles(accessToken);

            assertThat(extractedUsername).isPresent().contains(username);
            assertThat(extractedOrgId).isPresent().contains(organizationId);
            assertThat(extractedRoles).containsAll(roles);

            // Step 4: Gateway generates service token for downstream service
            Optional<String> serviceToken = jwtTokenProvider.generateServiceToken("api-gateway", "propertize");
            assertThat(serviceToken).isPresent();

            // Step 5: Downstream service validates service token
            assertThat(jwtTokenProvider.validateServiceToken(serviceToken.get(), "propertize")).isTrue();
            assertThat(jwtTokenProvider.isServiceToken(serviceToken.get())).isTrue();
        }

        @Test
        @DisplayName("Token refresh flow")
        void shouldCompleteTokenRefreshFlow() {
            // Step 1: User has a valid refresh token
            String username = "testuser@example.com";
            String refreshToken = createRefreshToken(username);

            // Step 2: Validate it's a refresh token
            assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
            assertThat(jwtTokenProvider.validateRefreshToken(refreshToken)).isTrue();
            assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue();
            assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse();

            // Step 3: Generate new access token (simulated)
            String newAccessToken = createUserAccessToken(
                    username,
                    "user-123",
                    "org-456",
                    Set.of("USER"));

            // Step 4: Validate new access token
            assertThat(jwtTokenProvider.validateToken(newAccessToken)).isTrue();
            assertThat(jwtTokenProvider.isAccessToken(newAccessToken)).isTrue();
        }

        @Test
        @DisplayName("Service-to-service communication flow")
        void shouldCompleteServiceToServiceFlow() {
            // Scenario: API Gateway calls Propertize, which calls EmployeCraft

            // Step 1: API Gateway generates token for Propertize
            Optional<String> gatewayToPropertize = jwtTokenProvider.generateServiceToken("api-gateway", "propertize");
            assertThat(gatewayToPropertize).isPresent();

            // Step 2: Propertize validates incoming service token
            assertThat(jwtTokenProvider.validateServiceToken(gatewayToPropertize.get(), "propertize")).isTrue();

            // Step 3: Propertize generates token for EmployeCraft
            Optional<String> propertizeToEmployeCraft = jwtTokenProvider.generateServiceToken("propertize",
                    "employecraft");
            assertThat(propertizeToEmployeCraft).isPresent();

            // Step 4: EmployeCraft validates incoming service token
            assertThat(jwtTokenProvider.validateServiceToken(propertizeToEmployeCraft.get(), "employecraft")).isTrue();

            // Step 5: Verify tokens are for correct targets only
            assertThat(jwtTokenProvider.validateServiceToken(gatewayToPropertize.get(), "employecraft")).isFalse();
            assertThat(jwtTokenProvider.validateServiceToken(propertizeToEmployeCraft.get(), "propertize")).isFalse();
        }

        @Test
        @DisplayName("Multi-tenant authentication flow")
        void shouldCompleteMultiTenantAuthFlow() {
            // Two users from different organizations
            String user1Token = createUserAccessToken(
                    "admin@company1.com",
                    "user-1",
                    "org-company1",
                    Set.of("ORGANIZATION_OWNER"));

            String user2Token = createUserAccessToken(
                    "admin@company2.com",
                    "user-2",
                    "org-company2",
                    Set.of("ORGANIZATION_OWNER"));

            // Validate both tokens
            assertThat(jwtTokenProvider.validateToken(user1Token)).isTrue();
            assertThat(jwtTokenProvider.validateToken(user2Token)).isTrue();

            // Extract organization IDs
            Optional<String> org1 = jwtTokenProvider.getOrganizationId(user1Token);
            Optional<String> org2 = jwtTokenProvider.getOrganizationId(user2Token);

            assertThat(org1).isPresent().contains("org-company1");
            assertThat(org2).isPresent().contains("org-company2");

            // Organizations should be different
            assertThat(org1.get()).isNotEqualTo(org2.get());
        }
    }

    @Nested
    @DisplayName("Security Edge Cases")
    class SecurityEdgeCases {

        @BeforeEach
        void initRsa() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(true);
            when(rsaKeyProvider.getPublicKey()).thenReturn(rsaKeyPair.getPublic());
            when(rsaKeyProvider.hasPrivateKey()).thenReturn(true);
            when(rsaKeyProvider.getPrivateKey()).thenReturn(rsaKeyPair.getPrivate());
            jwtTokenProvider.init();
        }

        @Test
        @DisplayName("Should reject access token used as refresh token")
        void shouldRejectAccessTokenAsRefresh() {
            String accessToken = createUserAccessToken(
                    "test@example.com",
                    "user-1",
                    "org-1",
                    Set.of("USER"));

            assertThat(jwtTokenProvider.validateRefreshToken(accessToken)).isFalse();
        }

        @Test
        @DisplayName("Should reject service token used as user token")
        void shouldRejectServiceTokenAsUser() {
            Optional<String> serviceToken = jwtTokenProvider.generateServiceToken("api-gateway", "propertize");

            assertThat(serviceToken).isPresent();
            assertThat(jwtTokenProvider.isAccessToken(serviceToken.get())).isFalse();
            assertThat(jwtTokenProvider.isRefreshToken(serviceToken.get())).isFalse();
        }

        @Test
        @DisplayName("Should reject tampered tokens")
        void shouldRejectTamperedToken() {
            String validToken = createUserAccessToken(
                    "test@example.com",
                    "user-1",
                    "org-1",
                    Set.of("USER"));

            // Tamper with the payload
            String[] parts = validToken.split("\\.");
            String tamperedPayload = Base64.getEncoder().encodeToString(
                    "{\"sub\":\"hacked\",\"exp\":9999999999}".getBytes());
            String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

            assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
        }

        @Test
        @DisplayName("Should reject tokens signed with different keys")
        void shouldRejectTokensSignedWithDifferentKeys() throws Exception {
            // Generate a different key pair
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair differentKeyPair = generator.generateKeyPair();

            String tokenWithDifferentKey = Jwts.builder()
                    .subject("test@example.com")
                    .claim("type", "access")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(differentKeyPair.getPrivate(), Jwts.SIG.RS256)
                    .compact();

            assertThat(jwtTokenProvider.validateToken(tokenWithDifferentKey)).isFalse();
        }
    }

    @Nested
    @DisplayName("HMAC to RSA Migration Compatibility")
    class MigrationCompatibilityTests {

        @Test
        @DisplayName("Should validate HMAC tokens when RSA not available")
        void shouldValidateHmacTokensWhenRsaNotAvailable() {
            when(rsaKeyProvider.isRsaEnabled()).thenReturn(false);
            jwtTokenProvider.init();

            String hmacToken = Jwts.builder()
                    .subject("test@example.com")
                    .claim("type", "access")
                    .claim("organizationId", "org-123")
                    .claim("roles", Arrays.asList("USER"))
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(hmacKey, Jwts.SIG.HS256)
                    .compact();

            assertThat(jwtTokenProvider.validateToken(hmacToken)).isTrue();
            assertThat(jwtTokenProvider.getUsername(hmacToken)).isPresent().contains("test@example.com");
        }
    }

    // Helper methods
    private String createUserAccessToken(String username, String userId, String organizationId, Set<String> roles) {
        return Jwts.builder()
                .subject(username)
                .claim("type", "access")
                .claim("userId", userId)
                .claim("organizationId", organizationId)
                .claim("roles", new ArrayList<>(roles))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .signWith(rsaKeyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private String createRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 604800000))
                .signWith(rsaKeyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
