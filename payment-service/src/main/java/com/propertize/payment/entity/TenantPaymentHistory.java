package com.propertize.payment.entity;

import com.propertize.payment.entity.base.OrganizationScopedEntity;
import com.propertize.commons.enums.payment.PaymentMethodEnum;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.commons.enums.payment.PaymentTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_payment_history", indexes = {
        @Index(name = "idx_tph_tenant", columnList = "tenant_id"),
        @Index(name = "idx_tph_property", columnList = "property_id"),
        @Index(name = "idx_tph_lease", columnList = "lease_id"),
        @Index(name = "idx_tph_status", columnList = "status"),
        @Index(name = "idx_tph_org", columnList = "organization_id")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class TenantPaymentHistory extends OrganizationScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    // Cross-domain IDs
    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "property_id", length = 36)
    private String propertyId;

    @Column(name = "lease_id", length = 36)
    private String leaseId;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    // Snapshot data (denormalized for reporting)
    @Column(name = "tenant_name", length = 200)
    private String tenantName;

    @Column(name = "property_name", length = 200)
    private String propertyName;

    @Column(name = "property_address", length = 500)
    private String propertyAddress;

    @Column(name = "lease_number", length = 50)
    private String leaseNumber;

    // Payment details
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 50)
    private PaymentTypeEnum paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethodEnum paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private PaymentStatusEnum status;

    @Column(name = "amount_due", precision = 10, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDate billingPeriodEnd;

    @Column(name = "is_on_time")
    private Boolean isOnTime;

    @Column(name = "days_late")
    private Integer daysLate;

    @Column(name = "late_fee_amount", precision = 10, scale = 2)
    private BigDecimal lateFeeAmount;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}
