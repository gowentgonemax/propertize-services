package com.propertize.payment.entity;

import com.propertize.payment.entity.base.AuditableEntity;
import com.propertize.payment.enums.InstallmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment_plan_installment")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class PaymentPlanInstallment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_plan_id", nullable = false)
    private PaymentPlan paymentPlan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @Column(name = "paid_date")
    private LocalDate paidDate;
}
