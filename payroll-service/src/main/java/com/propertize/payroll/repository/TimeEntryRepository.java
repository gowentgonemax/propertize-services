package com.propertize.payroll.repository;

import com.propertize.payroll.entity.TimeEntry;
import com.propertize.payroll.enums.TimeEntryStatusEnum;
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
public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {

    List<TimeEntry> findByEmployeeId(UUID employeeId);

    Page<TimeEntry> findByEmployeeId(UUID employeeId, Pageable pageable);

    @Query("SELECT t FROM TimeEntry t WHERE t.employee.id = :employeeId AND t.workDate BETWEEN :startDate AND :endDate")
    List<TimeEntry> findByEmployeeIdAndDateRange(
        @Param("employeeId") UUID employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM TimeEntry t WHERE t.employee.client.id = :clientId AND t.workDate BETWEEN :startDate AND :endDate")
    List<TimeEntry> findByClientIdAndDateRange(
        @Param("clientId") UUID clientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    List<TimeEntry> findByEmployeeIdAndStatus(UUID employeeId, TimeEntryStatusEnum status);

    @Query("SELECT t FROM TimeEntry t WHERE t.employee.client.id = :clientId AND t.status = :status")
    Page<TimeEntry> findByClientIdAndStatus(@Param("clientId") UUID clientId, @Param("status") TimeEntryStatusEnum status, Pageable pageable);

    @Query("SELECT SUM(t.regularHours) FROM TimeEntry t WHERE t.employee.id = :employeeId AND t.workDate BETWEEN :startDate AND :endDate AND t.status = 'APPROVED'")
    java.math.BigDecimal sumApprovedRegularHours(
        @Param("employeeId") UUID employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.overtimeHours) FROM TimeEntry t WHERE t.employee.id = :employeeId AND t.workDate BETWEEN :startDate AND :endDate AND t.status = 'APPROVED'")
    java.math.BigDecimal sumApprovedOvertimeHours(
        @Param("employeeId") UUID employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
