package com.propertize.payroll.repository;

import com.propertize.payroll.entity.OvertimeRuleEntity;
import com.propertize.payroll.enums.RuleStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OvertimeRuleRepository extends JpaRepository<OvertimeRuleEntity, UUID> {

    List<OvertimeRuleEntity> findByClientId(UUID clientId);

    List<OvertimeRuleEntity> findByClientIdAndStatus(UUID clientId, RuleStatusEnum status);

    @Query("SELECT o FROM OvertimeRuleEntity o WHERE o.client.id = :clientId AND o.status = 'ACTIVE'")
    Optional<OvertimeRuleEntity> findActiveRuleForClient(
            @Param("clientId") UUID clientId,
            @Param("date") LocalDate date);

    @Query("SELECT o FROM OvertimeRuleEntity o WHERE o.status = 'ACTIVE'")
    List<OvertimeRuleEntity> findAllActiveRules();
}
