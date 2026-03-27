package com.propertize.payroll.repository;

import com.propertize.payroll.entity.EquityGrantEntity;
import com.propertize.payroll.enums.EquityTypeEnum;
import com.propertize.payroll.enums.GrantStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EquityGrantRepository extends JpaRepository<EquityGrantEntity, UUID> {

    List<EquityGrantEntity> findByEmployeeId(UUID employeeId);

    Page<EquityGrantEntity> findByEmployeeId(UUID employeeId, Pageable pageable);

    List<EquityGrantEntity> findByStatus(GrantStatusEnum status);

    @Query("SELECT eg FROM EquityGrantEntity eg WHERE eg.employee.id = :employeeId AND eg.status = :status")
    List<EquityGrantEntity> findByEmployeeIdAndStatus(
            @Param("employeeId") UUID employeeId,
            @Param("status") GrantStatusEnum status);

    @Query("SELECT eg FROM EquityGrantEntity eg WHERE eg.employee.id = :employeeId AND eg.equityType = :equityType")
    List<EquityGrantEntity> findByEmployeeIdAndEquityType(
            @Param("employeeId") UUID employeeId,
            @Param("equityType") EquityTypeEnum equityType);

    @Query("SELECT eg FROM EquityGrantEntity eg WHERE eg.vestingStartDate <= :date AND eg.status = 'ACTIVE'")
    List<EquityGrantEntity> findActiveGrantsWithVestingBefore(@Param("date") LocalDate date);

    @Query("SELECT SUM(eg.totalShares) FROM EquityGrantEntity eg WHERE eg.employee.id = :employeeId AND eg.status = 'ACTIVE'")
    BigDecimal sumSharesGrantedByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("SELECT SUM(eg.vestedShares) FROM EquityGrantEntity eg WHERE eg.employee.id = :employeeId")
    BigDecimal sumSharesVestedByEmployeeId(@Param("employeeId") UUID employeeId);
}
