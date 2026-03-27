package com.propertize.payroll.repository;

import com.propertize.payroll.entity.LeaveRequest;
import com.propertize.payroll.enums.LeaveStatusEnum;
import com.propertize.payroll.enums.LeaveTypeEnum;
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
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    List<LeaveRequest> findByEmployeeId(UUID employeeId);

    Page<LeaveRequest> findByEmployeeId(UUID employeeId, Pageable pageable);

    List<LeaveRequest> findByStatus(LeaveStatusEnum status);

    List<LeaveRequest> findByEmployeeIdAndStatus(UUID employeeId, LeaveStatusEnum status);

    List<LeaveRequest> findByEmployeeIdAndLeaveType(UUID employeeId, LeaveTypeEnum leaveType);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId AND " +
            "((lr.startDate BETWEEN :startDate AND :endDate) OR (lr.endDate BETWEEN :startDate AND :endDate) " +
            "OR (lr.startDate <= :startDate AND lr.endDate >= :endDate))")
    List<LeaveRequest> findOverlappingRequests(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.client.id = :clientId AND lr.status = :status")
    Page<LeaveRequest> findByClientIdAndStatus(@Param("clientId") UUID clientId,
            @Param("status") LeaveStatusEnum status, Pageable pageable);

    @Query("SELECT SUM(lr.hoursRequested) FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
            "AND lr.leaveType = :leaveType AND lr.status = 'APPROVED' AND YEAR(lr.startDate) = :year")
    java.math.BigDecimal sumApprovedHoursByTypeAndYear(
            @Param("employeeId") UUID employeeId,
            @Param("leaveType") LeaveTypeEnum leaveType,
            @Param("year") Integer year);
}
