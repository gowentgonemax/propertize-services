package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RbacConfigService — provides RBAC config data to other
 * services.
 * Tests getRbacConfig, getRoleDetails, getAllRoles, getRolesByScope,
 * getEndpointPermissions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RbacConfigService Tests")
class RbacConfigServiceTest {

    @Mock
    private RbacConfig rbacConfig;

    @Mock
    private RbacService rbacService;

    @InjectMocks
    private RbacConfigService rbacConfigService;

    private Map<String, RbacConfig.RoleConfig> rolesMap;

    @BeforeEach
    void setUp() {
        rolesMap = new LinkedHashMap<>();
    }

    // ==================== getRbacConfig ====================

    @Nested
    @DisplayName("getRbacConfig Tests")
    class GetRbacConfigTests {

        @Test
        @DisplayName("Should return role → permissions map for all roles")
        void testRbacConfigMap() {
            rolesMap.put("ADMIN", new RbacConfig.RoleConfig());
            rolesMap.put("VIEWER", new RbacConfig.RoleConfig());
            when(rbacConfig.getRoles()).thenReturn(rolesMap);
            when(rbacService.getPermissionsForRole("ADMIN"))
                    .thenReturn(Set.of("property:read", "property:delete"));
            when(rbacService.getPermissionsForRole("VIEWER"))
                    .thenReturn(Set.of("property:read"));

            Map<String, Set<String>> config = rbacConfigService.getRbacConfig();

            assertEquals(2, config.size());
            assertTrue(config.get("ADMIN").contains("property:delete"));
            assertTrue(config.get("VIEWER").contains("property:read"));
        }

        @Test
        @DisplayName("Should return empty map when roles is null")
        void testNullRoles() {
            when(rbacConfig.getRoles()).thenReturn(null);
            assertTrue(rbacConfigService.getRbacConfig().isEmpty());
        }
    }

    // ==================== getRoleDetails ====================

    @Nested
    @DisplayName("getRoleDetails Tests")
    class GetRoleDetailsTests {

        @Test
        @DisplayName("Should return detailed role info")
        void testRoleDetails() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setDescription("Property Manager Role");
            config.setScope("organization");
            config.setLevel(3);
            config.setCategory("business");
            config.setBypassAllChecks(false);
            config.setInherits(List.of("VIEWER"));
            config.setCapabilities(Map.of("canApprove", true));
            config.setFeatures(List.of("property-management"));

            RbacConfig.RoleRestrictions restrictions = new RbacConfig.RoleRestrictions();
            restrictions.setMaxProperties(50);
            config.setRestrictions(restrictions);

            rolesMap.put("PROPERTY_MANAGER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);
            when(rbacService.getPermissionsForRole("PROPERTY_MANAGER"))
                    .thenReturn(Set.of("property:read", "property:update"));

            Map<String, Object> details = rbacConfigService.getRoleDetails("PROPERTY_MANAGER");

            assertEquals("Property Manager Role", details.get("description"));
            assertEquals("organization", details.get("scope"));
            assertEquals(3, details.get("level"));
            assertEquals("business", details.get("category"));
            assertEquals(false, details.get("bypassAllChecks"));
            assertNotNull(details.get("permissions"));
            assertNotNull(details.get("restrictions"));
            assertNotNull(details.get("capabilities"));
            assertNotNull(details.get("features"));
        }

        @Test
        @DisplayName("Should return empty map for unknown role")
        void testUnknownRole() {
            when(rbacConfig.getRoles()).thenReturn(rolesMap);
            assertTrue(rbacConfigService.getRoleDetails("UNKNOWN").isEmpty());
        }

        @Test
        @DisplayName("Should handle null roles config")
        void testNullRolesConfig() {
            when(rbacConfig.getRoles()).thenReturn(null);
            assertTrue(rbacConfigService.getRoleDetails("ANY").isEmpty());
        }
    }

    // ==================== getAllRoles ====================

    @Test
    @DisplayName("getAllRoles should delegate to RbacService")
    void testGetAllRoles() {
        when(rbacService.getAllRoles()).thenReturn(Set.of("ADMIN", "VIEWER"));

        Set<String> roles = rbacConfigService.getAllRoles();
        assertEquals(2, roles.size());
        verify(rbacService).getAllRoles();
    }

    // ==================== getRolesByScope ====================

    @Nested
    @DisplayName("getRolesByScope Tests")
    class GetRolesByScopeTests {

        @Test
        @DisplayName("Should filter roles by scope")
        void testFilterByScope() {
            RbacConfig.RoleConfig platformConfig = new RbacConfig.RoleConfig();
            platformConfig.setScope("platform");
            rolesMap.put("PLATFORM_ADMIN", platformConfig);

            RbacConfig.RoleConfig orgConfig = new RbacConfig.RoleConfig();
            orgConfig.setScope("organization");
            rolesMap.put("ORG_ADMIN", orgConfig);

            RbacConfig.RoleConfig orgConfig2 = new RbacConfig.RoleConfig();
            orgConfig2.setScope("organization");
            rolesMap.put("PROPERTY_MANAGER", orgConfig2);

            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> orgRoles = rbacConfigService.getRolesByScope("organization");

            assertEquals(2, orgRoles.size());
            assertTrue(orgRoles.contains("ORG_ADMIN"));
            assertTrue(orgRoles.contains("PROPERTY_MANAGER"));
        }

        @Test
        @DisplayName("Should return empty set when no roles match scope")
        void testNoMatchingScope() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setScope("platform");
            rolesMap.put("ADMIN", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            assertTrue(rbacConfigService.getRolesByScope("self").isEmpty());
        }

        @Test
        @DisplayName("Should return empty set when roles is null")
        void testNullRoles() {
            when(rbacConfig.getRoles()).thenReturn(null);
            assertTrue(rbacConfigService.getRolesByScope("platform").isEmpty());
        }

        @Test
        @DisplayName("Should match scope case-insensitively")
        void testCaseInsensitiveScope() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setScope("Platform");
            rolesMap.put("ADMIN", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> result = rbacConfigService.getRolesByScope("platform");
            assertEquals(1, result.size());
        }
    }

    // ==================== getEndpointPermissions ====================

    @Test
    @DisplayName("getEndpointPermissions should delegate to RbacService")
    void testGetEndpointPermissions() {
        Map<String, Map<String, String>> endpoints = Map.of(
                "/api/property/**", Map.of("GET", "property:read"));
        when(rbacService.getEndpointPermissions()).thenReturn(endpoints);

        assertEquals(endpoints, rbacConfigService.getEndpointPermissions());
    }

    // ==================== getConfigVersion ====================

    @Test
    @DisplayName("getConfigVersion should return RBAC version from config")
    void testGetConfigVersion() {
        when(rbacConfig.getVersion()).thenReturn("5.0.0");
        assertEquals("5.0.0", rbacConfigService.getConfigVersion());
    }
}
