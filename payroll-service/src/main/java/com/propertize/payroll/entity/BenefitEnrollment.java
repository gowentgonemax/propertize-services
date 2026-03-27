package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.CoverageLevelEnum;
import com.propertize.payroll.enums.EnrollmentStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing an employee's enrollment in a benefit plan.
 */
@Entity
@Table(name = "benefit_enrollments", indexes = {
    @Index(name = "idx_benefit_enrollment_employee", columnList = "employee_id"),
    @Index(name = "idx_benefit_enrollment_plan", columnList = "benefit_plan_id"),
    @Index(name = "idx_benefit_enrollment_status", columnList = "status")
})
@Getter
@Setter
public class BenefitEnrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_plan_id", nullable = false)
    private BenefitPlan benefitPlan;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_level", length = 30)
    private CoverageLevelEnum coverageLevel;

    @Column(name = "employee_contribution", precision = 15, scale = 2)
    private BigDecimal employeeContribution;

    @Column(name = "employer_contribution", precision = 15, scale = 2)
    private BigDecimal employerContribution;

    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EnrollmentStatusEnum status = EnrollmentStatusEnum.ACTIVE;

    /**
     * Checks if the enrollment is currently active.
     */
    public boolean isActive() {
        if (status != EnrollmentStatusEnum.ACTIVE) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return (effectiveDate == null || !today.isBefore(effectiveDate)) &&
               (terminationDate == null || !today.isAfter(terminationDate));
    }

    /**
     * Calculates the total contribution per pay period.
     */
    public BigDecimal getTotalContribution() {
        BigDecimal emp = employeeContribution != null ? employeeContribution : BigDecimal.ZERO;
        BigDecimal er = employerContribution != null ? employerContribution : BigDecimal.ZERO;
        return emp.add(er);
    }
}
