package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.AuthorizationRequest;
import com.propertize.platform.auth.dto.AuthorizationResponse;
import com.propertize.platform.auth.rbac.engine.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthorizationService — the main authorization entry point.
 * Tests authorize(), bypass logic, resource/action enum resolution,
 * permission string fallback, batch checks, and cache operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService Tests")
class AuthorizationServiceTest {

    @Mock
    private RbacService rbacService;

    @Mock
    private PolicyEngine policyEngine;

    @InjectMocks
    private AuthorizationService authorizationService;

    // ==================== authorize() — Resource/Action Enum Path
    // ====================

    @Nested
    @DisplayName("authorize() with typed Resource/Action")
    class AuthorizeTypedTests {

        @Test
        @DisplayName("Should authorize when PolicyEngine allows")
        void testAuthorizeAllowed() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .resource("property")
                    .action("read")
                    .roles(List.of("PROPERTY_MANAGER"))
                    .build();

            PolicyDecision decision = PolicyDecision.builder()
                    .allowed(true)
                    .reason("Permission granted: property:read")
                    .matchedPermission("property:read")
                    .evaluatedRole("PROPERTY_MANAGER")
                    .evaluationTimeMs(5L)
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);
            when(policyEngine.evaluate(any(PolicyContext.class), eq(Resource.PROPERTY),
                    eq(Action.READ), anyMap())).thenReturn(decision);

            AuthorizationResponse response = authorizationService.authorize(request);

            assertTrue(response.isAuthorized());
            assertEquals("Permission granted: property:read", response.getReason());
        }

        @Test
        @DisplayName("Should deny when PolicyEngine denies")
        void testAuthorizeDenied() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .resource("property")
                    .action("delete")
                    .roles(List.of("VIEWER"))
                    .build();

            PolicyDecision decision = PolicyDecision.builder()
                    .allowed(false)
                    .reason("User does not have permission: property:delete")
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);
            when(policyEngine.evaluate(any(), eq(Resource.PROPERTY),
                    eq(Action.DELETE), anyMap())).thenReturn(decision);

            AuthorizationResponse response = authorizationService.authorize(request);

            assertFalse(response.isAuthorized());
            assertTrue(response.getReason().contains("property:delete"));
        }
    }

    // ==================== authorize() — Bypass Path ====================

    @Nested
    @DisplayName("authorize() with bypass")
    class AuthorizeBypassTests {

        @Test
        @DisplayName("Should grant access via platform admin bypass")
        void testPlatformAdminBypass() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("admin1")
                    .resource("system")
                    .action("configure")
                    .roles(List.of("PLATFORM_OVERSIGHT"))
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(true);

            AuthorizationResponse response = authorizationService.authorize(request);

            assertTrue(response.isAuthorized());
            assertEquals("Platform admin bypass", response.getReason());
            verify(policyEngine, never()).evaluate(any(), any(), any(), any());
        }
    }

    // ==================== authorize() — Permission String Fallback
    // ====================

    @Nested
    @DisplayName("authorize() with permission string fallback")
    class AuthorizePermissionStringTests {

        @Test
        @DisplayName("Should fall back to permission string when resource/action not in enum")
        void testUnknownResourceFallback() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .resource("custom_resource")
                    .action("custom_action")
                    .roles(List.of("ADMIN"))
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);
            when(policyEngine.hasPermission(any(PolicyContext.class),
                    eq("custom_resource:custom_action"))).thenReturn(true);

            AuthorizationResponse response = authorizationService.authorize(request);

            assertTrue(response.isAuthorized());
            assertEquals("Permission granted", response.getReason());
        }

        @Test
        @DisplayName("Should use explicit permission field when provided")
        void testExplicitPermission() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .permission("property:read")
                    .roles(List.of("VIEWER"))
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);
            when(policyEngine.hasPermission(any(PolicyContext.class),
                    eq("property:read"))).thenReturn(true);

            AuthorizationResponse response = authorizationService.authorize(request);

            assertTrue(response.isAuthorized());
        }

        @Test
        @DisplayName("Should deny when explicit permission not found")
        void testExplicitPermissionDenied() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .permission("system:delete")
                    .roles(List.of("VIEWER"))
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);
            when(policyEngine.hasPermission(any(PolicyContext.class),
                    eq("system:delete"))).thenReturn(false);

            AuthorizationResponse response = authorizationService.authorize(request);

            assertFalse(response.isAuthorized());
            assertTrue(response.getReason().contains("system:delete"));
        }
    }

    // ==================== authorize() — Invalid / Edge Cases ====================

    @Nested
    @DisplayName("authorize() edge cases")
    class AuthorizeEdgeCaseTests {

        @Test
        @DisplayName("Should deny when no resource, action, or permission provided")
        void testMissingAll() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .roles(List.of("VIEWER"))
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);

            AuthorizationResponse response = authorizationService.authorize(request);

            assertFalse(response.isAuthorized());
            assertTrue(response.getReason().contains("Invalid"));
        }

        @Test
        @DisplayName("Should handle organization ID in request")
        void testWithOrganizationId() {
            String orgId = UUID.randomUUID().toString();
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .resource("property")
                    .action("read")
                    .roles(List.of("MANAGER"))
                    .organizationId(orgId)
                    .build();

            PolicyDecision decision = PolicyDecision.builder()
                    .allowed(true)
                    .reason("Permission granted: property:read")
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);
            when(policyEngine.evaluate(any(), eq(Resource.PROPERTY),
                    eq(Action.READ), anyMap())).thenReturn(decision);

            AuthorizationResponse response = authorizationService.authorize(request);
            assertTrue(response.isAuthorized());
        }

        @Test
        @DisplayName("Should handle invalid organizationId gracefully")
        void testInvalidOrgId() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .resource("property")
                    .action("read")
                    .roles(List.of("MANAGER"))
                    .organizationId("not-a-uuid")
                    .build();

            PolicyDecision decision = PolicyDecision.builder()
                    .allowed(true)
                    .reason("OK")
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);
            when(policyEngine.evaluate(any(), eq(Resource.PROPERTY),
                    eq(Action.READ), anyMap())).thenReturn(decision);

            // Should not throw
            AuthorizationResponse response = authorizationService.authorize(request);
            assertTrue(response.isAuthorized());
        }

        @Test
        @DisplayName("Should handle exception gracefully")
        void testExceptionHandling() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .resource("property")
                    .action("read")
                    .roles(List.of("ADMIN"))
                    .build();

            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);
            when(policyEngine.evaluate(any(), any(), any(), anyMap()))
                    .thenThrow(new RuntimeException("DB down"));

            AuthorizationResponse response = authorizationService.authorize(request);

            assertFalse(response.isAuthorized());
            assertTrue(response.getReason().contains("DB down"));
        }
    }

    // ==================== getPermissionsForRoles ====================

    @Nested
    @DisplayName("getPermissionsForRoles Tests")
    class GetPermissionsForRolesTests {

        @Test
        @DisplayName("Should aggregate permissions from multiple roles")
        void testMultipleRoles() {
            when(rbacService.getPermissionsForRole("VIEWER"))
                    .thenReturn(Set.of("property:read"));
            when(rbacService.getPermissionsForRole("EDITOR"))
                    .thenReturn(Set.of("property:read", "property:update"));

            Set<String> perms = authorizationService.getPermissionsForRoles(
                    List.of("VIEWER", "EDITOR"));

            assertTrue(perms.contains("property:read"));
            assertTrue(perms.contains("property:update"));
        }

        @Test
        @DisplayName("Should return empty set for null/empty roles")
        void testEmptyRoles() {
            assertTrue(authorizationService.getPermissionsForRoles(null).isEmpty());
            assertTrue(authorizationService.getPermissionsForRoles(Collections.emptyList()).isEmpty());
        }
    }

    // ==================== checkPermissions (batch) ====================

    @Nested
    @DisplayName("checkPermissions Batch Tests")
    class CheckPermissionsTests {

        @Test
        @DisplayName("Should check multiple permissions in batch")
        void testBatchCheck() {
            when(policyEngine.hasPermission(any(PolicyContext.class), eq("property:read")))
                    .thenReturn(true);
            when(policyEngine.hasPermission(any(PolicyContext.class), eq("property:delete")))
                    .thenReturn(false);

            Map<String, Boolean> results = authorizationService.checkPermissions(
                    "user1",
                    List.of("VIEWER"),
                    List.of("property:read", "property:delete"));

            assertTrue(results.get("property:read"));
            assertFalse(results.get("property:delete"));
        }

        @Test
        @DisplayName("Should return empty map for null permissions")
        void testNullPermissions() {
            Map<String, Boolean> results = authorizationService.checkPermissions(
                    "user1", List.of("VIEWER"), null);
            assertTrue(results.isEmpty());
        }
    }

    // ==================== Cache Operations ====================

    @Nested
    @DisplayName("Cache Operations Tests")
    class CacheTests {

        @Test
        @DisplayName("invalidateUserCache should delegate to policy engine")
        void testInvalidateUserCache() {
            authorizationService.invalidateUserCache("user1");
            verify(policyEngine).invalidateCache("user1");
        }

        @Test
        @DisplayName("invalidateAllCaches should delegate to policy engine")
        void testInvalidateAllCaches() {
            authorizationService.invalidateAllCaches();
            verify(policyEngine).invalidateAllCache();
        }
    }
}
