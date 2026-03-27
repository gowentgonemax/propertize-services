package com.propertize.payment.entity;

import com.propertize.payment.entity.base.OrganizationScopedEntity;
import com.propertize.payment.enums.PaymentPlanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "payment_plan", indexes = {
        @Index(name = "idx_plan_tenant", columnList = "tenant_id"),
        @Index(name = "idx_plan_lease", columnList = "lease_id"),
        @Index(name = "idx_plan_org", columnList = "organization_id"),
        @Index(name = "idx_plan_status", columnList = "status")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "installments")
@NoArgsConstructor
public class PaymentPlan extends OrganizationScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "lease_id", length = 36)
    private String leaseId;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "down_payment", precision = 10, scale = 2)
    private BigDecimal downPayment;

    @Column(name = "installment_amount", precision = 10, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "number_of_installments", nullable = false)
    private Integer numberOfInstallments;

    @Column(name = "frequency", length = 20)
    private String frequency;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private PaymentPlanStatus status = PaymentPlanStatus.ACTIVE;

    @Column(name = "description", length = 500)
    private String description;

    @OneToMany(mappedBy = "paymentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentPlanInstallment> installments;
}
