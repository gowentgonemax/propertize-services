package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.TaxDepositStatusEnum;
import com.propertize.payroll.enums.TaxTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity for tracking payroll tax deposits (Federal, State, Local)
 */
@Entity
@Table(name = "payroll_tax_deposits", indexes = {
    @Index(name = "idx_tax_deposit_client", columnList = "client_id"),
    @Index(name = "idx_tax_deposit_due_date", columnList = "due_date"),
    @Index(name = "idx_tax_deposit_status", columnList = "status")
})
@Getter
@Setter
public class PayrollTaxDepositEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 50)
    private TaxTypeEnum taxType;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaxDepositStatusEnum status = TaxDepositStatusEnum.PENDING;

    @Column(name = "quarter")
    private Integer quarter;

    @Column(name = "tax_year")
    private Integer taxYear;

    @Column(length = 500)
    private String notes;
}
