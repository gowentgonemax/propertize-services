package com.propertize.payroll.repository;

import com.propertize.payroll.entity.PayrollAdjustment;
import com.propertize.payroll.enums.AdjustmentStatusEnum;
import com.propertize.payroll.enums.AdjustmentTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for PayrollAdjustment entity operations.
 */
@Repository
public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, UUID> {

    /**
     * Find adjustments by employee ID.
     */
    List<PayrollAdjustment> findByEmployeeId(String employeeId);

    /**
     * Find adjustments by payroll run ID.
     */
    List<PayrollAdjustment> findByPayrollRunId(UUID payrollRunId);

    /**
     * Find adjustments by status.
     */
    List<PayrollAdjustment> findByStatus(AdjustmentStatusEnum status);

    /**
     * Find adjustments by type.
     */
    List<PayrollAdjustment> findByAdjustmentType(AdjustmentTypeEnum adjustmentType);

    /**
     * Find pending adjustments for an employee.
     */
    List<PayrollAdjustment> findByEmployeeIdAndStatus(String employeeId, AdjustmentStatusEnum status);

    /**
     * Find adjustments within a date range.
     */
    List<PayrollAdjustment> findByEffectiveDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find unprocessed adjustments for a payroll run.
     */
    @Query("SELECT pa FROM PayrollAdjustment pa WHERE pa.payrollRun.id = :payrollRunId AND pa.status = :status")
    List<PayrollAdjustment> findByPayrollRunIdAndStatus(
        @Param("payrollRunId") UUID payrollRunId,
        @Param("status") AdjustmentStatusEnum status
    );

    /**
     * Find adjustments by employee with pagination.
     */
    Page<PayrollAdjustment> findByEmployeeId(String employeeId, Pageable pageable);
}
