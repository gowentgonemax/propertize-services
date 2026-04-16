package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.entity.embedded.DatePeriod;
import com.propertize.payroll.entity.embedded.PayrollTotals;
import com.propertize.commons.enums.employee.PayrollStatusEnum;
import com.propertize.payroll.enums.PayrollTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a payroll run for a client.
 */
@Entity
@Table(name = "payroll_runs", indexes = {
        @Index(name = "idx_payroll_client", columnList = "client_id"),
        @Index(name = "idx_payroll_status", columnList = "status"),
        @Index(name = "idx_payroll_pay_date", columnList = "payDate")
})
@Getter
@Setter
@BatchSize(size = 25)
public class PayrollRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "pay_period_start", nullable = false)),
            @AttributeOverride(name = "endDate", column = @Column(name = "pay_period_end", nullable = false))
    })
    private DatePeriod payPeriod;

    @Column(nullable = false)
    private LocalDate payDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollStatusEnum status = PayrollStatusEnum.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PayrollTypeEnum payrollType = PayrollTypeEnum.REGULAR;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "totalGrossPay", column = @Column(name = "total_gross_pay")),
            @AttributeOverride(name = "totalNetPay", column = @Column(name = "total_net_pay")),
            @AttributeOverride(name = "totalTaxes", column = @Column(name = "total_taxes")),
            @AttributeOverride(name = "totalDeductions", column = @Column(name = "total_deductions"))
    })
    private PayrollTotals totals = new PayrollTotals();

    @Column
    private Integer employeeCount;

    @Column
    private LocalDateTime processedAt;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private LocalDateTime submittedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Paystub> paystubs = new ArrayList<>();

    @Column(length = 500)
    private String notes;

    /**
     * Validates payroll dates before processing.
     */
    public void validateDates() {
        if (payPeriod == null || payPeriod.getStartDate() == null || payPeriod.getEndDate() == null) {
            throw new IllegalArgumentException("Pay period start and end dates are required");
        }
        if (payPeriod.getStartDate().isAfter(payPeriod.getEndDate())) {
            throw new IllegalArgumentException("Pay period start date must be before end date");
        }
        if (payDate.isBefore(payPeriod.getEndDate())) {
            throw new IllegalArgumentException("Pay date must be on or after pay period end date");
        }
    }

    /**
     * Checks if the payroll run can be processed.
     */
    public boolean canProcess() {
        return status == PayrollStatusEnum.DRAFT;
    }

    /**
     * Checks if the payroll run can be approved.
     */
    public boolean canApprove() {
        return status == PayrollStatusEnum.COMPLETED;
    }

    /**
     * Checks if the payroll run can be submitted.
     */
    public boolean canSubmit() {
        return status == PayrollStatusEnum.APPROVED;
    }
}
