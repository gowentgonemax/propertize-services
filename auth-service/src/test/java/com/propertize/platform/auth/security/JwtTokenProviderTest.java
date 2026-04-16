package com.propertize.platform.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtTokenProvider — covers both token generation methods.
 * Uses an in-memory RSA key pair so tests are standalone (no file I/O).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    @Mock
    private RsaKeyProvider rsaKeyProvider;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();

        lenient().when(rsaKeyProvider.getPrivateKey()).thenReturn(keyPair.getPrivate());
        lenient().when(rsaKeyProvider.getPublicKey()).thenReturn(keyPair.getPublic());
    }

    // ==================== generateAccessTokenWithPermissions ====================

    @Nested
    @DisplayName("generateAccessTokenWithPermissions Tests")
    class GenerateTokenWithPermissionsTests {

        @Test
        @DisplayName("Should include 'role' claim with primary role (first alphabetically)")
        void testPrimaryRoleClaimIsSet() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "alice", Set.of("ORGANIZATION_OWNER", "ACCOUNTANT"), "42", "ORG-001",
                    Set.of("PROPERTY_READ"), "INDIVIDUAL_PROPERTY_OWNER", null, null);

            Claims claims = parseClaims(token);
            // Alphabetically first: "ACCOUNTANT" < "ORGANIZATION_OWNER"
            assertEquals("ACCOUNTANT", claims.get("role", String.class));
        }

        @Test
        @DisplayName("Should include 'roles' claim with all roles")
        void testRolesClaimContainsAll() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "bob", Set.of("PORTFOLIO_OWNER"), "99", "ORG-002",
                    Set.of("PROPERTY_MANAGE"), "PROPERTY_MANAGEMENT_COMPANY", null, null);

            Claims claims = parseClaims(token);
            List<?> roles = claims.get("roles", List.class);
            assertNotNull(roles);
            assertTrue(roles.contains("PORTFOLIO_OWNER"));
        }

        @Test
        @DisplayName("Should NOT include 'permissions' in JWT (permissions cached in Redis by PermissionCacheService)")
        void testPermissionsClaimContainsAll() {
            Set<String> perms = Set.of("PROPERTY_READ", "LEASE_CREATE", "TENANT_READ");
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "carol", Set.of("PROPERTY_MANAGER"), "7", "ORG-003", perms, "CORPORATE", null, null);

            Claims claims = parseClaims(token);
            List<?> permissions = claims.get("permissions", List.class);
            assertNull(permissions, "Permissions should not be in JWT — they are cached in Redis");
        }

        @Test
        @DisplayName("Should include 'orgType' claim")
        void testOrgTypeClaimIsSet() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "dave", Set.of("SOLO_OWNER"), "1", "ORG-004",
                    Set.of("PROPERTY_READ"), "INDIVIDUAL_PROPERTY_OWNER", null, null);

            Claims claims = parseClaims(token);
            assertEquals("INDIVIDUAL_PROPERTY_OWNER", claims.get("orgType", String.class));
        }

        @Test
        @DisplayName("Should set orgType to empty string when null is supplied")
        void testNullOrgTypeBecomesEmptyString() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "eve", Set.of("TENANT"), "2", "ORG-005", Set.of("TENANT_READ"), null, null, null);

            Claims claims = parseClaims(token);
            assertEquals("", claims.get("orgType", String.class));
        }

        @Test
        @DisplayName("Should include 'organizationId' and 'organizationCode' claims")
        void testOrgClaimsAreSet() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "frank", Set.of("ORGANIZATION_ADMIN"), "55", "ORG-ALPHA",
                    Set.of("USER_READ"), "HOUSING_ASSOCIATION", null, null);

            Claims claims = parseClaims(token);
            assertEquals("55", claims.get("organizationId", String.class));
            assertEquals("ORG-ALPHA", claims.get("organizationCode", String.class));
        }

        @Test
        @DisplayName("Should set tokenType to 'access'")
        void testTokenTypeIsAccess() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "grace", Set.of("TEAM_MEMBER"), "3", "ORG-006", Set.of(), "CORPORATE", null, null);

            Claims claims = parseClaims(token);
            assertEquals("access", claims.get("tokenType", String.class));
        }

        @Test
        @DisplayName("Subject (sub) should be the username")
        void testSubjectIsUsername() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "henry", Set.of("INSPECTOR"), "4", "ORG-007", Set.of(), "CORPORATE", null, null);

            Claims claims = parseClaims(token);
            assertEquals("henry", claims.getSubject());
        }

        @Test
        @DisplayName("Should handle empty roles set — primaryRole should be empty string")
        void testEmptyRolesPrimaryRoleEmpty() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "ivan", null, "5", "ORG-008", Set.of(), "CORPORATE", null, null);

            Claims claims = parseClaims(token);
            assertEquals("", claims.get("role", String.class));
        }
    }

    // ==================== validateToken ====================

    @Nested
    @DisplayName("validateToken Tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return true for a valid token")
        void testValidToken() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "user1", Set.of("TENANT"), "10", "T-001", Set.of(), "HOUSING_ASSOCIATION", null, null);

            assertTrue(jwtTokenProvider.validateToken(token));
        }

        @Test
        @DisplayName("Should return false for a garbage string")
        void testInvalidToken() {
            assertFalse(jwtTokenProvider.validateToken("not.a.jwt.token"));
        }

        @Test
        @DisplayName("Should return false for null/empty token")
        void testNullToken() {
            assertFalse(jwtTokenProvider.validateToken(null));
            assertFalse(jwtTokenProvider.validateToken(""));
        }
    }

    // ==================== getUsernameFromToken ====================

    @Nested
    @DisplayName("getUsernameFromToken Tests")
    class GetUsernameFromTokenTests {

        @Test
        @DisplayName("Should extract username from a valid token")
        void testExtractUsername() {
            String token = jwtTokenProvider.generateAccessTokenWithPermissions(
                    "myuser", Set.of("VENDOR"), "20", "V-001", Set.of(), "CORPORATE", null, null);

            assertEquals("myuser", jwtTokenProvider.getUsernameFromToken(token));
        }
    }

    // ==================== Helpers ====================

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(keyPair.getPublic())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
