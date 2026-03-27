package com.propertize.platform.auth.rbac.engine;

import com.propertize.platform.auth.service.DynamicRoleComposer;
import com.propertize.platform.auth.service.RbacService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for DefaultPolicyEngine — the core policy evaluation engine.
 * Tests evaluate(), hasPermission(), listPermissions(), batch evaluation,
 * accessible resources, allowed actions, and ABAC condition evaluation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPolicyEngine Tests")
class DefaultPolicyEngineTest {

        @Mock
        private RbacService rbacService;

        @Mock
        private ConditionEvaluator ownershipEvaluator;

        @Mock
        private DynamicRoleComposer dynamicRoleComposer;

        private DefaultPolicyEngine policyEngine;

        @BeforeEach
        void setUp() {
                policyEngine = new DefaultPolicyEngine(rbacService, List.of(ownershipEvaluator), dynamicRoleComposer);
                // By default, dynamic role composer returns the same roles (no composition)
                lenient().when(dynamicRoleComposer.composeRoles(any(PolicyContext.class)))
                                .thenAnswer(invocation -> {
                                        PolicyContext ctx = invocation.getArgument(0);
                                        return new LinkedHashSet<>(ctx.getRoles());
                                });
                // Ownership evaluator supports "ownership" condition
                lenient().when(ownershipEvaluator.supports("ownership")).thenReturn(true);
        }

        // ==================== evaluate() ====================

        @Nested
        @DisplayName("evaluate() Tests")
        class EvaluateTests {

                @Test
                @DisplayName("Should allow when user has permission")
                void testAllowedPermission() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("PROPERTY_MANAGER")
                                        .build();

                        when(rbacService.hasPermission("PROPERTY_MANAGER", "property:read")).thenReturn(true);

                        PolicyDecision decision = policyEngine.evaluate(
                                        context, Resource.PROPERTY, Action.READ, Collections.emptyMap());

                        assertTrue(decision.isAllowed());
                        assertTrue(decision.getReason().contains("Permission granted"));
                        assertEquals("property", decision.getResource());
                        assertEquals("read", decision.getAction());
                        assertEquals("user1", decision.getUserId());
                        assertNotNull(decision.getMatchedPermissions());
                        assertTrue(decision.getMatchedPermissions().contains("property:read"));
                }

                @Test
                @DisplayName("Should deny when user lacks permission")
                void testDeniedPermission() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("VIEWER")
                                        .build();

                        when(rbacService.hasPermission("VIEWER", "property:delete")).thenReturn(false);

                        PolicyDecision decision = policyEngine.evaluate(
                                        context, Resource.PROPERTY, Action.DELETE, Collections.emptyMap());

                        assertFalse(decision.isAllowed());
                        assertTrue(decision.getReason().contains("does not have permission"));
                }

                @Test
                @DisplayName("Should deny when ownership ABAC condition fails")
                void testAbacConditionDeny() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("MANAGER")
                                        .build();

                        when(rbacService.hasPermission("MANAGER", "property:update")).thenReturn(true);
                        when(ownershipEvaluator.evaluate(any(), eq("ownership"), anyMap())).thenReturn(false);

                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("ownerId", "other-user");

                        PolicyDecision decision = policyEngine.evaluate(
                                        context, Resource.PROPERTY, Action.UPDATE, attributes);

                        assertFalse(decision.isAllowed());
                        assertTrue(decision.getReason().contains("Ownership condition"));
                }

                @Test
                @DisplayName("Should allow when ownership ABAC condition passes")
                void testAbacConditionAllow() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("MANAGER")
                                        .build();

                        when(rbacService.hasPermission("MANAGER", "property:update")).thenReturn(true);
                        when(ownershipEvaluator.evaluate(any(), eq("ownership"), anyMap())).thenReturn(true);

                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("ownerId", "user1");

                        PolicyDecision decision = policyEngine.evaluate(
                                        context, Resource.PROPERTY, Action.UPDATE, attributes);

                        assertTrue(decision.isAllowed());
                }

                @Test
                @DisplayName("Should include evaluatedRoles in decision")
                void testEvaluatedRolesInDecision() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("ADMIN")
                                        .role("VIEWER")
                                        .build();

                        when(rbacService.hasPermission(anyString(), anyString())).thenReturn(true);

                        PolicyDecision decision = policyEngine.evaluate(
                                        context, Resource.PROPERTY, Action.READ, Collections.emptyMap());

                        assertNotNull(decision.getEvaluatedRoles());
                        assertTrue(decision.getEvaluatedRoles().contains("ADMIN"));
                        assertTrue(decision.getEvaluatedRoles().contains("VIEWER"));
                }

                @Test
                @DisplayName("Should set configVersion and policyId in decision")
                void testDecisionMetadata() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("ADMIN")
                                        .build();

                        when(rbacService.hasPermission(anyString(), anyString())).thenReturn(true);

                        PolicyDecision decision = policyEngine.evaluate(
                                        context, Resource.PROPERTY, Action.READ, Collections.emptyMap());

                        assertEquals("3.0.0", decision.getConfigVersion());
                        assertEquals("policy-v3", decision.getPolicyId());
                        assertFalse(decision.isFromCache());
                }
        }

        // ==================== hasPermission() ====================

        @Nested
        @DisplayName("hasPermission() Tests")
        class HasPermissionTests {

                @Test
                @DisplayName("Should return true when any role has the permission")
                void testAnyRoleHasPermission() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("VIEWER")
                                        .role("EDITOR")
                                        .build();

                        when(rbacService.hasPermission("VIEWER", "property:update")).thenReturn(false);
                        when(rbacService.hasPermission("EDITOR", "property:update")).thenReturn(true);

                        assertTrue(policyEngine.hasPermission(context, "property:update"));
                }

                @Test
                @DisplayName("Should return false when no role has the permission")
                void testNoRoleHasPermission() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("VIEWER")
                                        .build();

                        when(rbacService.hasPermission("VIEWER", "property:delete")).thenReturn(false);

                        assertFalse(policyEngine.hasPermission(context, "property:delete"));
                }

                @Test
                @DisplayName("Should return false when no roles set")
                void testNoRoles() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .build();

                        assertFalse(policyEngine.hasPermission(context, "property:read"));
                }
        }

        // ==================== listPermissions() ====================

        @Nested
        @DisplayName("listPermissions() Tests")
        class ListPermissionsTests {

                @Test
                @DisplayName("Should aggregate permissions from all roles")
                void testAggregatePermissions() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("VIEWER")
                                        .role("EDITOR")
                                        .build();

                        when(rbacService.getPermissionsForRole("VIEWER"))
                                        .thenReturn(Set.of("property:read"));
                        when(rbacService.getPermissionsForRole("EDITOR"))
                                        .thenReturn(Set.of("property:read", "property:update"));

                        Set<String> perms = policyEngine.listPermissions(context);

                        assertTrue(perms.contains("property:read"));
                        assertTrue(perms.contains("property:update"));
                        assertEquals(2, perms.size());
                }

                @Test
                @DisplayName("Should return empty set when no roles")
                void testNoRoles() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .build();

                        Set<String> perms = policyEngine.listPermissions(context);
                        assertTrue(perms.isEmpty());
                }
        }

        // ==================== listAccessibleResources() ====================

        @Nested
        @DisplayName("listAccessibleResources() Tests")
        class ListAccessibleResourcesTests {

                @Test
                @DisplayName("Should extract resources from permissions")
                void testExtractResources() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("MANAGER")
                                        .build();

                        when(rbacService.getPermissionsForRole("MANAGER"))
                                        .thenReturn(Set.of("property:read", "property:update",
                                                        "tenant:read", "lease:create"));

                        Set<Resource> resources = policyEngine.listAccessibleResources(context);

                        assertTrue(resources.contains(Resource.PROPERTY));
                        assertTrue(resources.contains(Resource.TENANT));
                        assertTrue(resources.contains(Resource.LEASE));
                }
        }

        // ==================== getAllowedActions() ====================

        @Nested
        @DisplayName("getAllowedActions() Tests")
        class GetAllowedActionsTests {

                @Test
                @DisplayName("Should extract actions for a specific resource")
                void testActionsForResource() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("MANAGER")
                                        .build();

                        when(rbacService.getPermissionsForRole("MANAGER"))
                                        .thenReturn(Set.of("property:read", "property:update",
                                                        "property:create", "tenant:read"));

                        Set<Action> actions = policyEngine.getAllowedActions(context, Resource.PROPERTY);

                        assertTrue(actions.contains(Action.READ));
                        assertTrue(actions.contains(Action.UPDATE));
                        assertTrue(actions.contains(Action.CREATE));
                        assertEquals(3, actions.size());
                }
        }

        // ==================== evaluateBatch() ====================

        @Nested
        @DisplayName("evaluateBatch() Tests")
        class EvaluateBatchTests {

                @Test
                @DisplayName("Should evaluate multiple resource:action pairs")
                void testBatchEvaluation() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("MANAGER")
                                        .build();

                        when(rbacService.hasPermission("MANAGER", "property:read")).thenReturn(true);
                        when(rbacService.hasPermission("MANAGER", "property:delete")).thenReturn(false);

                        Map<Resource, Set<Action>> checks = new LinkedHashMap<>();
                        checks.put(Resource.PROPERTY, Set.of(Action.READ, Action.DELETE));

                        Map<String, PolicyDecision> results = policyEngine.evaluateBatch(context, checks);

                        assertTrue(results.get("property:read").isAllowed());
                        assertFalse(results.get("property:delete").isAllowed());
                }
        }

        // ==================== hasAnyPermission / hasAllPermissions
        // ====================

        @Nested
        @DisplayName("hasAnyPermission / hasAllPermissions Tests")
        class CompositePermissionTests {

                @Test
                @DisplayName("hasAnyPermission should return true if any matches")
                void testHasAnyPermission() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("VIEWER")
                                        .build();

                        lenient().when(rbacService.hasPermission("VIEWER", "property:delete")).thenReturn(false);
                        when(rbacService.hasPermission("VIEWER", "property:read")).thenReturn(true);

                        assertTrue(policyEngine.hasAnyPermission(context,
                                        "property:delete", "property:read"));
                }

                @Test
                @DisplayName("hasAnyPermission should return false if none match")
                void testHasAnyPermissionNone() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("VIEWER")
                                        .build();

                        when(rbacService.hasPermission("VIEWER", "property:delete")).thenReturn(false);
                        when(rbacService.hasPermission("VIEWER", "system:configure")).thenReturn(false);

                        assertFalse(policyEngine.hasAnyPermission(context,
                                        "property:delete", "system:configure"));
                }

                @Test
                @DisplayName("hasAllPermissions should return true if all match")
                void testHasAllPermissions() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("ADMIN")
                                        .build();

                        when(rbacService.hasPermission("ADMIN", "property:read")).thenReturn(true);
                        when(rbacService.hasPermission("ADMIN", "property:update")).thenReturn(true);

                        assertTrue(policyEngine.hasAllPermissions(context,
                                        "property:read", "property:update"));
                }

                @Test
                @DisplayName("hasAllPermissions should return false if any missing")
                void testHasAllPermissionsMissing() {
                        PolicyContext context = PolicyContext.builder()
                                        .userId("user1")
                                        .role("VIEWER")
                                        .build();

                        when(rbacService.hasPermission("VIEWER", "property:read")).thenReturn(true);
                        when(rbacService.hasPermission("VIEWER", "property:delete")).thenReturn(false);

                        assertFalse(policyEngine.hasAllPermissions(context,
                                        "property:read", "property:delete"));
                }
        }

        // ==================== getConfigVersion ====================

        @Test
        @DisplayName("Should return config version 3.0.0")
        void testGetConfigVersion() {
                assertEquals("3.0.0", policyEngine.getConfigVersion());
        }
}
