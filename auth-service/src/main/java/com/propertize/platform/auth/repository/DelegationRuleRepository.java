package com.propertize.platform.auth.repository;

import com.propertize.platform.auth.entity.DelegationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link DelegationRule} entities.
 *
 * @version 1.0 - Phase 3: Permission Delegation
 */
@Repository
public interface DelegationRuleRepository extends JpaRepository<DelegationRule, Long> {

    /**
     * Find the active delegation rule for a specific delegator role.
     */
    Optional<DelegationRule> findByDelegatorRoleAndIsActiveTrue(String delegatorRole);

    /**
     * Find all active delegation rules.
     */
    List<DelegationRule> findByIsActiveTrue();
}
