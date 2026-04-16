package com.propertize.platform.auth.controller;

import com.propertize.platform.auth.dto.CustomRoleRequest;
import com.propertize.platform.auth.dto.CustomRoleResponse;
import com.propertize.platform.auth.entity.CustomRole;
import com.propertize.platform.auth.entity.RbacRole;
import com.propertize.platform.auth.entity.UserCustomRoleAssignment;
import com.propertize.platform.auth.repository.UserRepository;
import com.propertize.platform.auth.service.CustomRoleService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomRoleController Tests")
class CustomRoleControllerTest {

    @Mock
    private CustomRoleService customRoleService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomRoleController controller;

    private CustomRole buildRole(Long id, String name) {
        return CustomRole.builder()
                .id(id)
                .roleName(name)
                .displayName("Display " + name)
                .organizationId(1L)
                .permissions("tenant:view,property:read")
                .isActive(true)
                .isSystem(false)
                .maxLevel(3)
                .build();
    }

    // ── POST /custom-roles ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/rbac/custom-roles — createCustomRole()")
    class CreateCustomRole {

        @Test
        @DisplayName("Returns 201 with created role on success")
        void returns201OnSuccess() {
            when(customRoleService.createCustomRole(any(), any()))
                    .thenReturn(buildRole(1L, "FRONT_DESK"));

            CustomRoleRequest req = CustomRoleRequest.builder()
                    .roleName("FRONT_DESK").displayName("Front Desk Agent")
                    .organizationId(1L).permissions(List.of("tenant:view"))
                    .build();

            ResponseEntity<CustomRoleResponse> response = controller.createCustomRole(req, "10");

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getBody().getRoleName()).isEqualTo("FRONT_DESK");
        }
    }

    // ── GET /custom-roles ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/rbac/custom-roles — listCustomRoles()")
    class ListCustomRoles {

        @Test
        @DisplayName("Returns all custom roles for an org")
        void returnsRolesForOrg() {
            when(customRoleService.getCustomRoles(1L))
                    .thenReturn(List.of(buildRole(1L, "ROLE_A"), buildRole(2L, "ROLE_B")));

            ResponseEntity<List<CustomRoleResponse>> response = controller.listCustomRoles(1L);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("Returns empty list when no roles exist")
        void returnsEmptyList() {
            when(customRoleService.getCustomRoles(99L)).thenReturn(List.of());

            ResponseEntity<List<CustomRoleResponse>> response = controller.listCustomRoles(99L);

            assertThat(response.getBody()).isEmpty();
        }
    }

    // ── GET /custom-roles/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/rbac/custom-roles/{id} — getCustomRole()")
    class GetCustomRole {

        @Test
        @DisplayName("Returns 200 with role and effective permissions")
        void returns200WithEffectivePerms() {
            when(customRoleService.getCustomRole(1L)).thenReturn(buildRole(1L, "MANAGER_LITE"));
            when(customRoleService.resolveCustomRolePermissions(1L))
                    .thenReturn(Set.of("tenant:view", "property:read"));

            ResponseEntity<CustomRoleResponse> response = controller.getCustomRole(1L);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getEffectivePermissions())
                    .containsExactlyInAnyOrder("tenant:view", "property:read");
        }
    }

    // ── DELETE /custom-roles/{id} ─────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/rbac/custom-roles/{id} — deleteCustomRole()")
    class DeleteCustomRole {

        @Test
        @DisplayName("Returns 200 with success message")
        void returns200WithMessage() {
            doNothing().when(customRoleService).deleteCustomRole(5L);

            ResponseEntity<Map<String, String>> response = controller.deleteCustomRole(5L);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("message", "Custom role deleted successfully");
        }
    }

    // ── POST /custom-roles/{id}/assign ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/rbac/custom-roles/{id}/assign — assignRole()")
    class AssignRole {

        @Test
        @DisplayName("Returns 400 when userId is missing from body")
        void returns400WhenUserIdMissing() {
            Map<String, Long> body = Map.of("organizationId", 1L); // no userId

            ResponseEntity<Map<String, Object>> response = controller.assignRole(1L, body, "10");

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("Returns 400 when organizationId is missing from body")
        void returns400WhenOrgIdMissing() {
            Map<String, Long> body = Map.of("userId", 42L); // no orgId

            ResponseEntity<Map<String, Object>> response = controller.assignRole(1L, body, "10");

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("Returns 201 on successful role assignment")
        void returns201OnSuccess() {
            RbacRole role = RbacRole.builder().id(1L).roleName("FRONT_DESK").build();
            UserCustomRoleAssignment assignment = UserCustomRoleAssignment.builder()
                    .id(100L).userId(42L).rbacRole(role).organizationId(1L).isActive(true).build();
            when(customRoleService.assignCustomRole(anyLong(), anyLong(), anyLong(), any()))
                    .thenReturn(assignment);

            Map<String, Long> body = Map.of("userId", 42L, "organizationId", 1L);
            ResponseEntity<Map<String, Object>> response = controller.assignRole(1L, body, "10");

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getBody()).containsEntry("message", "Role assigned successfully");
        }
    }

    // ── DELETE /custom-roles/{id}/assign/{userId} ─────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/rbac/custom-roles/{id}/assign/{userId} — unassignRole()")
    class UnassignRole {

        @Test
        @DisplayName("Returns 200 on successful revocation")
        void returns200OnSuccess() {
            doNothing().when(customRoleService).unassignCustomRole(1L, 42L);

            ResponseEntity<Map<String, String>> response = controller.unassignRole(1L, 42L);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("message", "Role revoked successfully");
        }
    }
}
