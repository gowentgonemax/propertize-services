package com.propertize.platform.auth.rbac.engine.evaluators;

import com.propertize.platform.auth.rbac.engine.ConditionEvaluator;
import com.propertize.platform.auth.rbac.engine.PolicyContext;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OwnershipConditionEvaluator — ABAC ownership checks.
 * Tests owner match, org match, platform admin bypass, and non-ownership
 * passthrough.
 */
@DisplayName("OwnershipConditionEvaluator Tests")
class OwnershipConditionEvaluatorTest {

    private OwnershipConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new OwnershipConditionEvaluator();
    }

    // ==================== supports() ====================

    @Test
    @DisplayName("Should support 'ownership' condition")
    void testSupportsOwnership() {
        assertTrue(evaluator.supports("ownership"));
    }

    @Test
    @DisplayName("Should not support other conditions")
    void testDoesNotSupportOther() {
        assertFalse(evaluator.supports("time-based"));
        assertFalse(evaluator.supports("ip-range"));
        assertFalse(evaluator.supports(null));
    }

    // ==================== evaluate() — non-ownership condition
    // ====================

    @Test
    @DisplayName("Should return true for non-ownership conditions (passthrough)")
    void testNonOwnershipPassthrough() {
        PolicyContext context = PolicyContext.builder()
                .userId("user1")
                .build();

        assertTrue(evaluator.evaluate(context, "other-condition", Collections.emptyMap()));
    }

    // ==================== evaluate() — owner match ====================

    @Nested
    @DisplayName("Owner Match Tests")
    class OwnerMatchTests {

        @Test
        @DisplayName("Should allow when user is the owner")
        void testOwnerMatch() {
            PolicyContext context = PolicyContext.builder()
                    .userId("user1")
                    .build();

            Map<String, Object> attrs = Map.of("ownerId", "user1");

            assertTrue(evaluator.evaluate(context, "ownership", attrs));
        }

        @Test
        @DisplayName("Should deny when user is not the owner")
        void testOwnerMismatch() {
            PolicyContext context = PolicyContext.builder()
                    .userId("user1")
                    .build();

            Map<String, Object> attrs = Map.of("ownerId", "other-user");

            assertFalse(evaluator.evaluate(context, "ownership", attrs));
        }
    }

    // ==================== evaluate() — org match ====================

    @Nested
    @DisplayName("Organization Match Tests")
    class OrgMatchTests {

        @Test
        @DisplayName("Should allow when user is in same organization")
        void testOrgMatch() {
            UUID orgId = UUID.randomUUID();
            PolicyContext context = PolicyContext.builder()
                    .userId("user1")
                    .organizationId(orgId)
                    .build();

            Map<String, Object> attrs = Map.of("organizationId", orgId.toString());

            assertTrue(evaluator.evaluate(context, "ownership", attrs));
        }

        @Test
        @DisplayName("Should deny when user is in different organization and not owner")
        void testOrgMismatch() {
            PolicyContext context = PolicyContext.builder()
                    .userId("user1")
                    .organizationId(UUID.randomUUID())
                    .build();

            Map<String, Object> attrs = new HashMap<>();
            attrs.put("organizationId", UUID.randomUUID().toString());
            attrs.put("ownerId", "other-user");

            assertFalse(evaluator.evaluate(context, "ownership", attrs));
        }

        @Test
        @DisplayName("Should handle UUID object in attributes")
        void testUuidObjectInAttributes() {
            UUID orgId = UUID.randomUUID();
            PolicyContext context = PolicyContext.builder()
                    .userId("user1")
                    .organizationId(orgId)
                    .build();

            Map<String, Object> attrs = Map.of("organizationId", orgId);

            assertTrue(evaluator.evaluate(context, "ownership", attrs));
        }

        @Test
        @DisplayName("Should handle invalid UUID string gracefully")
        void testInvalidUuidString() {
            PolicyContext context = PolicyContext.builder()
                    .userId("user1")
                    .organizationId(UUID.randomUUID())
                    .build();

            Map<String, Object> attrs = new HashMap<>();
            attrs.put("organizationId", "not-a-uuid");
            attrs.put("ownerId", "other-user");

            assertFalse(evaluator.evaluate(context, "ownership", attrs));
        }
    }

    // ==================== evaluate() — platform admin bypass ====================

    @Nested
    @DisplayName("Platform Admin Bypass Tests")
    class PlatformAdminBypassTests {

        @Test
        @DisplayName("Should allow PLATFORM_OVERSIGHT regardless of ownership")
        void testPlatformOversightBypass() {
            PolicyContext context = PolicyContext.builder()
                    .userId("admin1")
                    .role("PLATFORM_OVERSIGHT")
                    .build();

            Map<String, Object> attrs = Map.of("ownerId", "other-user");

            assertTrue(evaluator.evaluate(context, "ownership", attrs));
        }

        @Test
        @DisplayName("Should allow PLATFORM_OPERATIONS regardless of ownership")
        void testPlatformOperationsBypass() {
            PolicyContext context = PolicyContext.builder()
                    .userId("ops1")
                    .role("PLATFORM_OPERATIONS")
                    .build();

            Map<String, Object> attrs = Map.of("ownerId", "other-user");

            assertTrue(evaluator.evaluate(context, "ownership", attrs));
        }
    }

    // ==================== evaluate() — edge cases ====================

    @Test
    @DisplayName("Should deny when no ownerId, no orgId, and no admin role")
    void testNoMatchingCriteria() {
        PolicyContext context = PolicyContext.builder()
                .userId("user1")
                .role("VIEWER")
                .build();

        assertFalse(evaluator.evaluate(context, "ownership", Collections.emptyMap()));
    }
}
