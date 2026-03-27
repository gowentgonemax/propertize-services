package com.propertize.payroll.repository;

import com.propertize.payroll.entity.PayrollRun;
import com.propertize.payroll.enums.PayrollStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    @EntityGraph(attributePaths = { "client", "approvedBy" })
    Page<PayrollRun> findByClientId(UUID clientId, Pageable pageable);

    @EntityGraph(attributePaths = { "client", "approvedBy" })
    @Query("SELECT pr FROM PayrollRun pr WHERE pr.client.id = :clientId " +
            "AND pr.payPeriod.startDate BETWEEN :startDateFrom AND :startDateTo " +
            "AND pr.payPeriod.endDate BETWEEN :endDateFrom AND :endDateTo")
    List<PayrollRun> findByClientIdAndPayPeriodStartBetweenAndPayPeriodEndBetween(
            @Param("clientId") UUID clientId,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo,
            @Param("endDateFrom") LocalDate endDateFrom,
            @Param("endDateTo") LocalDate endDateTo);

    @EntityGraph(attributePaths = { "client", "approvedBy" })
    List<PayrollRun> findByClientIdAndStatus(UUID clientId, PayrollStatusEnum status);

    @Override
    @EntityGraph(attributePaths = { "client", "approvedBy" })
    Optional<PayrollRun> findById(UUID id);
}
