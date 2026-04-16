package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.entity.CompositeRole;
import com.propertize.platform.auth.repository.CompositeRoleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RoleCompositionService Tests")
class RoleCompositionServiceTest {

    @Mock
    private RbacService rbacService;
    @Mock
    private RbacConfig rbacConfig;
    @Mock
    private CompositeRoleRepository compositeRoleRepository;

    @InjectMocks
    private RoleCompositionService roleCompositionService;

    @BeforeEach
    void defaultSetup() {
        // Default: all roles return empty permissions and don't exist
        when(rbacService.getPermissionsForRole(anyString())).thenReturn(Collections.emptySet());
        when(rbacService.getAllRoles()).thenReturn(Collections.emptySet());
    }

    // ── resolveEffectivePermissions(Set<String>) ─────────────────────────────

    @Nested
    @DisplayName("resolveEffectivePermissions(Set<String>)")
    class ResolveFromRoleSet {

        @Test
        @DisplayName("Returns empty set for null input")
        void returnsEmptyForNullRoles() {
            assertThat(roleCompositionService.resolveEffectivePermissions((Set<String>) null)).isEmpty();
        }

        @Test
        @DisplayName("Returns empty set for empty role set")
        void returnsEmptyForEmptyRoles() {
            assertThat(roleCompositionService.resolveEffectivePermissions(new HashSet<>())).isEmpty();
        }

        @Test
        @DisplayName("Returns union of permissions from multiple known roles")
        void returnsUnionOfPermissions() {
            when(rbacService.getPermissionsForRole("ROLE_A")).thenReturn(Set.of("perm:a", "perm:shared"));
            when(rbacService.getPermissionsForRole("ROLE_B")).thenReturn(Set.of("perm:b", "perm:shared"));
            when(rbacService.getAllRoles()).thenReturn(Set.of("ROLE_A", "ROLE_B"));

            Set<String> result = roleCompositionService
                    .resolveEffectivePermissions(Set.of("ROLE_A", "ROLE_B"));

            assertThat(result).containsExactlyInAnyOrder("perm:a", "perm:b", "perm:shared");
        }

        @Test
        @DisplayName("Ignores unknown roles silently")
        void ignoresUnknownRoles() {
            Set<String> result = roleCompositionService
                    .resolveEffectivePermissions(Set.of("UNKNOWN_ROLE"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Filters out blank role names")
        void filtersBlankRoleNames() {
            Set<String> roles = new HashSet<>(Arrays.asList("  ", "", null));
            assertThat(roleCompositionService.resolveEffectivePermissions(roles)).isEmpty();
        }

        @Test
        @DisplayName("Returns single role's permissions")
        void returnsSingleRolePermissions() {
            when(rbacService.getPermissionsForRole("MANAGER")).thenReturn(Set.of("tenant:view", "property:read"));
            when(rbacService.getAllRoles()).thenReturn(Set.of("MANAGER"));

            Set<String> result = roleCompositionService.resolveEffectivePermissions(Set.of("MANAGER"));

            assertThat(result).containsExactlyInAnyOrder("tenant:view", "property:read");
        }

        @Test
        @DisplayName("Result is unmodifiable")
        void resultIsUnmodifiable() {
            when(rbacService.getPermissionsForRole("MANAGER")).thenReturn(Set.of("perm:x"));
            when(rbacService.getAllRoles()).thenReturn(Set.of("MANAGER"));

            Set<String> result = roleCompositionService.resolveEffectivePermissions(Set.of("MANAGER"));

            assertThatThrownBy(() -> result.add("perm:y"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ── resolveEffectivePermissions(String) ──────────────────────────────────

    @Nested
    @DisplayName("resolveEffectivePermissions(String compositeRoleName)")
    class ResolveFromCompositeName {

        @Test
        @DisplayName("Returns empty set for null composite role name")
        void returnsEmptyForNull() {
            assertThat(roleCompositionService.resolveEffectivePermissions((String) null)).isEmpty();
        }

        @Test
        @DisplayName("Returns empty set for blank composite role name")
        void returnsEmptyForBlank() {
            assertThat(roleCompositionService.resolveEffectivePermissions("  ")).isEmpty();
        }

        @Test
        @DisplayName("Returns empty set when composite role not found")
        void returnsEmptyWhenNotFound() {
            when(compositeRoleRepository.findByNameAndIsActiveTrue("MISSING"))
                    .thenReturn(Optional.empty());

            assertThat(roleCompositionService.resolveEffectivePermissions("MISSING")).isEmpty();
        }

        @Test
        @DisplayName("Resolves permissions from component roles of a composite role")
        void resolvesFromCompositeRole() {
            CompositeRole composite = CompositeRole.builder()
                    .name("PM_ACCOUNTANT")
                    .componentRoles(List.of("PROPERTY_MANAGER", "ACCOUNTANT"))
                    .isActive(true)
                    .build();
            when(compositeRoleRepository.findByNameAndIsActiveTrue("PM_ACCOUNTANT"))
                    .thenReturn(Optional.of(composite));
            when(rbacService.getPermissionsForRole("PROPERTY_MANAGER"))
                    .thenReturn(Set.of("property:manage"));
            when(rbacService.getPermissionsForRole("ACCOUNTANT"))
                    .thenReturn(Set.of("finance:view"));
            when(rbacService.getAllRoles()).thenReturn(Set.of("PROPERTY_MANAGER", "ACCOUNTANT"));

            Set<String> result = roleCompositionService.resolveEffectivePermissions("PM_ACCOUNTANT");

            assertThat(result).containsExactlyInAnyOrder("property:manage", "finance:view");
        }
    }
}
