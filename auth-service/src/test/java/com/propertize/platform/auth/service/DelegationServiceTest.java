package com.propertize.platform.auth.service;

import com.propertize.platform.auth.entity.*;
import com.propertize.platform.auth.repository.DelegationRepository;
import com.propertize.platform.auth.repository.DelegationRuleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DelegationService Tests")
class DelegationServiceTest {

    @Mock
    private DelegationRepository delegationRepository;
    @Mock
    private DelegationRuleRepository delegationRuleRepository;
    @Mock
    private TemporalPermissionService temporalPermissionService;

    @InjectMocks
    private DelegationService delegationService;

    private DelegationRule buildRule(String delegatorRole, String permissions,
            String allowedRoles, int maxHours,
            boolean requiresReason, boolean requiresApproval) {
        return DelegationRule.builder()
                .id(1L)
                .delegatorRole(delegatorRole)
                .delegatablePermissions(permissions)
                .allowedDelegateRoles(allowedRoles)
                .maxDurationHours(maxHours)
                .requiresReason(requiresReason)
                .requiresApproval(requiresApproval)
                .maxChainDepth(1)
                .isActive(true)
                .build();
    }

    @BeforeEach
    void defaultSetup() {
        when(delegationRepository.countByDelegateUserIdAndPermissionAndStatus(any(), any(), any()))
                .thenReturn(0L);
        when(delegationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        TemporalPermission mockTp = mock(TemporalPermission.class);
        when(mockTp.getId()).thenReturn(999L);
        when(temporalPermissionService.grantTemporaryPermission(any(), anyString(), any(), anyString(), any()))
                .thenReturn(mockTp);
    }

    // ── Input validation ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("Throws when delegatorUserId is null")
        void throwsOnNullDelegatorId() {
            assertThatThrownBy(() -> delegationService.delegatePermission(null, 2L, "perm:x", 8,
                    "reason", "MANAGER", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Delegator user ID");
        }

        @Test
        @DisplayName("Throws when delegateUserId is null")
        void throwsOnNullDelegateId() {
            assertThatThrownBy(() -> delegationService.delegatePermission(1L, null, "perm:x", 8,
                    "reason", "MANAGER", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Delegate user ID");
        }

        @Test
        @DisplayName("Throws when permission is blank")
        void throwsOnBlankPermission() {
            assertThatThrownBy(() -> delegationService.delegatePermission(1L, 2L, "  ", 8,
                    "reason", "MANAGER", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Permission");
        }

        @Test
        @DisplayName("Throws when reason is blank")
        void throwsOnBlankReason() {
            assertThatThrownBy(() -> delegationService.delegatePermission(1L, 2L, "perm:x", 8,
                    "  ", "MANAGER", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reason");
        }

        @Test
        @DisplayName("Throws when delegator role is blank")
        void throwsOnBlankDelegatorRole() {
            assertThatThrownBy(() -> delegationService.delegatePermission(1L, 2L, "perm:x", 8,
                    "reason", "  ", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Delegator role");
        }

        @Test
        @DisplayName("Throws when delegator and delegate are the same user")
        void throwsOnSelfDelegation() {
            assertThatThrownBy(() -> delegationService.delegatePermission(5L, 5L, "perm:x", 8,
                    "reason", "MANAGER", "MANAGER", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("themselves");
        }
    }

    // ── Rule lookup ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Delegation rule enforcement")
    class RuleEnforcement {

        @Test
        @DisplayName("Throws when no active rule exists for delegator role")
        void throwsWhenNoRuleFound() {
            when(delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue("UNKNOWN_ROLE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> delegationService.delegatePermission(1L, 2L, "perm:x", 8,
                    "reason", "UNKNOWN_ROLE", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No active delegation rule");
        }

        @Test
        @DisplayName("Throws when permission is not delegatable by rule")
        void throwsWhenPermissionNotDelegatable() {
            DelegationRule rule = buildRule("MANAGER", "maintenance:view", "AGENT", 168, true, false);
            when(delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue("MANAGER"))
                    .thenReturn(Optional.of(rule));

            assertThatThrownBy(() -> delegationService.delegatePermission(1L, 2L, "admin:delete", 8,
                    "reason", "MANAGER", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not delegatable");
        }

        @Test
        @DisplayName("Throws when delegate role is not in allowed roles")
        void throwsWhenRoleNotAllowed() {
            DelegationRule rule = buildRule("MANAGER", "maintenance:view", "AGENT", 168, true, false);
            when(delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue("MANAGER"))
                    .thenReturn(Optional.of(rule));

            assertThatThrownBy(() -> delegationService.delegatePermission(1L, 2L, "maintenance:view", 8,
                    "reason", "MANAGER", "ADMIN", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not an allowed delegation target");
        }

        @Test
        @DisplayName("Throws when duration exceeds rule maximum")
        void throwsWhenDurationExceedsMax() {
            DelegationRule rule = buildRule("MANAGER", "maintenance:view", "AGENT", 8, true, false);
            when(delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue("MANAGER"))
                    .thenReturn(Optional.of(rule));

            assertThatThrownBy(() -> delegationService.delegatePermission(1L, 2L, "maintenance:view", 24,
                    "reason", "MANAGER", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds maximum");
        }

        @Test
        @DisplayName("Throws when duration is less than 1 hour")
        void throwsWhenDurationZero() {
            DelegationRule rule = buildRule("MANAGER", "maintenance:view", "AGENT", 168, true, false);
            when(delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue("MANAGER"))
                    .thenReturn(Optional.of(rule));

            assertThatThrownBy(() -> delegationService.delegatePermission(1L, 2L, "maintenance:view", 0,
                    "reason", "MANAGER", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 1 hour");
        }

        @Test
        @DisplayName("Throws when duplicate active delegation exists")
        void throwsOnDuplicateDelegation() {
            DelegationRule rule = buildRule("MANAGER", "maintenance:view", "AGENT", 168, true, false);
            when(delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue("MANAGER"))
                    .thenReturn(Optional.of(rule));
            when(delegationRepository.countByDelegateUserIdAndPermissionAndStatus(
                    2L, "maintenance:view", DelegationStatus.ACTIVE)).thenReturn(1L);

            assertThatThrownBy(() -> delegationService.delegatePermission(1L, 2L, "maintenance:view", 8,
                    "reason", "MANAGER", "AGENT", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already has an active delegation");
        }
    }

    // ── Successful delegation ────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful delegation creation")
    class SuccessfulDelegation {

        @Test
        @DisplayName("Creates ACTIVE delegation when rule does not require approval")
        void createsActiveDelegation() {
            DelegationRule rule = buildRule("MANAGER", "maintenance:view", "AGENT", 168, true, false);
            when(delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue("MANAGER"))
                    .thenReturn(Optional.of(rule));

            Delegation result = delegationService.delegatePermission(1L, 2L, "maintenance:view", 8,
                    "testing", "MANAGER", "AGENT", null, 10L);

            assertThat(result.getDelegatorUserId()).isEqualTo(1L);
            assertThat(result.getDelegateUserId()).isEqualTo(2L);
            assertThat(result.getPermission()).isEqualTo("maintenance:view");
            assertThat(result.getStatus()).isEqualTo(DelegationStatus.ACTIVE);
        }

        @Test
        @DisplayName("Creates PENDING_APPROVAL delegation when rule requires approval")
        void createsPendingDelegation() {
            DelegationRule rule = buildRule("MANAGER", "maintenance:view", "AGENT", 168, false, true);
            when(delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue("MANAGER"))
                    .thenReturn(Optional.of(rule));

            Delegation result = delegationService.delegatePermission(1L, 2L, "maintenance:view", 4,
                    "reason", "MANAGER", "AGENT", null, 10L);

            assertThat(result.getStatus()).isEqualTo(DelegationStatus.PENDING_APPROVAL);
        }
    }
}
