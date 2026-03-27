package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.DeductionMethodEnum;
import com.propertize.payroll.enums.DeductionStatusEnum;
import com.propertize.payroll.enums.DeductionTypeDetailEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing a recurring deduction configuration for an employee.
 */
@Entity
@Table(name = "deductions", indexes = {
    @Index(name = "idx_deduction_employee", columnList = "employee_id"),
    @Index(name = "idx_deduction_type", columnList = "deduction_type"),
    @Index(name = "idx_deduction_status", columnList = "status")
})
@Getter
@Setter
public class Deduction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "deduction_type", nullable = false, length = 30)
    private DeductionTypeDetailEnum deductionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeductionMethodEnum method;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "max_amount", precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "ytd_amount", precision = 15, scale = 2)
    private BigDecimal ytdAmount = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DeductionStatusEnum status = DeductionStatusEnum.ACTIVE;

    @Column(name = "is_pre_tax")
    private Boolean isPreTax = false;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(length = 500)
    private String notes;

    /**
     * Calculates the deduction amount based on gross pay.
     */
    public BigDecimal calculateDeduction(BigDecimal grossPay) {
        if (method == DeductionMethodEnum.FLAT_AMOUNT) {
            return amount != null ? amount : BigDecimal.ZERO;
        } else if (method == DeductionMethodEnum.PERCENTAGE && percentage != null) {
            BigDecimal calculated = grossPay.multiply(percentage).divide(BigDecimal.valueOf(100));
            if (maxAmount != null && calculated.compareTo(maxAmount) > 0) {
                return maxAmount;
            }
            return calculated;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Checks if the deduction is currently active.
     */
    public boolean isActive() {
        if (status != DeductionStatusEnum.ACTIVE) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return (startDate == null || !today.isBefore(startDate)) &&
               (endDate == null || !today.isAfter(endDate));
    }
}
