package com.propertize.payroll.repository;

import com.propertize.payroll.entity.LeaveBalanceEntity;
import com.propertize.payroll.enums.LeaveTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalanceEntity, UUID> {

    Optional<LeaveBalanceEntity> findByEmployeeIdAndLeaveTypeAndYear(
            UUID employeeId, LeaveTypeEnum leaveType, Integer year);

    List<LeaveBalanceEntity> findByEmployeeIdAndYear(UUID employeeId, Integer year);

    List<LeaveBalanceEntity> findByEmployeeId(UUID employeeId);

    @Query("SELECT lb FROM LeaveBalanceEntity lb WHERE lb.employee.id = :employeeId AND lb.year = :year ORDER BY lb.leaveType")
    List<LeaveBalanceEntity> findAllBalancesForEmployee(
            @Param("employeeId") UUID employeeId,
            @Param("year") Integer year);

    @Query("SELECT lb FROM LeaveBalanceEntity lb WHERE lb.year = :year " +
            "AND (lb.beginningBalance + lb.accruedHours + lb.carriedOverHours + lb.adjustedHours - lb.usedHours) < 0")
    List<LeaveBalanceEntity> findNegativeBalances(@Param("year") Integer year);
}
