package com.propertize.payroll.repository;

import com.propertize.payroll.entity.CommissionStructureEntity;
import com.propertize.payroll.enums.CommissionStatusEnum;
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
public interface CommissionRepository extends JpaRepository<CommissionStructureEntity, UUID> {

    List<CommissionStructureEntity> findByEmployeeId(UUID employeeId);

    Page<CommissionStructureEntity> findByEmployeeId(UUID employeeId, Pageable pageable);

    List<CommissionStructureEntity> findByStatus(CommissionStatusEnum status);

    @Query("SELECT c FROM CommissionStructureEntity c WHERE c.employee.id = :employeeId AND c.status = :status")
    List<CommissionStructureEntity> findByEmployeeIdAndStatus(
            @Param("employeeId") UUID employeeId,
            @Param("status") CommissionStatusEnum status);

    @Query("SELECT c FROM CommissionStructureEntity c WHERE c.effectiveDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date)")
    List<CommissionStructureEntity> findActiveCommissionsAsOf(@Param("date") LocalDate date);
}
