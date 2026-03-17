package com.propertize.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.platform.auth.dto.AuthorizationRequest;
import com.propertize.platform.auth.dto.AuthorizationResponse;
import com.propertize.platform.auth.service.AuthorizationService;
import com.propertize.platform.auth.service.RbacConfigService;
import com.propertize.platform.auth.service.RbacService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RbacController — the REST API for centralized RBAC.
 * Tests all 10 endpoints: authorize, permissions, roles, config, cache.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RbacController Tests")
class RbacControllerTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private RbacConfigService rbacConfigService;

    @Mock
    private RbacService rbacService;

    @InjectMocks
    private RbacController rbacController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== POST /authorize ====================

    @Nested
    @DisplayName("POST /authorize Tests")
    class AuthorizeTests {

        @Test
        @DisplayName("Should return authorized response")
        void testAuthorized() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .resource("property")
                    .action("read")
                    .roles(List.of("MANAGER"))
                    .build();

            AuthorizationResponse mockResponse = AuthorizationResponse.builder()
                    .authorized(true)
                    .reason("Permission granted")
                    .matchedPermissions(List.of("property:read"))
                    .build();

            when(authorizationService.authorize(any())).thenReturn(mockResponse);

            ResponseEntity<AuthorizationResponse> response = rbacController.authorize(request);

            assertEquals(200, response.getStatusCode().value());
            assertTrue(response.getBody().isAuthorized());
            assertEquals("Permission granted", response.getBody().getReason());
        }

        @Test
        @DisplayName("Should return denied response")
        void testDenied() {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .userId("user1")
                    .resource("system")
                    .action("delete")
                    .roles(List.of("VIEWER"))
                    .build();

            AuthorizationResponse mockResponse = AuthorizationResponse.builder()
                    .authorized(false)
                    .reason("Permission denied: system:delete")
                    .build();

            when(authorizationService.authorize(any())).thenReturn(mockResponse);

            ResponseEntity<AuthorizationResponse> response = rbacController.authorize(request);

            assertEquals(200, response.getStatusCode().value());
            assertFalse(response.getBody().isAuthorized());
        }
    }

    // ==================== GET /permissions/{role} ====================

    @Nested
    @DisplayName("GET /permissions/{role} Tests")
    class GetPermissionsTests {

        @Test
        @DisplayName("Should return permissions for valid role")
        void testPermissionsFound() {
            when(rbacService.getPermissionsForRole("ADMIN"))
                    .thenReturn(Set.of("property:read", "property:delete"));
            when(rbacService.getScopeForRole("ADMIN")).thenReturn("platform");

            ResponseEntity<Map<String, Object>> response = rbacController.getPermissionsForRole("ADMIN");

            assertEquals(200, response.getStatusCode().value());
            assertEquals("ADMIN", response.getBody().get("role"));
            assertEquals("platform", response.getBody().get("scope"));
            assertEquals(2, response.getBody().get("count"));
        }

        @Test
        @DisplayName("Should return 404 for unknown role")
        void testPermissionsNotFound() {
            when(rbacService.getPermissionsForRole("UNKNOWN"))
                    .thenReturn(Collections.emptySet());

            ResponseEntity<Map<String, Object>> response = rbacController.getPermissionsForRole("UNKNOWN");

            assertEquals(404, response.getStatusCode().value());
        }
    }

    // ==================== POST /permissions/resolve ====================

    @Nested
    @DisplayName("POST /permissions/resolve Tests")
    class ResolvePermissionsTests {

        @Test
        @DisplayName("Should resolve permissions for multiple roles")
        void testResolve() {
            when(authorizationService.getPermissionsForRoles(anyList()))
                    .thenReturn(Set.of("property:read", "property:update"));
            when(rbacService.shouldBypassAllChecks(anyList())).thenReturn(false);

            Map<String, List<String>> request = Map.of("roles", List.of("VIEWER", "EDITOR"));
            ResponseEntity<Map<String, Object>> response = rbacController.resolvePermissions(request);

            assertEquals(200, response.getStatusCode().value());
            assertEquals(2, response.getBody().get("count"));
            assertEquals(false, response.getBody().get("bypassAllChecks"));
        }

        @Test
        @DisplayName("Should return 400 when roles missing")
        void testMissingRoles() {
            Map<String, List<String>> request = Map.of("roles", Collections.emptyList());
            ResponseEntity<Map<String, Object>> response = rbacController.resolvePermissions(request);
            assertEquals(400, response.getStatusCode().value());
        }
    }

    // ==================== POST /permissions/check ====================

    @Nested
    @DisplayName("POST /permissions/check Tests")
    class CheckPermissionsTests {

        @Test
        @DisplayName("Should batch check permissions")
        void testBatchCheck() {
            Map<String, Boolean> mockResults = new LinkedHashMap<>();
            mockResults.put("property:read", true);
            mockResults.put("property:delete", false);
            when(authorizationService.checkPermissions(anyString(), anyList(), anyList()))
                    .thenReturn(mockResults);

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("userId", "user1");
            request.put("roles", List.of("VIEWER"));
            request.put("permissions", List.of("property:read", "property:delete"));

            ResponseEntity<Map<String, Boolean>> response = rbacController.checkPermissions(request);

            assertEquals(200, response.getStatusCode().value());
            assertTrue(response.getBody().get("property:read"));
            assertFalse(response.getBody().get("property:delete"));
        }
    }

    // ==================== GET /roles ====================

    @Nested
    @DisplayName("GET /roles Tests")
    class GetAllRolesTests {

        @Test
        @DisplayName("Should list all roles with count and version")
        void testGetAllRoles() {
            when(rbacConfigService.getAllRoles()).thenReturn(Set.of("ADMIN", "VIEWER", "EDITOR"));
            when(rbacConfigService.getConfigVersion()).thenReturn("5.0.0");

            ResponseEntity<Map<String, Object>> response = rbacController.getAllRoles();

            assertEquals(200, response.getStatusCode().value());
            assertEquals(3, response.getBody().get("count"));
            assertEquals("5.0.0", response.getBody().get("version"));
        }
    }

    // ==================== GET /roles/{role} ====================

    @Nested
    @DisplayName("GET /roles/{role} Tests")
    class GetRoleDetailsTests {

        @Test
        @DisplayName("Should return role details for valid role")
        void testRoleDetails() {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("description", "Admin");
            details.put("scope", "platform");
            when(rbacConfigService.getRoleDetails("ADMIN")).thenReturn(details);

            ResponseEntity<Map<String, Object>> response = rbacController.getRoleDetails("ADMIN");

            assertEquals(200, response.getStatusCode().value());
            assertEquals("Admin", response.getBody().get("description"));
            assertEquals("ADMIN", response.getBody().get("role"));
        }

        @Test
        @DisplayName("Should return 404 for unknown role")
        void testRoleDetailsNotFound() {
            when(rbacConfigService.getRoleDetails("UNKNOWN"))
                    .thenReturn(Collections.emptyMap());

            ResponseEntity<Map<String, Object>> response = rbacController.getRoleDetails("UNKNOWN");

            assertEquals(404, response.getStatusCode().value());
        }
    }

    // ==================== GET /roles/scope/{scope} ====================

    @Test
    @DisplayName("Should return roles filtered by scope")
    void testGetRolesByScope() {
        when(rbacConfigService.getRolesByScope("platform"))
                .thenReturn(Set.of("PLATFORM_ADMIN", "PLATFORM_OVERSIGHT"));

        ResponseEntity<Map<String, Object>> response = rbacController.getRolesByScope("platform");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("platform", response.getBody().get("scope"));
        assertEquals(2, response.getBody().get("count"));
    }

    // ==================== GET /rbac/config ====================

    @Test
    @DisplayName("Should return full RBAC config")
    void testGetRbacConfig() {
        Map<String, Set<String>> config = new LinkedHashMap<>();
        config.put("ADMIN", Set.of("property:read", "property:delete"));
        config.put("VIEWER", Set.of("property:read"));
        when(rbacConfigService.getRbacConfig()).thenReturn(config);
        when(rbacConfigService.getConfigVersion()).thenReturn("5.0.0");

        ResponseEntity<Map<String, Object>> response = rbacController.getRbacConfig();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("5.0.0", response.getBody().get("version"));
        assertEquals(2, response.getBody().get("totalRoles"));
    }

    // ==================== GET /rbac/endpoints ====================

    @Test
    @DisplayName("Should return endpoint permission mappings")
    void testGetEndpointPermissions() {
        Map<String, Map<String, String>> endpoints = Map.of(
                "/api/property/**", Map.of("GET", "property:read"));
        when(rbacConfigService.getEndpointPermissions()).thenReturn(endpoints);

        ResponseEntity<Map<String, Map<String, String>>> response = rbacController.getEndpointPermissions();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    // ==================== POST /cache/invalidate ====================

    @Nested
    @DisplayName("POST /cache/invalidate Tests")
    class CacheInvalidateTests {

        @Test
        @DisplayName("Should invalidate cache for specific user")
        void testInvalidateUser() {
            ResponseEntity<Map<String, String>> response = rbacController.invalidateCache("user1");

            assertEquals(200, response.getStatusCode().value());
            assertTrue(response.getBody().get("message").contains("user1"));
            verify(authorizationService).invalidateUserCache("user1");
        }

        @Test
        @DisplayName("Should invalidate all caches when no userId")
        void testInvalidateAll() {
            ResponseEntity<Map<String, String>> response = rbacController.invalidateCache(null);

            assertEquals(200, response.getStatusCode().value());
            assertTrue(response.getBody().get("message").contains("All"));
            verify(authorizationService).invalidateAllCaches();
        }

        @Test
        @DisplayName("Should invalidate all caches when userId is blank")
        void testInvalidateBlankUserId() {
            ResponseEntity<Map<String, String>> response = rbacController.invalidateCache("  ");

            assertEquals(200, response.getStatusCode().value());
            verify(authorizationService).invalidateAllCaches();
        }
    }
}
