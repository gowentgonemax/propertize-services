package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.LeaveTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity tracking employee leave/PTO balances and accruals.
 */
@Entity
@Table(name = "leave_balances", indexes = {
    @Index(name = "idx_leave_bal_employee", columnList = "employee_id"),
    @Index(name = "idx_leave_bal_type", columnList = "leaveType"),
    @Index(name = "idx_leave_bal_year", columnList = "year")
})
@Getter
@Setter
public class LeaveBalanceEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeaveTypeEnum leaveType;

    /**
     * Year this balance applies to
     */
    @Column(nullable = false)
    private Integer year;

    /**
     * Starting balance for the year
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal beginningBalance = BigDecimal.ZERO;

    /**
     * Hours accrued during the year
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal accruedHours = BigDecimal.ZERO;

    /**
     * Hours used during the year
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal usedHours = BigDecimal.ZERO;

    /**
     * Hours adjusted (positive or negative)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal adjustedHours = BigDecimal.ZERO;

    /**
     * Hours carried over from previous year
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal carriedOverHours = BigDecimal.ZERO;

    /**
     * Annual accrual cap (max hours that can accrue)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal annualAccrualCap;

    /**
     * Carryover cap (max hours that can carry over)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal carryoverCap;

    /**
     * Accrual rate per pay period
     */
    @Column(precision = 10, scale = 4)
    private BigDecimal accrualRatePerPeriod;

    /**
     * Whether leave can be used before accrued
     */
    @Column(nullable = false)
    private Boolean allowNegativeBalance = false;

    /**
     * Maximum negative balance allowed
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal maxNegativeBalance;

    /**
     * Last accrual date
     */
    @Column
    private LocalDate lastAccrualDate;

    /**
     * Calculates current available balance.
     */
    public BigDecimal getAvailableBalance() {
        return beginningBalance
            .add(accruedHours)
            .add(carriedOverHours)
            .add(adjustedHours)
            .subtract(usedHours);
    }

    /**
     * Checks if hours can be used.
     */
    public boolean canUseHours(BigDecimal hoursToUse) {
        BigDecimal afterUse = getAvailableBalance().subtract(hoursToUse);
        if (allowNegativeBalance) {
            return maxNegativeBalance == null || afterUse.compareTo(maxNegativeBalance.negate()) >= 0;
        }
        return afterUse.compareTo(BigDecimal.ZERO) >= 0;
    }
}
