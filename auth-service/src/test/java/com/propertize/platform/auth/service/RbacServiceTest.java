package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for RbacService — the core RBAC permission resolver.
 * Tests permission resolution, inheritance, wildcard expansion, canonical
 * normalization,
 * role checks, and SecurityContext integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RbacService Tests")
class RbacServiceTest {

    @Mock
    private RbacConfig rbacConfig;

    @InjectMocks
    private RbacService rbacService;

    private Map<String, RbacConfig.RoleConfig> rolesMap;

    @BeforeEach
    void setUp() {
        rolesMap = new LinkedHashMap<>();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== getPermissionsForRole ====================

    @Nested
    @DisplayName("getPermissionsForRole Tests")
    class GetPermissionsForRoleTests {

        @Test
        @DisplayName("Should return empty set when roles config is null")
        void testNullRolesConfig() {
            when(rbacConfig.getRoles()).thenReturn(null);
            Set<String> perms = rbacService.getPermissionsForRole("ADMIN");
            assertTrue(perms.isEmpty());
        }

        @Test
        @DisplayName("Should return empty set for unknown role")
        void testUnknownRole() {
            when(rbacConfig.getRoles()).thenReturn(rolesMap);
            Set<String> perms = rbacService.getPermissionsForRole("UNKNOWN_ROLE");
            assertTrue(perms.isEmpty());
        }

        @Test
        @DisplayName("Should return direct permissions for a role")
        void testDirectPermissions() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read", "property:create"));
            rolesMap.put("PROPERTY_MANAGER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> perms = rbacService.getPermissionsForRole("PROPERTY_MANAGER");

            assertTrue(perms.contains("property:read"));
            assertTrue(perms.contains("property:create"));
            // Should also contain canonical forms
            assertTrue(perms.contains("PROPERTY_READ"));
            assertTrue(perms.contains("PROPERTY_CREATE"));
        }

        @Test
        @DisplayName("Should resolve inherited permissions")
        void testInheritedPermissions() {
            RbacConfig.RoleConfig viewerConfig = new RbacConfig.RoleConfig();
            viewerConfig.setPermissions(List.of("property:read"));
            rolesMap.put("VIEWER", viewerConfig);

            RbacConfig.RoleConfig editorConfig = new RbacConfig.RoleConfig();
            editorConfig.setPermissions(List.of("property:update"));
            editorConfig.setInherits(List.of("VIEWER"));
            rolesMap.put("EDITOR", editorConfig);

            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> perms = rbacService.getPermissionsForRole("EDITOR");

            assertTrue(perms.contains("property:read"), "Should contain inherited read");
            assertTrue(perms.contains("property:update"), "Should contain own update");
        }

        @Test
        @DisplayName("Should expand wildcard (*) permissions")
        void testWildcardExpansion() {
            RbacConfig.RoleConfig basicConfig = new RbacConfig.RoleConfig();
            basicConfig.setPermissions(List.of("property:read", "tenant:read"));
            rolesMap.put("BASIC", basicConfig);

            RbacConfig.RoleConfig superConfig = new RbacConfig.RoleConfig();
            superConfig.setPermissions(List.of("*"));
            rolesMap.put("SUPER_ADMIN", superConfig);

            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> perms = rbacService.getPermissionsForRole("SUPER_ADMIN");

            assertTrue(perms.contains("property:read"));
            assertTrue(perms.contains("tenant:read"));
            assertFalse(perms.contains("*"), "Wildcard itself should be removed");
        }

        @Test
        @DisplayName("Should produce canonical forms — colons/dots/hyphens become underscore uppercase")
        void testCanonicalNormalization() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read", "audit-log.view"));
            rolesMap.put("ROLE_A", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> perms = rbacService.getPermissionsForRole("ROLE_A");

            assertTrue(perms.contains("property:read"));
            assertTrue(perms.contains("PROPERTY_READ"));
            assertTrue(perms.contains("audit-log.view"));
            assertTrue(perms.contains("AUDIT_LOG_VIEW"));
        }

        @Test
        @DisplayName("Should filter null and blank permissions")
        void testFiltersNullBlank() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(Arrays.asList("property:read", null, "", "  ", "tenant:read"));
            rolesMap.put("ROLE_B", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> perms = rbacService.getPermissionsForRole("ROLE_B");

            assertTrue(perms.contains("property:read"));
            assertTrue(perms.contains("tenant:read"));
            assertFalse(perms.stream().anyMatch(String::isBlank));
        }

        @Test
        @DisplayName("Should expand permission hierarchy includes")
        void testPermissionHierarchyExpansion() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:manage"));
            rolesMap.put("MANAGER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            RbacConfig.PermissionHierarchy hierarchy = new RbacConfig.PermissionHierarchy();
            hierarchy.setIncludes(List.of("property:read", "property:update", "property:delete"));
            when(rbacConfig.getPermissionHierarchy()).thenReturn(Map.of("property:manage", hierarchy));

            Set<String> perms = rbacService.getPermissionsForRole("MANAGER");

            assertTrue(perms.contains("property:manage"));
            assertTrue(perms.contains("property:read"));
            assertTrue(perms.contains("property:update"));
            assertTrue(perms.contains("property:delete"));
        }

        @Test
        @DisplayName("Should return unmodifiable set")
        void testUnmodifiable() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read"));
            rolesMap.put("TEST", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> perms = rbacService.getPermissionsForRole("TEST");
            assertThrows(UnsupportedOperationException.class, () -> perms.add("hack"));
        }
    }

    // ==================== hasPermission ====================

    @Nested
    @DisplayName("hasPermission Tests")
    class HasPermissionTests {

        @Test
        @DisplayName("Should return true when role has direct permission")
        void testRoleHasDirectPermission() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read"));
            rolesMap.put("VIEWER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            assertTrue(rbacService.hasPermission("VIEWER", "property:read"));
        }

        @Test
        @DisplayName("Should return true when role has canonical form of permission")
        void testRoleHasCanonicalPermission() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read"));
            rolesMap.put("VIEWER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            assertTrue(rbacService.hasPermission("VIEWER", "PROPERTY_READ"));
        }

        @Test
        @DisplayName("Should return false for null permission")
        void testNullPermission() {
            assertFalse(rbacService.hasPermission("ANY", null));
        }

        @Test
        @DisplayName("Should return false when role doesn't have permission")
        void testRoleLacksPermission() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read"));
            rolesMap.put("VIEWER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            assertFalse(rbacService.hasPermission("VIEWER", "property:delete"));
        }

        @Test
        @DisplayName("Should check SecurityContext for current user permission")
        void testCurrentUserHasPermission() {
            setSecurityContext("user1", List.of("property:read", "PROPERTY_READ"));
            assertTrue(rbacService.hasPermission("property:read"));
        }

        @Test
        @DisplayName("Should return false when SecurityContext has no authentication")
        void testNoAuthentication() {
            SecurityContextHolder.clearContext();
            assertFalse(rbacService.hasPermission("property:read"));
        }

        @Test
        @DisplayName("Should return false when authentication is not authenticated")
        void testNotAuthenticated() {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user1", null,
                    Collections.emptyList());
            auth.setAuthenticated(false);
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);

            assertFalse(rbacService.hasPermission("property:read"));
        }
    }

    // ==================== getAllRoles ====================

    @Nested
    @DisplayName("getAllRoles Tests")
    class GetAllRolesTests {

        @Test
        @DisplayName("Should return all role names")
        void testAllRoles() {
            rolesMap.put("ADMIN", new RbacConfig.RoleConfig());
            rolesMap.put("VIEWER", new RbacConfig.RoleConfig());
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> roles = rbacService.getAllRoles();
            assertEquals(2, roles.size());
            assertTrue(roles.contains("ADMIN"));
            assertTrue(roles.contains("VIEWER"));
        }

        @Test
        @DisplayName("Should return empty set when roles is null")
        void testNullRoles() {
            when(rbacConfig.getRoles()).thenReturn(null);
            assertTrue(rbacService.getAllRoles().isEmpty());
        }
    }

    // ==================== getAuthoritiesForRole ====================

    @Nested
    @DisplayName("getAuthoritiesForRole Tests")
    class GetAuthoritiesForRoleTests {

        @Test
        @DisplayName("Should include ROLE_ prefix authority")
        void testRolePrefixAuthority() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read"));
            rolesMap.put("VIEWER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Collection<GrantedAuthority> authorities = rbacService.getAuthoritiesForRole("VIEWER", null);

            assertTrue(authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER")));
        }

        @Test
        @DisplayName("Should include permission authorities")
        void testPermissionAuthorities() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read"));
            config.setScope("client");
            rolesMap.put("VIEWER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Collection<GrantedAuthority> authorities = rbacService.getAuthoritiesForRole("VIEWER", null);

            assertTrue(authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("property:read")));
        }

        @Test
        @DisplayName("Should include ORG_ authority when scope is client")
        void testOrgAuthority() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read"));
            config.setScope("client");
            rolesMap.put("VIEWER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Collection<GrantedAuthority> authorities = rbacService.getAuthoritiesForRole("VIEWER", "org-123");

            assertTrue(authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ORG_org-123")));
        }

        @Test
        @DisplayName("Should NOT include ORG_ when scope is not client")
        void testNoOrgAuthorityForPlatformScope() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read"));
            config.setScope("platform");
            rolesMap.put("ADMIN", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Collection<GrantedAuthority> authorities = rbacService.getAuthoritiesForRole("ADMIN", "org-123");

            assertFalse(authorities.stream()
                    .anyMatch(a -> a.getAuthority().startsWith("ORG_")));
        }
    }

    // ==================== getRolesWithPermission ====================

    @Nested
    @DisplayName("getRolesWithPermission Tests")
    class GetRolesWithPermissionTests {

        @Test
        @DisplayName("Should return roles that have the exact permission")
        void testExactMatch() {
            RbacConfig.RoleConfig adminConfig = new RbacConfig.RoleConfig();
            adminConfig.setPermissions(List.of("property:read", "property:delete"));
            rolesMap.put("ADMIN", adminConfig);

            RbacConfig.RoleConfig viewerConfig = new RbacConfig.RoleConfig();
            viewerConfig.setPermissions(List.of("property:read"));
            rolesMap.put("VIEWER", viewerConfig);

            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> roles = rbacService.getRolesWithPermission("property:read");
            assertTrue(roles.contains("ADMIN"));
            assertTrue(roles.contains("VIEWER"));
        }

        @Test
        @DisplayName("Should return empty set for non-existent permission")
        void testNoMatch() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setPermissions(List.of("property:read"));
            rolesMap.put("VIEWER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> roles = rbacService.getRolesWithPermission("property:delete");
            assertTrue(roles.isEmpty());
        }
    }

    // ==================== bypass / platform admin ====================

    @Nested
    @DisplayName("Bypass & Platform Admin Tests")
    class BypassTests {

        @Test
        @DisplayName("Should detect bypassAllChecks on role")
        void testBypassAllChecks() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setBypassAllChecks(true);
            rolesMap.put("PLATFORM_OVERSIGHT", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            assertTrue(rbacService.hasBypassAllChecks("PLATFORM_OVERSIGHT"));
        }

        @Test
        @DisplayName("Should return false for non-bypass role")
        void testNonBypassRole() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setBypassAllChecks(false);
            rolesMap.put("VIEWER", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            assertFalse(rbacService.hasBypassAllChecks("VIEWER"));
        }

        @Test
        @DisplayName("Should detect platform admin in roles collection")
        void testIsPlatformAdmin() {
            assertTrue(rbacService.isPlatformAdmin(List.of("VIEWER", "PLATFORM_OVERSIGHT")));
            assertFalse(rbacService.isPlatformAdmin(List.of("VIEWER")));
            assertFalse(rbacService.isPlatformAdmin(null));
        }

        @Test
        @DisplayName("Should check shouldBypassAllChecks across roles")
        void testShouldBypassAllChecks() {
            RbacConfig.RoleConfig bypassConfig = new RbacConfig.RoleConfig();
            bypassConfig.setBypassAllChecks(true);
            rolesMap.put("PLATFORM_OVERSIGHT", bypassConfig);

            RbacConfig.RoleConfig normalConfig = new RbacConfig.RoleConfig();
            rolesMap.put("VIEWER", normalConfig);

            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            assertTrue(rbacService.shouldBypassAllChecks(List.of("VIEWER", "PLATFORM_OVERSIGHT")));
            assertFalse(rbacService.shouldBypassAllChecks(List.of("VIEWER")));
            assertFalse(rbacService.shouldBypassAllChecks(null));
            assertFalse(rbacService.shouldBypassAllChecks(Collections.emptyList()));
        }
    }

    // ==================== hasRole / hasAnyRole ====================

    @Nested
    @DisplayName("hasRole & hasAnyRole Tests")
    class RoleCheckTests {

        @Test
        @DisplayName("Should detect role with and without ROLE_ prefix")
        void testHasRole() {
            setSecurityContext("user1", List.of("ROLE_ADMIN"));
            assertTrue(rbacService.hasRole("ADMIN"));
        }

        @Test
        @DisplayName("Should return false when role not present")
        void testHasRoleFalse() {
            setSecurityContext("user1", List.of("ROLE_VIEWER"));
            assertFalse(rbacService.hasRole("ADMIN"));
        }

        @Test
        @DisplayName("Should detect any of specified roles")
        void testHasAnyRole() {
            setSecurityContext("user1", List.of("ROLE_VIEWER", "ROLE_EDITOR"));
            assertTrue(rbacService.hasAnyRole("ADMIN", "VIEWER"));
        }

        @Test
        @DisplayName("Should return false when none of specified roles present")
        void testHasAnyRoleFalse() {
            setSecurityContext("user1", List.of("ROLE_VIEWER"));
            assertFalse(rbacService.hasAnyRole("ADMIN", "EDITOR"));
        }

        @Test
        @DisplayName("Should return false for null/empty role names")
        void testHasAnyRoleNull() {
            setSecurityContext("user1", List.of("ROLE_VIEWER"));
            assertFalse(rbacService.hasAnyRole((String[]) null));
            assertFalse(rbacService.hasAnyRole());
        }

        @Test
        @DisplayName("isSuperAdmin should check for PLATFORM_OVERSIGHT")
        void testIsSuperAdmin() {
            setSecurityContext("admin1", List.of("ROLE_PLATFORM_OVERSIGHT", "PLATFORM_OVERSIGHT"));
            assertTrue(rbacService.isSuperAdmin());
        }
    }

    // ==================== formatAccessDenied ====================

    @Nested
    @DisplayName("formatAccessDenied Tests")
    class FormatAccessDeniedTests {

        @Test
        @DisplayName("Should format access denied message with all fields")
        void testFullMessage() {
            String result = RbacService.formatAccessDenied("john", "POST", "/api/property", "property:create");
            assertTrue(result.contains("user=john"));
            assertTrue(result.contains("method=POST"));
            assertTrue(result.contains("uri=/api/property"));
            assertTrue(result.contains("missingPermission=property:create"));
        }

        @Test
        @DisplayName("Should handle null/blank username")
        void testNullUsername() {
            String result = RbacService.formatAccessDenied(null, "GET", "/api", null);
            assertTrue(result.contains("user=<anonymous>"));
        }

        @Test
        @DisplayName("Should handle null method and uri")
        void testNullMethodUri() {
            String result = RbacService.formatAccessDenied("user", null, null, null);
            assertTrue(result.contains("method=<unknown>"));
            assertTrue(result.contains("uri=<unknown>"));
            assertFalse(result.contains("missingPermission"));
        }
    }

    // ==================== Utility methods ====================

    @Nested
    @DisplayName("Utility Methods")
    class UtilityTests {

        @Test
        @DisplayName("getScopeForRole should return scope from config")
        void testGetScopeForRole() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setScope("platform");
            rolesMap.put("ADMIN", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            assertEquals("platform", rbacService.getScopeForRole("ADMIN"));
            assertNull(rbacService.getScopeForRole("UNKNOWN"));
        }

        @Test
        @DisplayName("getConfigVersion should return rbac version")
        void testGetConfigVersion() {
            when(rbacConfig.getVersion()).thenReturn("5.0.0");
            assertEquals("5.0.0", rbacService.getConfigVersion());
        }

        @Test
        @DisplayName("getRoleConfig should return role config object")
        void testGetRoleConfig() {
            RbacConfig.RoleConfig config = new RbacConfig.RoleConfig();
            config.setDescription("Admin role");
            rolesMap.put("ADMIN", config);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            assertEquals("Admin role", rbacService.getRoleConfig("ADMIN").getDescription());
            assertNull(rbacService.getRoleConfig("UNKNOWN"));
        }

        @Test
        @DisplayName("getEndpointPermissions should return endpoint map")
        void testGetEndpointPermissions() {
            Map<String, Map<String, String>> endpoints = Map.of(
                    "/api/property/**", Map.of("GET", "property:read"));
            when(rbacConfig.getEndpoints()).thenReturn(endpoints);

            assertEquals(endpoints, rbacService.getEndpointPermissions());
        }

        @Test
        @DisplayName("getEndpointPermissions should return empty map when null")
        void testGetEndpointPermissionsNull() {
            when(rbacConfig.getEndpoints()).thenReturn(null);
            assertTrue(rbacService.getEndpointPermissions().isEmpty());
        }
    }

    // ==================== getExplicitDenialsForRoles ====================

    @Nested
    @DisplayName("getExplicitDenialsForRoles Tests")
    class GetExplicitDenialsForRolesTests {

        @Test
        @DisplayName("Should return empty set when roles is null")
        void testNullRoles() {
            Set<String> result = rbacService.getExplicitDenialsForRoles(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty set when roles is empty")
        void testEmptyRoles() {
            Set<String> result = rbacService.getExplicitDenialsForRoles(Collections.emptySet());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty set when rbacConfig.getRoles() is null")
        void testNullRolesConfig() {
            when(rbacConfig.getRoles()).thenReturn(null);
            Set<String> result = rbacService.getExplicitDenialsForRoles(Set.of("ORGANIZATION_OWNER"));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return denials for a role with explicit denials configured")
        void testRoleWithExplicitDenials() {
            RbacConfig.RoleConfig cfg = new RbacConfig.RoleConfig();
            cfg.setExplicitDenials(List.of("EMPLOYEE_MANAGE", "PAYROLL_MANAGE", "EMPLOYEE_CREATE"));
            rolesMap.put("ORGANIZATION_OWNER", cfg);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> denials = rbacService.getExplicitDenialsForRoles(Set.of("ORGANIZATION_OWNER"));

            assertEquals(3, denials.size());
            assertTrue(denials.contains("EMPLOYEE_MANAGE"));
            assertTrue(denials.contains("PAYROLL_MANAGE"));
            assertTrue(denials.contains("EMPLOYEE_CREATE"));
        }

        @Test
        @DisplayName("Should union denials across multiple roles")
        void testMultipleRolesDenialsUnioned() {
            RbacConfig.RoleConfig ownerCfg = new RbacConfig.RoleConfig();
            ownerCfg.setExplicitDenials(List.of("EMPLOYEE_MANAGE", "PAYROLL_MANAGE"));

            RbacConfig.RoleConfig adminCfg = new RbacConfig.RoleConfig();
            adminCfg.setExplicitDenials(List.of("ORGANIZATION_DELETE", "PAYROLL_MANAGE"));

            rolesMap.put("ORGANIZATION_OWNER", ownerCfg);
            rolesMap.put("ORGANIZATION_ADMIN", adminCfg);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> denials = rbacService.getExplicitDenialsForRoles(
                    Set.of("ORGANIZATION_OWNER", "ORGANIZATION_ADMIN"));

            assertEquals(3, denials.size(), "Duplicate PAYROLL_MANAGE should appear once");
            assertTrue(denials.contains("EMPLOYEE_MANAGE"));
            assertTrue(denials.contains("PAYROLL_MANAGE"));
            assertTrue(denials.contains("ORGANIZATION_DELETE"));
        }

        @Test
        @DisplayName("Should return empty set for a role with no explicit denials")
        void testRoleWithNoExplicitDenials() {
            RbacConfig.RoleConfig cfg = new RbacConfig.RoleConfig();
            cfg.setExplicitDenials(null);
            rolesMap.put("TEAM_MEMBER", cfg);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> result = rbacService.getExplicitDenialsForRoles(Set.of("TEAM_MEMBER"));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should ignore blank/null entries in the denials list")
        void testBlankEntriesIgnored() {
            RbacConfig.RoleConfig cfg = new RbacConfig.RoleConfig();
            cfg.setExplicitDenials(Arrays.asList("EMPLOYEE_MANAGE", "  ", null, "PAYROLL_MANAGE"));
            rolesMap.put("SOLO_OWNER", cfg);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> denials = rbacService.getExplicitDenialsForRoles(Set.of("SOLO_OWNER"));
            assertEquals(2, denials.size());
            assertFalse(denials.contains("  "));
            assertFalse(denials.contains(null));
        }

        @Test
        @DisplayName("Returned set should be unmodifiable")
        void testReturnedSetIsUnmodifiable() {
            RbacConfig.RoleConfig cfg = new RbacConfig.RoleConfig();
            cfg.setExplicitDenials(List.of("EMPLOYEE_MANAGE"));
            rolesMap.put("ORGANIZATION_OWNER", cfg);
            when(rbacConfig.getRoles()).thenReturn(rolesMap);

            Set<String> denials = rbacService.getExplicitDenialsForRoles(Set.of("ORGANIZATION_OWNER"));
            assertThrows(UnsupportedOperationException.class, () -> denials.add("SOMETHING"));
        }
    }

    // ==================== Helpers ====================

    private void setSecurityContext(String username, List<String> authorities) {
        List<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
                grantedAuthorities);

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }
}
