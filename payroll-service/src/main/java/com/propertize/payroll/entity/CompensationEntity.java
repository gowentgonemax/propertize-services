package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.CompensationTypeEnum;
import com.propertize.payroll.enums.CompensationStatusEnum;
import com.propertize.payroll.enums.PayFrequencyEnum;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing employee compensation details including salary, hourly rates, and adjustments.
 */
@Entity
@Table(name = "compensations", indexes = {
    @Index(name = "idx_compensation_employee", columnList = "employee_id"),
    @Index(name = "idx_compensation_effective_date", columnList = "effectiveDate"),
    @Index(name = "idx_compensation_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompensationTypeEnum compensationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompensationStatusEnum status = CompensationStatusEnum.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayFrequencyEnum payFrequency = PayFrequencyEnum.BI_WEEKLY;

    /**
     * Base hourly rate for hourly employees
     */
    @Column(precision = 15, scale = 4)
    private BigDecimal hourlyRate;

    /**
     * Annual salary for salaried employees
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal annualSalary;

    /**
     * Pay rate per period (calculated from salary or hourly)
     */
    @Column(precision = 15, scale = 4)
    private BigDecimal payRatePerPeriod;

    /**
     * Standard hours per pay period
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal standardHoursPerPeriod;

    /**
     * Overtime multiplier (typically 1.5)
     */
    @Column(precision = 4, scale = 2)
    private BigDecimal overtimeMultiplier = new BigDecimal("1.50");

    /**
     * Double time multiplier (typically 2.0)
     */
    @Column(precision = 4, scale = 2)
    private BigDecimal doubleTimeMultiplier = new BigDecimal("2.00");

    /**
     * Date this compensation becomes effective
     */
    @Column(nullable = false)
    private LocalDate effectiveDate;

    /**
     * End date of this compensation (null if current)
     */
    @Column
    private LocalDate endDate;

    /**
     * Reason for compensation change
     */
    @Column(length = 500)
    private String changeReason;

    /**
     * Additional notes about compensation
     */
    @Column(length = 1000)
    private String notes;

    /**
     * Whether this is the current/active compensation record
     */
    @Column(nullable = false)
    private Boolean isCurrent = true;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column
    private LocalDate approvedDate;

    /**
     * Calculates the effective hourly rate.
     */
    public BigDecimal getEffectiveHourlyRate() {
        if (hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) > 0) {
            return hourlyRate;
        }
        if (annualSalary != null && annualSalary.compareTo(BigDecimal.ZERO) > 0) {
            // Calculate hourly from annual (2080 hours/year standard)
            return annualSalary.divide(new BigDecimal("2080"), 4, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculates overtime rate.
     */
    public BigDecimal getOvertimeRate() {
        return getEffectiveHourlyRate().multiply(overtimeMultiplier);
    }

    /**
     * Calculates double time rate.
     */
    public BigDecimal getDoubleTimeRate() {
        return getEffectiveHourlyRate().multiply(doubleTimeMultiplier);
    }
}
