package com.propertize.payroll.repository;

import com.propertize.payroll.entity.TimesheetEntity;
import com.propertize.payroll.enums.TimesheetStatusEnum;
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

/**
 * Repository for Timesheet entity operations.
 */
@Repository
public interface TimesheetRepository extends JpaRepository<TimesheetEntity, UUID> {

    /**
     * Find all timesheets for a specific employee with pagination.
     */
    Page<TimesheetEntity> findByEmployeeId(String employeeId, Pageable pageable);

    /**
     * Find all timesheets for a specific employee.
     */
    List<TimesheetEntity> findByEmployeeId(String employeeId);

    /**
     * Find timesheet by employee ID and pay period ID.
     */
    Optional<TimesheetEntity> findByEmployeeIdAndPayPeriodId(String employeeId, UUID payPeriodId);

    /**
     * Find timesheets by status.
     */
    List<TimesheetEntity> findByStatus(TimesheetStatusEnum status);

    /**
     * Find timesheets by employee and status.
     */
    List<TimesheetEntity> findByEmployeeIdAndStatus(String employeeId, TimesheetStatusEnum status);

    /**
     * Find timesheet by employee ID and week containing a specific date.
     */
    @Query("SELECT t FROM TimesheetEntity t WHERE t.employeeId = :employeeId " +
            "AND :date BETWEEN t.weekPeriod.startDate AND t.weekPeriod.endDate")
    Optional<TimesheetEntity> findByEmployeeIdAndDateWithin(
            @Param("employeeId") String employeeId,
            @Param("date") LocalDate date);

    /**
     * Find pending approval timesheets for a list of employee IDs (for managers).
     */
    @Query("SELECT t FROM TimesheetEntity t WHERE t.employeeId IN :employeeIds AND t.status = :status")
    List<TimesheetEntity> findByEmployeeIdInAndStatus(
            @Param("employeeIds") List<String> employeeIds,
            @Param("status") TimesheetStatusEnum status);

    /**
     * Find timesheets by pay period.
     */
    List<TimesheetEntity> findByPayPeriodId(UUID payPeriodId);

    /**
     * Count timesheets by status for an employee.
     */
    Long countByEmployeeIdAndStatus(String employeeId, TimesheetStatusEnum status);

    /**
     * Find timesheets within a date range.
     */
    @Query("SELECT t FROM TimesheetEntity t WHERE t.employeeId = :employeeId " +
            "AND t.weekPeriod.startDate >= :startDate AND t.weekPeriod.endDate <= :endDate")
    List<TimesheetEntity> findByEmployeeIdAndDateRange(
            @Param("employeeId") String employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find all submitted timesheets pending approval for a client.
     */
    @Query("SELECT t FROM TimesheetEntity t " +
            "JOIN t.payPeriod pp " +
            "WHERE pp.client.id = :clientId AND t.status = 'SUBMITTED'")
    List<TimesheetEntity> findPendingApprovalsForClient(@Param("clientId") UUID clientId);
}
