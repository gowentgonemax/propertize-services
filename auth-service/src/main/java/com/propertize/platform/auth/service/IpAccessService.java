package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.IpAccessRuleRequest;
import com.propertize.platform.auth.entity.IpAccessRule;
import com.propertize.platform.auth.entity.IpRuleScope;
import com.propertize.platform.auth.entity.IpRuleType;
import com.propertize.platform.auth.repository.IpAccessRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Service for IP-based access control.
 *
 * Evaluates whether a given IP address is allowed or denied based on
 * configured rules at various scopes (global, organization, role, user).
 *
 * Rule evaluation order:
 * 1. GLOBAL blacklist → deny if matched
 * 2. GLOBAL whitelist → allow if matched
 * 3. USER-specific rules
 * 4. ROLE-specific rules (each role checked)
 * 5. ORGANIZATION-specific rules
 * 6. Default: allow
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IpAccessService {

    private final IpAccessRuleRepository ipAccessRuleRepository;

    /**
     * Check if an IP address is allowed for a given user context.
     *
     * @param ipAddress the client IP address
     * @param userId    the authenticated user's ID (nullable for unauthenticated)
     * @param roles     the user's roles (nullable)
     * @param orgId     the user's organization ID (nullable)
     * @return true if the IP is allowed, false if blocked
     */
    public boolean isIpAllowed(String ipAddress, Long userId, Set<String> roles, Long orgId) {
        if (ipAddress == null || ipAddress.isBlank()) {
            log.warn("IP address is null or blank — defaulting to allow");
            return true;
        }

        // Filter out expired rules from all checks
        LocalDateTime now = LocalDateTime.now();

        // 1. Check GLOBAL blacklist first — deny immediately if matched
        List<IpAccessRule> globalRules = getActiveRulesByScope(IpRuleScope.GLOBAL);
        for (IpAccessRule rule : globalRules) {
            if (isExpired(rule, now))
                continue;
            if (rule.getRuleType() == IpRuleType.BLACKLIST && matchesPattern(ipAddress, rule.getIpPattern())) {
                log.info("IP {} denied by GLOBAL blacklist rule: {} ({})",
                        ipAddress, rule.getIpPattern(), rule.getDescription());
                return false;
            }
        }

        // 2. Check GLOBAL whitelist — allow if matched
        for (IpAccessRule rule : globalRules) {
            if (isExpired(rule, now))
                continue;
            if (rule.getRuleType() == IpRuleType.WHITELIST && matchesPattern(ipAddress, rule.getIpPattern())) {
                log.debug("IP {} allowed by GLOBAL whitelist rule: {}", ipAddress, rule.getIpPattern());
                return true;
            }
        }

        // 3. Check USER-specific rules
        if (userId != null) {
            List<IpAccessRule> userRules = getActiveRulesByScopeAndValue(
                    IpRuleScope.USER, userId.toString());
            Boolean userResult = evaluateRules(ipAddress, userRules, now, "USER:" + userId);
            if (userResult != null)
                return userResult;
        }

        // 4. Check ROLE-specific rules (check each role)
        if (roles != null && !roles.isEmpty()) {
            for (String role : roles) {
                List<IpAccessRule> roleRules = getActiveRulesByScopeAndValue(
                        IpRuleScope.ROLE, role);
                Boolean roleResult = evaluateRules(ipAddress, roleRules, now, "ROLE:" + role);
                if (roleResult != null)
                    return roleResult;
            }
        }

        // 5. Check ORGANIZATION-specific rules
        if (orgId != null) {
            List<IpAccessRule> orgRules = getActiveRulesByScopeAndValue(
                    IpRuleScope.ORGANIZATION, orgId.toString());
            Boolean orgResult = evaluateRules(ipAddress, orgRules, now, "ORG:" + orgId);
            if (orgResult != null)
                return orgResult;
        }

        // 6. Default: allow
        log.debug("No matching IP rules for {} — defaulting to allow", ipAddress);
        return true;
    }

    /**
     * Evaluate a list of rules against an IP address.
     *
     * @return Boolean.TRUE if whitelisted, Boolean.FALSE if blacklisted, null if no
     *         match
     */
    private Boolean evaluateRules(String ipAddress, List<IpAccessRule> rules,
            LocalDateTime now, String context) {
        for (IpAccessRule rule : rules) {
            if (isExpired(rule, now))
                continue;
            if (matchesPattern(ipAddress, rule.getIpPattern())) {
                if (rule.getRuleType() == IpRuleType.BLACKLIST) {
                    log.info("IP {} denied by {} blacklist rule: {} ({})",
                            ipAddress, context, rule.getIpPattern(), rule.getDescription());
                    return Boolean.FALSE;
                } else {
                    log.debug("IP {} allowed by {} whitelist rule: {}", ipAddress, context, rule.getIpPattern());
                    return Boolean.TRUE;
                }
            }
        }
        return null;
    }

    /**
     * Check if a rule has expired.
     */
    private boolean isExpired(IpAccessRule rule, LocalDateTime now) {
        return rule.getExpiresAt() != null && rule.getExpiresAt().isBefore(now);
    }

    /**
     * Match an IP address against a pattern.
     * Supports:
     * - Exact match: "192.168.1.100"
     * - CIDR notation: "192.168.1.0/24"
     * - Wildcard: "192.168.*" or "10.*.*.*"
     *
     * @param ipAddress the IP address to check
     * @param pattern   the pattern to match against
     * @return true if the IP matches the pattern
     */
    public boolean matchesPattern(String ipAddress, String pattern) {
        if (ipAddress == null || pattern == null)
            return false;

        String trimmedIp = ipAddress.trim();
        String trimmedPattern = pattern.trim();

        // Exact match
        if (trimmedIp.equals(trimmedPattern)) {
            return true;
        }

        // CIDR notation (e.g., 192.168.1.0/24)
        if (trimmedPattern.contains("/")) {
            return matchesCidr(trimmedIp, trimmedPattern);
        }

        // Wildcard match (e.g., 192.168.*)
        if (trimmedPattern.contains("*")) {
            return matchesWildcard(trimmedIp, trimmedPattern);
        }

        return false;
    }

    /**
     * Match an IP address against a CIDR range.
     * Uses Java InetAddress for proper network address calculation.
     */
    private boolean matchesCidr(String ipAddress, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2)
                return false;

            InetAddress networkAddress = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            byte[] networkBytes = networkAddress.getAddress();
            byte[] ipBytes = InetAddress.getByName(ipAddress).getAddress();

            // Must be same address family (IPv4 vs IPv6)
            if (networkBytes.length != ipBytes.length)
                return false;

            int totalBits = networkBytes.length * 8;
            if (prefixLength < 0 || prefixLength > totalBits)
                return false;

            // Compare the prefix bits
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            // Compare full bytes
            for (int i = 0; i < fullBytes; i++) {
                if (networkBytes[i] != ipBytes[i])
                    return false;
            }

            // Compare remaining bits in the partial byte
            if (remainingBits > 0 && fullBytes < networkBytes.length) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                if ((networkBytes[fullBytes] & mask) != (ipBytes[fullBytes] & mask)) {
                    return false;
                }
            }

            return true;

        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("Invalid CIDR pattern or IP address: cidr={}, ip={}, error={}", cidr, ipAddress, e.getMessage());
            return false;
        }
    }

    /**
     * Match an IP address against a wildcard pattern.
     * Each '*' matches any single octet in IPv4.
     */
    private boolean matchesWildcard(String ipAddress, String pattern) {
        String[] ipOctets = ipAddress.split("\\.");
        String[] patternOctets = pattern.split("\\.");

        if (ipOctets.length != patternOctets.length)
            return false;

        for (int i = 0; i < ipOctets.length; i++) {
            if (!"*".equals(patternOctets[i]) && !ipOctets[i].equals(patternOctets[i])) {
                return false;
            }
        }
        return true;
    }

    // ========================================================================
    // CRUD Operations
    // ========================================================================

    /**
     * Create a new IP access rule.
     */
    @Transactional
    @CacheEvict(value = "ipRules", allEntries = true)
    public IpAccessRule createRule(IpAccessRuleRequest request, Long createdBy) {
        log.info("Creating IP access rule: type={}, scope={}, pattern={}, createdBy={}",
                request.getRuleType(), request.getScope(), request.getIpPattern(), createdBy);

        // Validate scope + scopeValue consistency
        if (request.getScope() != IpRuleScope.GLOBAL
                && (request.getScopeValue() == null || request.getScopeValue().isBlank())) {
            throw new IllegalArgumentException(
                    "scopeValue is required for scope: " + request.getScope());
        }

        IpAccessRule rule = IpAccessRule.builder()
                .ruleType(request.getRuleType())
                .ipPattern(request.getIpPattern())
                .description(request.getDescription())
                .scope(request.getScope())
                .scopeValue(request.getScopeValue())
                .isActive(true)
                .createdBy(createdBy)
                .expiresAt(request.getExpiresAt())
                .build();

        IpAccessRule saved = ipAccessRuleRepository.save(rule);
        log.info("IP access rule created: id={}", saved.getId());
        return saved;
    }

    /**
     * Delete (deactivate) an IP access rule.
     */
    @Transactional
    @CacheEvict(value = "ipRules", allEntries = true)
    public void deleteRule(Long ruleId) {
        IpAccessRule rule = ipAccessRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("IP access rule not found: " + ruleId));

        rule.setActive(false);
        ipAccessRuleRepository.save(rule);
        log.info("IP access rule deactivated: id={}", ruleId);
    }

    /**
     * Get rules by scope and optional scope value.
     */
    @Transactional(readOnly = true)
    public List<IpAccessRule> getRules(IpRuleScope scope, String scopeValue) {
        if (scopeValue != null && !scopeValue.isBlank()) {
            return ipAccessRuleRepository.findByScopeAndScopeValueAndIsActiveTrue(scope, scopeValue);
        }
        return ipAccessRuleRepository.findByScopeAndIsActiveTrue(scope);
    }

    // ========================================================================
    // Cached lookups
    // ========================================================================

    @Cacheable(value = "ipRules", key = "'scope:' + #scope.name()")
    public List<IpAccessRule> getActiveRulesByScope(IpRuleScope scope) {
        return ipAccessRuleRepository.findByScopeAndIsActiveTrue(scope);
    }

    @Cacheable(value = "ipRules", key = "'scope:' + #scope.name() + ':' + #scopeValue")
    public List<IpAccessRule> getActiveRulesByScopeAndValue(IpRuleScope scope, String scopeValue) {
        return ipAccessRuleRepository.findByScopeAndScopeValueAndIsActiveTrue(scope, scopeValue);
    }
}
