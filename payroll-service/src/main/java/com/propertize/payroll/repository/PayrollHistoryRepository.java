package com.propertize.payroll.repository;

import com.propertize.payroll.entity.PayrollHistoryEntity;
import com.propertize.payroll.enums.PayrollActionEnum;
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
public interface PayrollHistoryRepository extends JpaRepository<PayrollHistoryEntity, UUID> {

    List<PayrollHistoryEntity> findByPayrollRunIdOrderByPerformedAtDesc(UUID payrollRunId);

    Page<PayrollHistoryEntity> findByPayrollRunId(UUID payrollRunId, Pageable pageable);

    List<PayrollHistoryEntity> findByAction(PayrollActionEnum action);

    @Query("SELECT ph FROM PayrollHistoryEntity ph WHERE ph.payrollRun.id = :payrollRunId AND ph.action = :action")
    List<PayrollHistoryEntity> findByPayrollRunIdAndAction(
            @Param("payrollRunId") UUID payrollRunId,
            @Param("action") PayrollActionEnum action);

    @Query("SELECT ph FROM PayrollHistoryEntity ph WHERE ph.performedBy = :userId ORDER BY ph.performedAt DESC")
    Page<PayrollHistoryEntity> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT ph FROM PayrollHistoryEntity ph WHERE ph.performedAt BETWEEN :startDate AND :endDate ORDER BY ph.performedAt DESC")
    List<PayrollHistoryEntity> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
