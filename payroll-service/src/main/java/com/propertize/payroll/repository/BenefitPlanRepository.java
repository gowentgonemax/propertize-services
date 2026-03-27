package com.propertize.payroll.repository;

import com.propertize.payroll.entity.BenefitPlan;
import com.propertize.payroll.enums.BenefitTypeEnum;
import com.propertize.payroll.enums.PlanStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BenefitPlanRepository extends JpaRepository<BenefitPlan, UUID> {

    List<BenefitPlan> findByClientId(UUID clientId);

    Page<BenefitPlan> findByClientId(UUID clientId, Pageable pageable);

    List<BenefitPlan> findByClientIdAndStatus(UUID clientId, PlanStatusEnum status);

    List<BenefitPlan> findByClientIdAndBenefitType(UUID clientId, BenefitTypeEnum benefitType);

    @Query("SELECT bp FROM BenefitPlan bp WHERE bp.client.id = :clientId AND bp.status = 'ACTIVE' " +
           "AND (bp.planStartDate IS NULL OR bp.planStartDate <= :date) " +
           "AND (bp.planEndDate IS NULL OR bp.planEndDate >= :date)")
    List<BenefitPlan> findActiveByClientIdAndDate(@Param("clientId") UUID clientId, @Param("date") LocalDate date);
}
