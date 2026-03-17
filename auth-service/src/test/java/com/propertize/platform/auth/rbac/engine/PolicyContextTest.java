package com.propertize.platform.auth.rbac.engine;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolicyContext — the immutable request context for policy
 * evaluation.
 * Tests builder, role checks, attribute access, static factories, and display
 * name.
 */
@DisplayName("PolicyContext Tests")
class PolicyContextTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build context with all fields")
        void testFullBuilder() {
            UUID orgId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();

            PolicyContext ctx = PolicyContext.builder()
                    .userId("user1")
                    .role("ADMIN")
                    .role("VIEWER")
                    .organizationId(orgId)
                    .tenantId(tenantId)
                    .requestPath("/api/property")
                    .requestMethod("GET")
                    .ipAddress("10.0.0.1")
                    .sessionId("sess123")
                    .userEmail("user@test.com")
                    .attribute("key1", "value1")
                    .authenticated(true)
                    .internalRequest(false)
                    .correlationId("corr123")
                    .userAgent("TestAgent")
                    .build();

            assertEquals("user1", ctx.getUserId());
            assertTrue(ctx.getRoles().contains("ADMIN"));
            assertTrue(ctx.getRoles().contains("VIEWER"));
            assertEquals(orgId, ctx.getOrganizationId());
            assertEquals(tenantId, ctx.getTenantId());
            assertEquals("/api/property", ctx.getRequestPath());
            assertEquals("GET", ctx.getRequestMethod());
            assertEquals("10.0.0.1", ctx.getIpAddress());
            assertEquals("sess123", ctx.getSessionId());
            assertEquals("user@test.com", ctx.getUserEmail());
            assertTrue(ctx.isAuthenticated());
            assertFalse(ctx.isInternalRequest());
            assertEquals("corr123", ctx.getCorrelationId());
            assertEquals("TestAgent", ctx.getUserAgent());
        }

        @Test
        @DisplayName("Should default authenticated to true")
        void testDefaultAuthenticated() {
            PolicyContext ctx = PolicyContext.builder().userId("u").build();
            assertTrue(ctx.isAuthenticated());
        }

        @Test
        @DisplayName("Should default internalRequest to false")
        void testDefaultInternalRequest() {
            PolicyContext ctx = PolicyContext.builder().userId("u").build();
            assertFalse(ctx.isInternalRequest());
        }

        @Test
        @DisplayName("Should set timestamp")
        void testTimestamp() {
            long before = System.currentTimeMillis();
            PolicyContext ctx = PolicyContext.builder().userId("u").build();
            long after = System.currentTimeMillis();

            assertTrue(ctx.getTimestamp() >= before);
            assertTrue(ctx.getTimestamp() <= after);
        }
    }

    // ==================== hasRole ====================

    @Nested
    @DisplayName("hasRole Tests")
    class HasRoleTests {

        @Test
        @DisplayName("Should detect present role")
        void testHasRole() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").role("ADMIN").build();
            assertTrue(ctx.hasRole("ADMIN"));
        }

        @Test
        @DisplayName("Should return false for absent role")
        void testDoesNotHaveRole() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").role("VIEWER").build();
            assertFalse(ctx.hasRole("ADMIN"));
        }

        @Test
        @DisplayName("Should handle empty roles")
        void testEmptyRoles() {
            PolicyContext ctx = PolicyContext.builder().userId("u").build();
            assertFalse(ctx.hasRole("ADMIN"));
        }
    }

    // ==================== hasAnyRole ====================

    @Nested
    @DisplayName("hasAnyRole Tests")
    class HasAnyRoleTests {

        @Test
        @DisplayName("Should return true when any role matches")
        void testAnyMatch() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").role("VIEWER").build();
            assertTrue(ctx.hasAnyRole("ADMIN", "VIEWER"));
        }

        @Test
        @DisplayName("Should return false when none match")
        void testNoneMatch() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").role("VIEWER").build();
            assertFalse(ctx.hasAnyRole("ADMIN", "EDITOR"));
        }

        @Test
        @DisplayName("Should handle null args")
        void testNullArgs() {
            PolicyContext ctx = PolicyContext.builder().userId("u").build();
            assertFalse(ctx.hasAnyRole((String[]) null));
        }
    }

    // ==================== hasAllRoles ====================

    @Nested
    @DisplayName("hasAllRoles Tests")
    class HasAllRolesTests {

        @Test
        @DisplayName("Should return true when all roles present")
        void testAllPresent() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").role("ADMIN").role("VIEWER").build();
            assertTrue(ctx.hasAllRoles("ADMIN", "VIEWER"));
        }

        @Test
        @DisplayName("Should return false when any role missing")
        void testOneMissing() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").role("ADMIN").build();
            assertFalse(ctx.hasAllRoles("ADMIN", "VIEWER"));
        }
    }

    // ==================== Attributes ====================

    @Nested
    @DisplayName("Attribute Access Tests")
    class AttributeTests {

        @Test
        @DisplayName("hasAttribute should return true for present key")
        void testHasAttribute() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").attribute("key1", "val1").build();
            assertTrue(ctx.hasAttribute("key1"));
            assertFalse(ctx.hasAttribute("missing"));
        }

        @Test
        @DisplayName("getAttribute with type should cast correctly")
        void testGetAttributeTyped() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").attribute("count", 42).build();
            assertEquals(42, ctx.getAttribute("count", Integer.class));
        }

        @Test
        @DisplayName("getAttribute with type should return null for missing")
        void testGetAttributeTypedNull() {
            PolicyContext ctx = PolicyContext.builder().userId("u").build();
            assertNull(ctx.getAttribute("missing", String.class));
        }

        @Test
        @DisplayName("getAttribute with type should throw ClassCastException on mismatch")
        void testGetAttributeTypeMismatch() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").attribute("val", "string").build();
            assertThrows(ClassCastException.class,
                    () -> ctx.getAttribute("val", Integer.class));
        }

        @Test
        @DisplayName("getAttribute with default should return default for missing")
        void testGetAttributeDefault() {
            PolicyContext ctx = PolicyContext.builder().userId("u").build();
            assertEquals("default", ctx.getAttribute("missing", "default"));
        }
    }

    // ==================== getDisplayName ====================

    @Nested
    @DisplayName("getDisplayName Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("Should return email when present")
        void testEmailDisplay() {
            PolicyContext ctx = PolicyContext.builder()
                    .userId("u").userEmail("test@mail.com").build();
            assertEquals("test@mail.com", ctx.getDisplayName());
        }

        @Test
        @DisplayName("Should return userId when no email")
        void testUserIdDisplay() {
            PolicyContext ctx = PolicyContext.builder().userId("user1").build();
            assertEquals("user1", ctx.getDisplayName());
        }

        @Test
        @DisplayName("Should return 'anonymous' when no email or userId")
        void testAnonymousDisplay() {
            PolicyContext ctx = PolicyContext.builder().build();
            assertEquals("anonymous", ctx.getDisplayName());
        }
    }

    // ==================== Static Factories ====================

    @Nested
    @DisplayName("Static Factory Tests")
    class StaticFactoryTests {

        @Test
        @DisplayName("anonymous() should create unauthenticated context")
        void testAnonymous() {
            PolicyContext ctx = PolicyContext.anonymous();
            assertEquals("anonymous", ctx.getUserId());
            assertFalse(ctx.isAuthenticated());
        }

        @Test
        @DisplayName("service() should create internal request context")
        void testService() {
            PolicyContext ctx = PolicyContext.service("gateway");
            assertEquals("service:gateway", ctx.getUserId());
            assertTrue(ctx.isAuthenticated());
            assertTrue(ctx.isInternalRequest());
        }
    }
}
