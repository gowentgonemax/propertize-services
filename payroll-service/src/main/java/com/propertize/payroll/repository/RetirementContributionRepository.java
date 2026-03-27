package com.propertize.payroll.repository;

import com.propertize.payroll.entity.RetirementContributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetirementContributionRepository extends JpaRepository<RetirementContributionEntity, UUID> {

    List<RetirementContributionEntity> findByEmployeeId(UUID employeeId);

    @Query("SELECT rc FROM RetirementContributionEntity rc WHERE rc.employee.id = :employeeId AND rc.isActive = true")
    Optional<RetirementContributionEntity> findActiveByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("SELECT rc FROM RetirementContributionEntity rc WHERE rc.isActive = true AND rc.contributionDate <= :date")
    List<RetirementContributionEntity> findActiveContributionsAsOf(@Param("date") LocalDate date);

    @Query("SELECT SUM(rc.ytdEmployeeContributions) FROM RetirementContributionEntity rc WHERE rc.employee.id = :employeeId AND YEAR(rc.updatedAt) = :year")
    BigDecimal sumYtdContributionsByEmployeeId(
            @Param("employeeId") UUID employeeId,
            @Param("year") Integer year);
}
