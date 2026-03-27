package com.propertize.payroll.repository;

import com.propertize.payroll.entity.TaxWithholdingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxWithholdingRepository extends JpaRepository<TaxWithholdingEntity, UUID> {

    List<TaxWithholdingEntity> findByEmployeeId(String employeeId);

    // String-based employeeId methods for TaxService
    @Query("SELECT t FROM TaxWithholdingEntity t WHERE t.employeeId = :employeeId AND t.isActive = true")
    Optional<TaxWithholdingEntity> findByEmployeeIdAndIsActiveTrue(@Param("employeeId") String employeeId);

    @Query("SELECT t FROM TaxWithholdingEntity t WHERE t.employeeId = :employeeId ORDER BY t.effectiveDate DESC")
    List<TaxWithholdingEntity> findByEmployeeIdOrderByEffectiveDateDesc(@Param("employeeId") String employeeId);

    Optional<TaxWithholdingEntity> findByEmployeeIdAndTaxYearAndIsCurrent(String employeeId, Integer taxYear,
            Boolean isCurrent);

    @Query("SELECT t FROM TaxWithholdingEntity t WHERE t.employee.id = :employeeId AND t.isCurrent = true ORDER BY t.taxYear DESC")
    Optional<TaxWithholdingEntity> findCurrentByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("SELECT t FROM TaxWithholdingEntity t WHERE t.employee.id = :employeeId AND t.taxYear = :taxYear")
    List<TaxWithholdingEntity> findByEmployeeIdAndTaxYear(@Param("employeeId") UUID employeeId,
            @Param("taxYear") Integer taxYear);

    @Query("SELECT t FROM TaxWithholdingEntity t WHERE t.employee.client.id = :clientId AND t.taxYear = :taxYear")
    List<TaxWithholdingEntity> findByClientIdAndTaxYear(@Param("clientId") UUID clientId,
            @Param("taxYear") Integer taxYear);
}
