package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.entity.RbacRole;
import com.propertize.platform.auth.repository.RbacRoleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RbacSeederService — focuses on the applicableOrgTypes and
 * explicitDenials columns added in V14 that were previously dropped during
 * seeding.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RbacSeederService Tests")
class RbacSeederServiceTest {

    @Mock
    private RbacConfig rbacConfig;

    @Mock
    private RbacRoleRepository rbacRoleRepository;

    @InjectMocks
    private RbacSeederService rbacSeederService;

    /** Build a minimal RoleConfig with the two new fields populated. */
    private RbacConfig.RoleConfig roleConfig(List<String> applicableOrgTypes,
            List<String> explicitDenials) {
        RbacConfig.RoleConfig cfg = new RbacConfig.RoleConfig();
        cfg.setDescription("Test role");
        cfg.setScope("organization");
        cfg.setLevel(50);
        cfg.setCategory("test");
        cfg.setPermissions(List.of("property:read"));
        cfg.setApplicableOrgTypes(applicableOrgTypes);
        cfg.setExplicitDenials(explicitDenials);
        return cfg;
    }

    @Nested
    @DisplayName("New role creation — applicableOrgTypes and explicitDenials persistence")
    class NewRoleCreationTests {

        @Test
        @DisplayName("Should persist both applicableOrgTypes and explicitDenials as CSV for new role")
        void testNewRolePersistsBothFields() throws Exception {
            RbacConfig.RoleConfig cfg = roleConfig(
                    List.of("INDIVIDUAL_PROPERTY_OWNER", "PROPERTY_MANAGEMENT_COMPANY"),
                    List.of("tenant:delete", "ledger:write"));
            when(rbacConfig.getRoles()).thenReturn(Map.of("TEST_ROLE", cfg));
            when(rbacRoleRepository.findByRoleNameAndIsActiveTrue("TEST_ROLE"))
                    .thenReturn(Optional.empty());
            when(rbacRoleRepository.save(any(RbacRole.class))).thenAnswer(inv -> inv.getArgument(0));

            rbacSeederService.run(null);

            ArgumentCaptor<RbacRole> captor = ArgumentCaptor.forClass(RbacRole.class);
            verify(rbacRoleRepository).save(captor.capture());

            RbacRole saved = captor.getValue();
            assertEquals("INDIVIDUAL_PROPERTY_OWNER,PROPERTY_MANAGEMENT_COMPANY",
                    saved.getApplicableOrgTypes());
            assertEquals("tenant:delete,ledger:write", saved.getExplicitDenials());
        }

        @Test
        @DisplayName("Should persist empty string when applicableOrgTypes is null")
        void testApplicableOrgTypesNullBecomesEmptyString() throws Exception {
            RbacConfig.RoleConfig cfg = roleConfig(null, List.of("report:delete"));
            when(rbacConfig.getRoles()).thenReturn(Map.of("TEST_ROLE", cfg));
            when(rbacRoleRepository.findByRoleNameAndIsActiveTrue("TEST_ROLE"))
                    .thenReturn(Optional.empty());
            when(rbacRoleRepository.save(any(RbacRole.class))).thenAnswer(inv -> inv.getArgument(0));

            rbacSeederService.run(null);

            ArgumentCaptor<RbacRole> captor = ArgumentCaptor.forClass(RbacRole.class);
            verify(rbacRoleRepository).save(captor.capture());
            assertEquals("", captor.getValue().getApplicableOrgTypes());
        }

        @Test
        @DisplayName("Should persist empty string when explicitDenials is null")
        void testExplicitDenialsNullBecomesEmptyString() throws Exception {
            RbacConfig.RoleConfig cfg = roleConfig(List.of("INDIVIDUAL_PROPERTY_OWNER"), null);
            when(rbacConfig.getRoles()).thenReturn(Map.of("TEST_ROLE", cfg));
            when(rbacRoleRepository.findByRoleNameAndIsActiveTrue("TEST_ROLE"))
                    .thenReturn(Optional.empty());
            when(rbacRoleRepository.save(any(RbacRole.class))).thenAnswer(inv -> inv.getArgument(0));

            rbacSeederService.run(null);

            ArgumentCaptor<RbacRole> captor = ArgumentCaptor.forClass(RbacRole.class);
            verify(rbacRoleRepository).save(captor.capture());
            assertEquals("", captor.getValue().getExplicitDenials());
        }

        @Test
        @DisplayName("Should persist empty string when both lists are empty")
        void testEmptyListsProduceEmptyStrings() throws Exception {
            RbacConfig.RoleConfig cfg = roleConfig(Collections.emptyList(), Collections.emptyList());
            when(rbacConfig.getRoles()).thenReturn(Map.of("TEST_ROLE", cfg));
            when(rbacRoleRepository.findByRoleNameAndIsActiveTrue("TEST_ROLE"))
                    .thenReturn(Optional.empty());
            when(rbacRoleRepository.save(any(RbacRole.class))).thenAnswer(inv -> inv.getArgument(0));

            rbacSeederService.run(null);

            ArgumentCaptor<RbacRole> captor = ArgumentCaptor.forClass(RbacRole.class);
            verify(rbacRoleRepository).save(captor.capture());
            assertEquals("", captor.getValue().getApplicableOrgTypes());
            assertEquals("", captor.getValue().getExplicitDenials());
        }
    }

    @Nested
    @DisplayName("Existing role update — applicableOrgTypes and explicitDenials update")
    class ExistingRoleUpdateTests {

        @Test
        @DisplayName("Should update applicableOrgTypes and explicitDenials on existing role")
        void testExistingRoleUpdatesBothFields() throws Exception {
            RbacConfig.RoleConfig cfg = roleConfig(
                    List.of("PROPERTY_MANAGEMENT_COMPANY"),
                    List.of("billing:delete"));
            RbacRole existingRole = RbacRole.builder()
                    .roleName("TEST_ROLE")
                    .isActive(true)
                    .applicableOrgTypes("OLD_VALUE")
                    .explicitDenials("old:deny")
                    .build();

            when(rbacConfig.getRoles()).thenReturn(Map.of("TEST_ROLE", cfg));
            when(rbacRoleRepository.findByRoleNameAndIsActiveTrue("TEST_ROLE"))
                    .thenReturn(Optional.of(existingRole));
            when(rbacRoleRepository.save(any(RbacRole.class))).thenAnswer(inv -> inv.getArgument(0));

            rbacSeederService.run(null);

            ArgumentCaptor<RbacRole> captor = ArgumentCaptor.forClass(RbacRole.class);
            verify(rbacRoleRepository).save(captor.capture());

            RbacRole updated = captor.getValue();
            assertEquals("PROPERTY_MANAGEMENT_COMPANY", updated.getApplicableOrgTypes());
            assertEquals("billing:delete", updated.getExplicitDenials());
        }

        @Test
        @DisplayName("Should clear applicableOrgTypes on existing role when config list is null")
        void testExistingRoleClearsApplicableOrgTypesWhenNull() throws Exception {
            RbacConfig.RoleConfig cfg = roleConfig(null, List.of("admin:delete"));
            RbacRole existing = RbacRole.builder()
                    .roleName("TEST_ROLE").isActive(true)
                    .applicableOrgTypes("STALE_ORG_TYPE").explicitDenials("old").build();

            when(rbacConfig.getRoles()).thenReturn(Map.of("TEST_ROLE", cfg));
            when(rbacRoleRepository.findByRoleNameAndIsActiveTrue("TEST_ROLE"))
                    .thenReturn(Optional.of(existing));
            when(rbacRoleRepository.save(any(RbacRole.class))).thenAnswer(inv -> inv.getArgument(0));

            rbacSeederService.run(null);

            ArgumentCaptor<RbacRole> captor = ArgumentCaptor.forClass(RbacRole.class);
            verify(rbacRoleRepository).save(captor.capture());
            assertEquals("", captor.getValue().getApplicableOrgTypes());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should skip seeding when roles config is null")
        void testNullRolesConfigSkipsSeeding() throws Exception {
            when(rbacConfig.getRoles()).thenReturn(null);

            rbacSeederService.run(null);

            verify(rbacRoleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip seeding when roles config is empty")
        void testEmptyRolesConfigSkipsSeeding() throws Exception {
            when(rbacConfig.getRoles()).thenReturn(Collections.emptyMap());

            rbacSeederService.run(null);

            verify(rbacRoleRepository, never()).save(any());
        }
    }
}
