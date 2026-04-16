package com.propertize.platform.auth.service;

import com.propertize.platform.auth.entity.*;
import com.propertize.platform.auth.repository.IpAccessRuleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IpAccessService Tests")
class IpAccessServiceTest {

    @Mock
    private IpAccessRuleRepository ipAccessRuleRepository;
    @InjectMocks
    private IpAccessService ipAccessService;

    private IpAccessRule buildRule(IpRuleType type, IpRuleScope scope, String pattern) {
        return IpAccessRule.builder()
                .ruleType(type)
                .scope(scope)
                .ipPattern(pattern)
                .description("test rule")
                .isActive(true)
                .build();
    }

    @BeforeEach
    void defaultEmptyRules() {
        when(ipAccessRuleRepository.findByScopeAndIsActiveTrue(any())).thenReturn(Collections.emptyList());
        when(ipAccessRuleRepository.findByScopeAndScopeValueAndIsActiveTrue(any(), any()))
                .thenReturn(Collections.emptyList());
    }

    // ── isIpAllowed ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isIpAllowed() - default allow")
    class DefaultAllow {

        @Test
        @DisplayName("Allows IP when no rules exist")
        void allowsWhenNoRules() {
            assertThat(ipAccessService.isIpAllowed("192.168.1.1", null, null, null)).isTrue();
        }

        @Test
        @DisplayName("Allows null IP (treated as blank — default allow)")
        void allowsNullIp() {
            assertThat(ipAccessService.isIpAllowed(null, null, null, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("isIpAllowed() - GLOBAL blacklist")
    class GlobalBlacklist {

        @Test
        @DisplayName("Denies IP matched by GLOBAL blacklist exact pattern")
        void deniesExactHit() {
            IpAccessRule rule = buildRule(IpRuleType.BLACKLIST, IpRuleScope.GLOBAL, "10.0.0.5");
            when(ipAccessRuleRepository.findByScopeAndIsActiveTrue(IpRuleScope.GLOBAL))
                    .thenReturn(List.of(rule));

            assertThat(ipAccessService.isIpAllowed("10.0.0.5", null, null, null)).isFalse();
        }

        @Test
        @DisplayName("Allows IP not matched by GLOBAL blacklist")
        void allowsNonMatchedIp() {
            IpAccessRule rule = buildRule(IpRuleType.BLACKLIST, IpRuleScope.GLOBAL, "10.0.0.5");
            when(ipAccessRuleRepository.findByScopeAndIsActiveTrue(IpRuleScope.GLOBAL))
                    .thenReturn(List.of(rule));

            assertThat(ipAccessService.isIpAllowed("10.0.0.6", null, null, null)).isTrue();
        }

        @Test
        @DisplayName("Denies expired global blacklist rule is ignored → IP allowed")
        void expiredBlacklistIsIgnored() {
            IpAccessRule expired = buildRule(IpRuleType.BLACKLIST, IpRuleScope.GLOBAL, "10.0.0.5");
            expired.setExpiresAt(LocalDateTime.now().minusHours(1));
            when(ipAccessRuleRepository.findByScopeAndIsActiveTrue(IpRuleScope.GLOBAL))
                    .thenReturn(List.of(expired));

            assertThat(ipAccessService.isIpAllowed("10.0.0.5", null, null, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("isIpAllowed() - GLOBAL whitelist")
    class GlobalWhitelist {

        @Test
        @DisplayName("Allows IP matching GLOBAL whitelist")
        void allowsWhitelistedIp() {
            IpAccessRule rule = buildRule(IpRuleType.WHITELIST, IpRuleScope.GLOBAL, "172.16.0.1");
            when(ipAccessRuleRepository.findByScopeAndIsActiveTrue(IpRuleScope.GLOBAL))
                    .thenReturn(List.of(rule));

            assertThat(ipAccessService.isIpAllowed("172.16.0.1", null, null, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("isIpAllowed() - wildcard patterns")
    class WildcardPatterns {

        @Test
        @DisplayName("Denies IP matching wildcard blacklist pattern")
        void deniesWildcardMatch() {
            IpAccessRule rule = buildRule(IpRuleType.BLACKLIST, IpRuleScope.GLOBAL, "192.168.1.*");
            when(ipAccessRuleRepository.findByScopeAndIsActiveTrue(IpRuleScope.GLOBAL))
                    .thenReturn(List.of(rule));

            assertThat(ipAccessService.isIpAllowed("192.168.1.100", null, null, null)).isFalse();
        }

        @Test
        @DisplayName("Allows IP not matching wildcard pattern")
        void allowsNonWildcardMatch() {
            IpAccessRule rule = buildRule(IpRuleType.BLACKLIST, IpRuleScope.GLOBAL, "192.168.*");
            when(ipAccessRuleRepository.findByScopeAndIsActiveTrue(IpRuleScope.GLOBAL))
                    .thenReturn(List.of(rule));

            assertThat(ipAccessService.isIpAllowed("10.0.0.1", null, null, null)).isTrue();
        }
    }

    // ── matchesPattern ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("matchesPattern()")
    class MatchesPattern {

        @Test
        @DisplayName("Exact match returns true")
        void exactMatch() {
            assertThat(ipAccessService.matchesPattern("192.168.1.1", "192.168.1.1")).isTrue();
        }

        @Test
        @DisplayName("Exact non-match returns false")
        void exactNonMatch() {
            assertThat(ipAccessService.matchesPattern("192.168.1.2", "192.168.1.1")).isFalse();
        }

        @Test
        @DisplayName("CIDR match returns true for IP inside range")
        void cidrMatchInRange() {
            assertThat(ipAccessService.matchesPattern("192.168.1.50", "192.168.1.0/24")).isTrue();
        }

        @Test
        @DisplayName("CIDR non-match returns false for IP outside range")
        void cidrNonMatch() {
            assertThat(ipAccessService.matchesPattern("10.0.0.1", "192.168.1.0/24")).isFalse();
        }

        @Test
        @DisplayName("Wildcard * matches any octet")
        void wildcardMatchAnyOctet() {
            assertThat(ipAccessService.matchesPattern("192.168.5.100", "192.168.5.*")).isTrue();
        }

        @Test
        @DisplayName("Null IP returns false")
        void nullIpReturnsFalse() {
            assertThat(ipAccessService.matchesPattern(null, "192.168.1.1")).isFalse();
        }

        @Test
        @DisplayName("Null pattern returns false")
        void nullPatternReturnsFalse() {
            assertThat(ipAccessService.matchesPattern("192.168.1.1", null)).isFalse();
        }
    }
}
