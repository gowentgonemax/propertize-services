package com.propertize.payroll.repository;

import com.propertize.payroll.entity.BenefitEnrollment;
import com.propertize.payroll.enums.EnrollmentStatusEnum;
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
public interface BenefitEnrollmentRepository extends JpaRepository<BenefitEnrollment, UUID> {

    List<BenefitEnrollment> findByEmployeeId(UUID employeeId);

    // Overload for String employeeId
    @Query("SELECT be FROM BenefitEnrollment be WHERE CAST(be.employee.id AS string) = :employeeId")
    List<BenefitEnrollment> findByEmployeeId(@Param("employeeId") String employeeId);

    Page<BenefitEnrollment> findByEmployeeId(UUID employeeId, Pageable pageable);

    List<BenefitEnrollment> findByEmployeeIdAndStatus(UUID employeeId, EnrollmentStatusEnum status);

    // Overload for String employeeId
    @Query("SELECT be FROM BenefitEnrollment be WHERE CAST(be.employee.id AS string) = :employeeId AND be.status = :status")
    List<BenefitEnrollment> findByEmployeeIdAndStatus(@Param("employeeId") String employeeId, @Param("status") EnrollmentStatusEnum status);

    List<BenefitEnrollment> findByBenefitPlanId(UUID benefitPlanId);

    // Find by employee and plan
    @Query("SELECT be FROM BenefitEnrollment be WHERE CAST(be.employee.id AS string) = :employeeId AND be.benefitPlan.id = :planId")
    List<BenefitEnrollment> findByEmployeeIdAndBenefitPlanId(@Param("employeeId") String employeeId, @Param("planId") UUID planId);

    @Query("SELECT be FROM BenefitEnrollment be WHERE be.employee.id = :employeeId AND be.status = 'ACTIVE' " +
           "AND (be.effectiveDate IS NULL OR be.effectiveDate <= :date) " +
           "AND (be.terminationDate IS NULL OR be.terminationDate >= :date)")
    List<BenefitEnrollment> findActiveByEmployeeIdAndDate(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(be) FROM BenefitEnrollment be WHERE be.benefitPlan.id = :planId AND be.status = 'ACTIVE'")
    long countActiveByPlanId(@Param("planId") UUID planId);

    // Count active enrollments by plan and status
    long countByBenefitPlanIdAndStatus(UUID benefitPlanId, EnrollmentStatusEnum status);
}
