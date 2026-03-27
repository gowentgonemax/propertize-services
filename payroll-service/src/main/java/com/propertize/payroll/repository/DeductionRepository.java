package com.propertize.payroll.repository;

import com.propertize.payroll.entity.Deduction;
import com.propertize.payroll.enums.DeductionStatusEnum;
import com.propertize.payroll.enums.DeductionTypeDetailEnum;
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
public interface DeductionRepository extends JpaRepository<Deduction, UUID> {

    List<Deduction> findByEmployeeId(UUID employeeId);

    Page<Deduction> findByEmployeeId(UUID employeeId, Pageable pageable);

    List<Deduction> findByEmployeeIdAndStatus(UUID employeeId, DeductionStatusEnum status);

    // String-based employeeId for PaystubService
    @Query("SELECT d FROM Deduction d WHERE CAST(d.employee.id AS string) = :employeeId AND d.status = :status")
    List<Deduction> findByEmployeeIdAndStatus(@Param("employeeId") String employeeId, @Param("status") DeductionStatusEnum status);

    List<Deduction> findByEmployeeIdAndDeductionType(UUID employeeId, DeductionTypeDetailEnum deductionType);

    @Query("SELECT d FROM Deduction d WHERE d.employee.id = :employeeId AND d.status = 'ACTIVE' " +
           "AND (d.startDate IS NULL OR d.startDate <= :date) " +
           "AND (d.endDate IS NULL OR d.endDate >= :date) " +
           "ORDER BY d.priority ASC")
    List<Deduction> findActiveByEmployeeIdAndDate(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date);

    @Query("SELECT d FROM Deduction d WHERE d.employee.id = :employeeId AND d.isPreTax = true AND d.status = 'ACTIVE'")
    List<Deduction> findPreTaxDeductionsByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("SELECT d FROM Deduction d WHERE d.employee.id = :employeeId AND d.isPreTax = false AND d.status = 'ACTIVE'")
    List<Deduction> findPostTaxDeductionsByEmployeeId(@Param("employeeId") UUID employeeId);
}
