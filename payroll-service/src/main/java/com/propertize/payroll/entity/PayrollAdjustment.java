package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.entity.embedded.DatePeriod;
import com.propertize.payroll.enums.AdjustmentStatusEnum;
import com.propertize.payroll.enums.AdjustmentTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing a payroll adjustment for an employee.
 */
@Entity
@Table(name = "payroll_adjustments", indexes = {
    @Index(name = "idx_adjustment_employee", columnList = "employee_id"),
    @Index(name = "idx_adjustment_payroll_run", columnList = "payroll_run_id"),
    @Index(name = "idx_adjustment_status", columnList = "status")
})
@Getter
@Setter
public class PayrollAdjustment extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private String employeeId; // Reference to Employee in Employee Microservice

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 30)
    private AdjustmentTypeEnum adjustmentType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String reason;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AdjustmentStatusEnum status = AdjustmentStatusEnum.PENDING;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private java.time.LocalDateTime approvedAt;

    /**
     * Checks if the adjustment is positive (addition).
     */
    public boolean isAddition() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if the adjustment is negative (deduction).
     */
    public boolean isDeduction() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Approves the adjustment.
     */
    public void approve(String approverUsername) {
        this.status = AdjustmentStatusEnum.APPROVED;
        this.approvedBy = approverUsername;
        this.approvedAt = java.time.LocalDateTime.now();
    }

    /**
     * Rejects the adjustment.
     */
    public void reject(String rejectionReason) {
        this.status = AdjustmentStatusEnum.REJECTED;
        this.reason = rejectionReason;
    }
}
