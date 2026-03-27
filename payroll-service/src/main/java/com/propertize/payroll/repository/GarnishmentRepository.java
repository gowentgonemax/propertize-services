package com.propertize.payroll.repository;

import com.propertize.payroll.entity.GarnishmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface GarnishmentRepository extends JpaRepository<GarnishmentEntity, UUID> {

    List<GarnishmentEntity> findByEmployeeId(UUID employeeId);

    @Query("SELECT g FROM GarnishmentEntity g WHERE g.employee.id = :employeeId AND g.status = 'ACTIVE'")
    List<GarnishmentEntity> findActiveByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("SELECT g FROM GarnishmentEntity g WHERE g.employee.id = :employeeId AND g.effectiveDate <= :date AND (g.endDate IS NULL OR g.endDate >= :date)")
    List<GarnishmentEntity> findActiveGarnishmentsAsOf(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date);

    @Query("SELECT g FROM GarnishmentEntity g WHERE g.status = 'ACTIVE' ORDER BY g.priorityOrder ASC")
    List<GarnishmentEntity> findAllActiveOrderByPriority();
}
