package com.propertize.payroll.repository;

import com.propertize.payroll.entity.CompensationEntity;
import com.propertize.payroll.enums.CompensationStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompensationRepository extends JpaRepository<CompensationEntity, UUID> {

    List<CompensationEntity> findByEmployeeId(UUID employeeId);

    Optional<CompensationEntity> findByEmployeeIdAndIsCurrent(UUID employeeId, Boolean isCurrent);

    @Query("SELECT c FROM CompensationEntity c WHERE c.employee.id = :employeeId AND c.isCurrent = true")
    Optional<CompensationEntity> findCurrentByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("SELECT c FROM CompensationEntity c WHERE c.employee.id = :employeeId ORDER BY c.effectiveDate DESC")
    List<CompensationEntity> findByEmployeeIdOrderByEffectiveDateDesc(@Param("employeeId") UUID employeeId);

    @Query("SELECT c FROM CompensationEntity c WHERE c.effectiveDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date) AND c.employee.id = :employeeId")
    Optional<CompensationEntity> findActiveOnDate(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date);

    List<CompensationEntity> findByStatus(CompensationStatusEnum status);

    Page<CompensationEntity> findByEmployeeClientId(UUID clientId, Pageable pageable);
}
