package com.propertize.payroll.repository;

import com.propertize.payroll.entity.Paystub;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
public interface PaystubRepository extends JpaRepository<Paystub, UUID> {

        @EntityGraph(attributePaths = { "employee", "payrollRun" })
        List<Paystub> findByPayrollRunId(UUID payrollRunId);

        @EntityGraph(attributePaths = { "payrollRun" })
        @Query("SELECT p FROM Paystub p WHERE p.employeeId = :employeeId ORDER BY p.payrollRun.payDate DESC")
        Page<Paystub> findByEmployeeIdOrderByPayDateDesc(@Param("employeeId") String employeeId, Pageable pageable);

        @EntityGraph(attributePaths = { "payrollRun" })
        @Query("SELECT p FROM Paystub p WHERE p.employeeId = :employeeId AND p.payrollRun.payDate BETWEEN :startDate AND :endDate")
        List<Paystub> findByEmployeeIdAndDateRange(
                        @Param("employeeId") String employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(p.grossPay) FROM Paystub p WHERE p.employeeId = :employeeId AND YEAR(p.payrollRun.payDate) = :year")
        BigDecimal sumGrossPayByEmployeeAndYear(@Param("employeeId") String employeeId, @Param("year") Integer year);

        @Query("SELECT SUM(p.netPay) FROM Paystub p WHERE p.employeeId = :employeeId AND YEAR(p.payrollRun.payDate) = :year")
        BigDecimal sumNetPayByEmployeeAndYear(@Param("employeeId") String employeeId, @Param("year") Integer year);

        @Query("SELECT p FROM Paystub p WHERE p.payrollRun.id = :payrollRunId AND p.employeeId = :employeeId")
        Paystub findByPayrollRunIdAndEmployeeId(@Param("payrollRunId") UUID payrollRunId,
                        @Param("employeeId") String employeeId);

        // Additional methods for PaystubService
        Optional<Paystub> findByEmployeeIdAndPayrollRunId(String employeeId, UUID payrollRunId);

        @EntityGraph(attributePaths = { "payrollRun" })
        @Query("SELECT p FROM Paystub p WHERE p.employeeId = :employeeId AND p.payDate BETWEEN :startDate AND :endDate")
        List<Paystub> findByEmployeeIdAndPayDateBetween(
                        @Param("employeeId") String employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Like findByEmployeeIdAndPayDateBetween but excludes one paystub by ID.
         * Used during YTD recalculation on regenerate to avoid double-counting the
         * paystub that is currently being updated (still in DB with old values).
         */
        @EntityGraph(attributePaths = { "payrollRun" })
        @Query("SELECT p FROM Paystub p WHERE p.employeeId = :employeeId AND p.payDate BETWEEN :startDate AND :endDate AND p.id <> :excludeId")
        List<Paystub> findByEmployeeIdAndPayDateBetweenExcluding(
                        @Param("employeeId") String employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("excludeId") UUID excludeId);
}
