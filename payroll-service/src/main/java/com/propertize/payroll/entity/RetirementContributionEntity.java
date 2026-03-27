package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing retirement plan contributions (401k, etc.).
 */
@Entity
@Table(name = "retirement_contributions", indexes = {
        @Index(name = "idx_retire_contrib_employee", columnList = "employee_id"),
        @Index(name = "idx_retire_contrib_date", columnList = "contributionDate"),
        @Index(name = "idx_retire_contrib_paystub", columnList = "paystub_id")
})
@Getter
@Setter
public class RetirementContributionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    /**
     * Whether this contribution record is active
     */
    @Column(nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paystub_id")
    private Paystub paystub;

    @Column(nullable = false)
    private LocalDate contributionDate;

    /**
     * Plan type (e.g., "401K", "403B", "SIMPLE_IRA", "ROTH_401K")
     */
    @Column(nullable = false, length = 30)
    private String planType;

    /**
     * Employee pre-tax contribution
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal employeePreTaxContribution = BigDecimal.ZERO;

    /**
     * Employee Roth (post-tax) contribution
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal employeeRothContribution = BigDecimal.ZERO;

    /**
     * Employee after-tax contribution (non-Roth)
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal employeeAfterTaxContribution = BigDecimal.ZERO;

    /**
     * Employer match contribution
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal employerMatchContribution = BigDecimal.ZERO;

    /**
     * Employer non-elective contribution
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal employerNonElectiveContribution = BigDecimal.ZERO;

    /**
     * YTD employee contributions
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal ytdEmployeeContributions = BigDecimal.ZERO;

    /**
     * YTD employer contributions
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal ytdEmployerContributions = BigDecimal.ZERO;

    /**
     * Employee contribution percentage of gross pay
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal employeeContributionPercent;

    /**
     * Whether catch-up contribution is included (age 50+)
     */
    @Column(nullable = false)
    private Boolean includesCatchUp = false;

    /**
     * Catch-up contribution amount (if included)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal catchUpAmount = BigDecimal.ZERO;

    /**
     * Gets total employee contribution for this period.
     */
    public BigDecimal getTotalEmployeeContribution() {
        return employeePreTaxContribution
                .add(employeeRothContribution)
                .add(employeeAfterTaxContribution);
    }

    /**
     * Gets total employer contribution for this period.
     */
    public BigDecimal getTotalEmployerContribution() {
        return employerMatchContribution.add(employerNonElectiveContribution);
    }

    /**
     * Gets total contribution for this period.
     */
    public BigDecimal getTotalContribution() {
        return getTotalEmployeeContribution().add(getTotalEmployerContribution());
    }
}
