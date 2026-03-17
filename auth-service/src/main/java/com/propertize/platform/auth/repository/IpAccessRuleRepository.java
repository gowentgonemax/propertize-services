package com.propertize.platform.auth.repository;

import com.propertize.platform.auth.entity.IpAccessRule;
import com.propertize.platform.auth.entity.IpRuleScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for IP Access Rules.
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
@Repository
public interface IpAccessRuleRepository extends JpaRepository<IpAccessRule, Long> {

    /**
     * Find all active rules for a given scope.
     */
    List<IpAccessRule> findByScopeAndIsActiveTrue(IpRuleScope scope);

    /**
     * Find all active rules for a given scope and scope value.
     */
    List<IpAccessRule> findByScopeAndScopeValueAndIsActiveTrue(IpRuleScope scope, String scopeValue);

    /**
     * Find all active rules regardless of scope.
     */
    List<IpAccessRule> findByIsActiveTrue();
}
