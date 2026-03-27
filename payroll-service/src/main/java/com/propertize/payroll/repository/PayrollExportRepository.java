package com.propertize.payroll.repository;

import com.propertize.payroll.entity.PayrollExportEntity;
import com.propertize.payroll.enums.ExportStatusEnum;
import com.propertize.payroll.enums.ExportTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollExportRepository extends JpaRepository<PayrollExportEntity, UUID> {

    List<PayrollExportEntity> findByPayrollRunId(UUID payrollRunId);

    Page<PayrollExportEntity> findByPayrollRunId(UUID payrollRunId, Pageable pageable);

    List<PayrollExportEntity> findByStatus(ExportStatusEnum status);

    @Query("SELECT pe FROM PayrollExportEntity pe WHERE pe.payrollRun.client.id = :clientId ORDER BY pe.exportedAt DESC")
    Page<PayrollExportEntity> findByClientId(@Param("clientId") UUID clientId, Pageable pageable);

    @Query("SELECT pe FROM PayrollExportEntity pe WHERE pe.payrollRun.id = :payrollRunId AND pe.exportType = :exportType ORDER BY pe.exportedAt DESC")
    List<PayrollExportEntity> findByPayrollRunIdAndExportType(
        @Param("payrollRunId") UUID payrollRunId,
        @Param("exportType") ExportTypeEnum exportType);

    @Query("SELECT pe FROM PayrollExportEntity pe WHERE pe.exportedAt BETWEEN :startDate AND :endDate ORDER BY pe.exportedAt DESC")
    List<PayrollExportEntity> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
}
