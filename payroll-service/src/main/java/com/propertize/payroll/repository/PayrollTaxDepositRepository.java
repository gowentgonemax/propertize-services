package com.propertize.payroll.repository;

import com.propertize.payroll.entity.PayrollTaxDepositEntity;
import com.propertize.payroll.enums.TaxDepositStatusEnum;
import com.propertize.payroll.enums.TaxTypeEnum;
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
public interface PayrollTaxDepositRepository extends JpaRepository<PayrollTaxDepositEntity, UUID> {

    List<PayrollTaxDepositEntity> findByClientId(UUID clientId);

    Page<PayrollTaxDepositEntity> findByClientId(UUID clientId, Pageable pageable);

    List<PayrollTaxDepositEntity> findByStatus(TaxDepositStatusEnum status);

    List<PayrollTaxDepositEntity> findByDueDateBetween(LocalDate start, LocalDate end);

    List<PayrollTaxDepositEntity> findByPayrollRunId(UUID payrollRunId);

    @Query("SELECT ptd FROM PayrollTaxDepositEntity ptd WHERE ptd.client.id = :clientId AND ptd.status = :status ORDER BY ptd.dueDate ASC")
    List<PayrollTaxDepositEntity> findByClientIdAndStatus(
        @Param("clientId") UUID clientId,
        @Param("status") TaxDepositStatusEnum status);

    @Query("SELECT ptd FROM PayrollTaxDepositEntity ptd WHERE ptd.client.id = :clientId AND ptd.taxYear = :year AND ptd.quarter = :quarter")
    List<PayrollTaxDepositEntity> findByClientIdAndQuarter(
        @Param("clientId") UUID clientId,
        @Param("year") Integer year,
        @Param("quarter") Integer quarter);

    @Query("SELECT ptd FROM PayrollTaxDepositEntity ptd WHERE ptd.dueDate < :date AND ptd.status IN ('PENDING', 'SCHEDULED')")
    List<PayrollTaxDepositEntity> findOverdueDeposits(@Param("date") LocalDate date);

    @Query("SELECT ptd FROM PayrollTaxDepositEntity ptd WHERE ptd.client.id = :clientId AND ptd.taxType = :taxType AND ptd.taxYear = :year")
    List<PayrollTaxDepositEntity> findByClientIdAndTaxTypeAndYear(
        @Param("clientId") UUID clientId,
        @Param("taxType") TaxTypeEnum taxType,
        @Param("year") Integer year);

    // Methods for TaxService
    @Query("SELECT ptd FROM PayrollTaxDepositEntity ptd WHERE ptd.client.id = :clientId AND ptd.dueDate BETWEEN :fromDate AND :toDate")
    List<PayrollTaxDepositEntity> findByClientIdAndDueDateBetween(
        @Param("clientId") UUID clientId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    @Query("SELECT ptd FROM PayrollTaxDepositEntity ptd WHERE ptd.client.id = :clientId AND ptd.dueDate < :date AND ptd.paidDate IS NULL")
    List<PayrollTaxDepositEntity> findByClientIdAndDueDateBeforeAndPaidDateIsNull(
        @Param("clientId") UUID clientId,
        @Param("date") LocalDate date);
}
