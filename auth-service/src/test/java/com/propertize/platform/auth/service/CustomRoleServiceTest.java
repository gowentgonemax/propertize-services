package com.propertize.platform.auth.service;

import com.propertize.enums.UserRoleEnum;
import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.dto.CustomRoleRequest;
import com.propertize.platform.auth.entity.CustomRole;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.CustomRoleRepository;
import com.propertize.platform.auth.repository.RbacRoleRepository;
import com.propertize.platform.auth.repository.UserCustomRoleAssignmentRepository;
import com.propertize.platform.auth.repository.UserRepository;
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
@DisplayName("CustomRoleService Tests")
class CustomRoleServiceTest {

    @Mock
    private CustomRoleRepository customRoleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RbacService rbacService;
    @Mock
    private RbacRoleRepository rbacRoleRepository;
    @Mock
    private UserCustomRoleAssignmentRepository userCustomRoleAssignmentRepository;
    @Mock
    private RbacConfig rbacConfig;

    @InjectMocks
    private CustomRoleService customRoleService;

    private User buildUser(Set<UserRoleEnum> roles) {
        User u = new User();
        u.setId(10L);
        u.setRoles(roles);
        return u;
    }

    private CustomRoleRequest buildRequest(String name, Long orgId, List<String> perms, int maxLevel) {
        return CustomRoleRequest.builder()
                .roleName(name)
                .displayName("Display " + name)
                .organizationId(orgId)
                .permissions(perms)
                .maxLevel(maxLevel)
                .build();
    }

    @BeforeEach
    void defaultSetup() {
        // Feature flag: enabled by default (getCore() returns null → flag check
        // skipped)
        when(rbacConfig.getCore()).thenReturn(null);

        // No duplicate role names
        when(customRoleRepository.existsByRoleNameAndOrganizationIdAndIsActiveTrue(any(), any()))
                .thenReturn(false);

        // Save returns arg
        when(customRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // User exists with ADMIN role
        when(userRepository.findById(10L)).thenReturn(Optional.of(buildUser(Set.of(UserRoleEnum.ORGANIZATION_ADMIN))));

        // ORGANIZATION_ADMIN role has a vast permission set
        when(rbacService.getPermissionsForRole("ORGANIZATION_ADMIN"))
                .thenReturn(Set.of("tenant:view", "property:read", "maintenance:write", "admin:create"));

        // Role config for ORGANIZATION_ADMIN → level 9
        RbacConfig.RoleConfig roleConfig = new RbacConfig.RoleConfig();
        roleConfig.setLevel(9);
        when(rbacService.getRoleConfig("ORGANIZATION_ADMIN")).thenReturn(roleConfig);

        // rbac_roles mirror helpers
        when(rbacRoleRepository.existsByRoleNameAndOrganizationIdAndIsActiveTrue(any(), any()))
                .thenReturn(false);
        when(rbacRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(rbacRoleRepository.findByRoleNameAndOrganizationIdAndIsActiveTrue(any(), any()))
                .thenReturn(Optional.empty());
    }

    // ── validatePermissions ──────────────────────────────────────────────────

    @Nested
    @DisplayName("validatePermissions()")
    class ValidatePermissions {

        @Test
        @DisplayName("Returns true when requested is subset of creator's")
        void trueWhenSubset() {
            assertThat(customRoleService.validatePermissions(
                    Set.of("perm:a"), Set.of("perm:a", "perm:b"))).isTrue();
        }

        @Test
        @DisplayName("Returns false when requested exceeds creator's")
        void falseWhenExceeds() {
            assertThat(customRoleService.validatePermissions(
                    Set.of("perm:a", "perm:x"), Set.of("perm:a"))).isFalse();
        }

        @Test
        @DisplayName("Returns true for null or empty requested permissions")
        void trueForNullRequest() {
            assertThat(customRoleService.validatePermissions(null, Set.of("perm:a"))).isTrue();
            assertThat(customRoleService.validatePermissions(Collections.emptySet(), Set.of("perm:a"))).isTrue();
        }

        @Test
        @DisplayName("Returns false when creator has no permissions but request is non-empty")
        void falseWhenCreatorEmpty() {
            assertThat(customRoleService.validatePermissions(Set.of("perm:a"), null)).isFalse();
            assertThat(customRoleService.validatePermissions(Set.of("perm:a"), Collections.emptySet())).isFalse();
        }
    }

    // ── createCustomRole ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("createCustomRole()")
    class CreateCustomRole {

        @Test
        @DisplayName("Creates and saves custom role successfully")
        void createsSuccessfully() {
            CustomRoleRequest req = buildRequest("FRONT_DESK", 5L,
                    List.of("tenant:view", "property:read"), 5);

            CustomRole result = customRoleService.createCustomRole(req, 10L);

            assertThat(result.getRoleName()).isEqualTo("FRONT_DESK");
            assertThat(result.getOrganizationId()).isEqualTo(5L);
            assertThat(result.getCreatedBy()).isEqualTo(10L);
            assertThat(result.isActive()).isTrue();
            assertThat(result.isSystem()).isFalse();
            verify(customRoleRepository).save(any(CustomRole.class));
        }

        @Test
        @DisplayName("Throws IllegalStateException when allowRuntimeRoleCreation is false")
        void throwsWhenFeatureFlagDisabled() {
            RbacConfig.CoreConfig core = mock(RbacConfig.CoreConfig.class);
            when(core.getAllowRuntimeRoleCreation()).thenReturn(Boolean.FALSE);
            when(rbacConfig.getCore()).thenReturn(core);

            assertThatThrownBy(() -> customRoleService.createCustomRole(
                    buildRequest("ROLE_X", 1L, List.of("tenant:view"), 1), 10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("allowRuntimeRoleCreation");
        }

        @Test
        @DisplayName("Throws when role name already exists in organization")
        void throwsOnDuplicateName() {
            when(customRoleRepository.existsByRoleNameAndOrganizationIdAndIsActiveTrue("DUP_ROLE", 5L))
                    .thenReturn(true);

            assertThatThrownBy(() -> customRoleService.createCustomRole(
                    buildRequest("DUP_ROLE", 5L, List.of("tenant:view"), 1), 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Throws when requested permissions exceed creator's")
        void throwsWhenPermissionsExceedCreator() {
            // Creator only has limited set - explicitly override the default stub
            when(rbacService.getPermissionsForRole("ORGANIZATION_ADMIN")).thenReturn(Set.of("tenant:view"));

            assertThatThrownBy(() -> customRoleService.createCustomRole(
                    buildRequest("ROLE_Y", 5L, List.of("tenant:view", "admin:delete"), 1), 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceed creator's permissions");
        }

        @Test
        @DisplayName("Throws when maxLevel exceeds creator's level")
        void throwsWhenMaxLevelExceeds() {
            assertThatThrownBy(() -> customRoleService.createCustomRole(
                    buildRequest("HIGH_ROLE", 5L, List.of("tenant:view"), 10), 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds creator's privilege level");
        }
    }

    // ── getCustomRole ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomRole()")
    class GetCustomRole {

        @Test
        @DisplayName("Returns role when found and active")
        void returnsActiveRole() {
            CustomRole role = CustomRole.builder().id(1L).roleName("TEST").isActive(true).build();
            when(customRoleRepository.findById(1L)).thenReturn(Optional.of(role));

            assertThat(customRoleService.getCustomRole(1L).getRoleName()).isEqualTo("TEST");
        }

        @Test
        @DisplayName("Throws when role not found")
        void throwsWhenNotFound() {
            when(customRoleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customRoleService.getCustomRole(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Throws when role is inactive")
        void throwsWhenInactive() {
            CustomRole inactive = CustomRole.builder().id(2L).roleName("OLD").isActive(false).build();
            when(customRoleRepository.findById(2L)).thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> customRoleService.getCustomRole(2L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── deleteCustomRole ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteCustomRole()")
    class DeleteCustomRole {

        @Test
        @DisplayName("Soft-deletes non-system active role")
        void softDeletesRole() {
            CustomRole role = CustomRole.builder().id(5L).roleName("TO_DELETE")
                    .isActive(true).isSystem(false)
                    .organizationId(1L)
                    .build();
            when(customRoleRepository.findById(5L)).thenReturn(Optional.of(role));

            customRoleService.deleteCustomRole(5L);

            assertThat(role.isActive()).isFalse();
            verify(customRoleRepository).save(role);
        }

        @Test
        @DisplayName("Throws when trying to delete system role")
        void throwsOnSystemRole() {
            CustomRole systemRole = CustomRole.builder().id(6L).roleName("SYS")
                    .isActive(true).isSystem(true).build();
            when(customRoleRepository.findById(6L)).thenReturn(Optional.of(systemRole));

            assertThatThrownBy(() -> customRoleService.deleteCustomRole(6L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("System roles cannot be deleted");
        }
    }

    // ── getCustomRoles ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomRoles()")
    class GetCustomRoles {

        @Test
        @DisplayName("Returns list from repository for given org")
        void returnsRolesForOrg() {
            List<CustomRole> roles = List.of(
                    CustomRole.builder().id(1L).roleName("A").build(),
                    CustomRole.builder().id(2L).roleName("B").build());
            when(customRoleRepository.findByOrganizationIdAndIsActiveTrue(99L)).thenReturn(roles);

            assertThat(customRoleService.getCustomRoles(99L)).hasSize(2);
        }
    }
}
