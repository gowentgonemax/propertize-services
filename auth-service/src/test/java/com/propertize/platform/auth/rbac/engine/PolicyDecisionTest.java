package com.propertize.platform.auth.rbac.engine;

import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolicyDecision — the immutable authorization result.
 * Tests static factories, summary, audit log, platform admin detection.
 */
@DisplayName("PolicyDecision Tests")
class PolicyDecisionTest {

    // ==================== Static Factories ====================

    @Nested
    @DisplayName("Static Factory Tests")
    class StaticFactoryTests {

        @Test
        @DisplayName("allow() should create allowed decision")
        void testAllow() {
            PolicyDecision d = PolicyDecision.allow("Access granted");
            assertTrue(d.isAllowed());
            assertEquals("Access granted", d.getReason());
        }

        @Test
        @DisplayName("deny() should create denied decision")
        void testDeny() {
            PolicyDecision d = PolicyDecision.deny("Insufficient permissions");
            assertFalse(d.isAllowed());
            assertEquals("Insufficient permissions", d.getReason());
        }

        @Test
        @DisplayName("allowWithPermissions() should include matched permissions")
        void testAllowWithPermissions() {
            PolicyDecision d = PolicyDecision.allowWithPermissions(
                    "OK", List.of("property:read", "property:update"));
            assertTrue(d.isAllowed());
            assertEquals(2, d.getMatchedPermissions().size());
            assertTrue(d.getMatchedPermissions().contains("property:read"));
        }
    }

    // ==================== Builder ====================

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build decision with all fields")
        void testFullBuilder() {
            PolicyDecision d = PolicyDecision.builder()
                    .allowed(true)
                    .reason("Permission granted")
                    .policyId("policy-v2")
                    .configVersion("2.0.0")
                    .matchedPermission("property:read")
                    .evaluatedRole("ADMIN")
                    .evaluatedRole("VIEWER")
                    .evaluationTimeMs(15L)
                    .resource("property")
                    .action("read")
                    .userId("user1")
                    .organizationId("org-123")
                    .conditionResult("ownership", true)
                    .fromCache(false)
                    .build();

            assertTrue(d.isAllowed());
            assertEquals("Permission granted", d.getReason());
            assertEquals("policy-v2", d.getPolicyId());
            assertEquals("2.0.0", d.getConfigVersion());
            assertEquals(1, d.getMatchedPermissions().size());
            assertEquals(2, d.getEvaluatedRoles().size());
            assertEquals(15L, d.getEvaluationTimeMs());
            assertEquals("property", d.getResource());
            assertEquals("read", d.getAction());
            assertEquals("user1", d.getUserId());
            assertEquals("org-123", d.getOrganizationId());
            assertTrue(d.getConditionResults().get("ownership"));
            assertFalse(d.isFromCache());
            assertNotNull(d.getTimestamp());
        }

        @Test
        @DisplayName("Should default timestamp to now")
        void testDefaultTimestamp() {
            Instant before = Instant.now();
            PolicyDecision d = PolicyDecision.builder().allowed(true).reason("ok").build();
            Instant after = Instant.now();

            assertNotNull(d.getTimestamp());
            assertFalse(d.getTimestamp().isBefore(before));
            assertFalse(d.getTimestamp().isAfter(after));
        }
    }

    // ==================== isPlatformAdminBypass ====================

    @Nested
    @DisplayName("isPlatformAdminBypass Tests")
    class PlatformAdminBypassTests {

        @Test
        @DisplayName("Should detect wildcard permission as admin bypass")
        void testWildcardBypass() {
            PolicyDecision d = PolicyDecision.builder()
                    .allowed(true)
                    .reason("ok")
                    .matchedPermission("*")
                    .build();
            assertTrue(d.isPlatformAdminBypass());
        }

        @Test
        @DisplayName("Should detect admin:all permission as admin bypass")
        void testAdminAllBypass() {
            PolicyDecision d = PolicyDecision.builder()
                    .allowed(true)
                    .reason("ok")
                    .matchedPermission("admin:all")
                    .build();
            assertTrue(d.isPlatformAdminBypass());
        }

        @Test
        @DisplayName("Should return false for normal permissions")
        void testNormalPermission() {
            PolicyDecision d = PolicyDecision.builder()
                    .allowed(true)
                    .reason("ok")
                    .matchedPermission("property:read")
                    .build();
            assertFalse(d.isPlatformAdminBypass());
        }

        @Test
        @DisplayName("Should return false when denied")
        void testDeniedNotBypass() {
            PolicyDecision d = PolicyDecision.builder()
                    .allowed(false)
                    .reason("denied")
                    .build();
            assertFalse(d.isPlatformAdminBypass());
        }
    }

    // ==================== getSummary ====================

    @Test
    @DisplayName("getSummary should produce formatted string")
    void testGetSummary() {
        PolicyDecision d = PolicyDecision.builder()
                .allowed(true)
                .reason("Permission granted")
                .resource("property")
                .action("read")
                .userId("user1")
                .organizationId("org-1")
                .evaluationTimeMs(5L)
                .build();

        String summary = d.getSummary();
        assertTrue(summary.contains("[ALLOW]"));
        assertTrue(summary.contains("property:read"));
        assertTrue(summary.contains("user1"));
        assertTrue(summary.contains("org-1"));
        assertTrue(summary.contains("5ms"));
    }

    // ==================== toAuditLog ====================

    @Test
    @DisplayName("toAuditLog should produce complete map")
    void testToAuditLog() {
        PolicyDecision d = PolicyDecision.builder()
                .allowed(true)
                .reason("ok")
                .resource("property")
                .action("read")
                .userId("user1")
                .organizationId("org-1")
                .matchedPermission("property:read")
                .evaluatedRole("ADMIN")
                .evaluationTimeMs(10L)
                .policyId("pol-1")
                .configVersion("2.0")
                .fromCache(true)
                .build();

        Map<String, Object> log = d.toAuditLog();

        assertEquals(true, log.get("allowed"));
        assertEquals("ok", log.get("reason"));
        assertEquals("property", log.get("resource"));
        assertEquals("read", log.get("action"));
        assertEquals("user1", log.get("userId"));
        assertEquals("org-1", log.get("organizationId"));
        assertEquals(10L, log.get("evaluationTimeMs"));
        assertEquals("pol-1", log.get("policyId"));
        assertEquals("2.0", log.get("configVersion"));
        assertEquals(true, log.get("fromCache"));
        assertNotNull(log.get("timestamp"));
    }

    @Test
    @DisplayName("toAuditLog should handle null fields gracefully")
    void testToAuditLogNullFields() {
        PolicyDecision d = PolicyDecision.builder()
                .allowed(false)
                .build();

        Map<String, Object> log = d.toAuditLog();

        assertEquals(false, log.get("allowed"));
        assertEquals("", log.get("reason"));
        assertEquals("", log.get("resource"));
        assertEquals("", log.get("action"));
        assertEquals("", log.get("userId"));
    }
}
