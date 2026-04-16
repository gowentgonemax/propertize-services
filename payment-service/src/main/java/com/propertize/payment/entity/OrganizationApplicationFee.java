package com.propertize.payment.entity;

import com.propertize.payment.entity.base.AuditableEntity;
import com.propertize.commons.enums.payment.PaymentMethodEnum;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "organization_application_fee", indexes = {
        @Index(name = "idx_org_app_fee_app", columnList = "organization_application_id"),
        @Index(name = "idx_org_app_fee_tracking", columnList = "tracking_id"),
        @Index(name = "idx_org_app_fee_status", columnList = "payment_status"),
        @Index(name = "idx_org_app_fee_org", columnList = "organization_id")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class OrganizationApplicationFee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    // organizationId is nullable initially — org may not exist yet at onboarding
    @Column(name = "organization_id", length = 36)
    private String organizationId;

    @Column(name = "organization_application_id", nullable = false, length = 36)
    private String organizationApplicationId;

    @Column(name = "tracking_id", unique = true, length = 50)
    private String trackingId;

    @Column(name = "fee_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal feeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30)
    private PaymentStatusEnum paymentStatus = PaymentStatusEnum.PENDING;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethodEnum paymentMethod;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "organization_name", length = 200)
    private String organizationName;

    // Waiver
    @Column(name = "is_waived")
    private Boolean isWaived = false;

    @Column(name = "waived_reason", length = 500)
    private String waivedReason;

    @Column(name = "waived_by", length = 100)
    private String waivedBy;

    @Column(name = "waived_at")
    private OffsetDateTime waivedAt;

    // Refund
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_date")
    private OffsetDateTime refundDate;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refunded_by", length = 100)
    private String refundedBy;

    // Stripe
    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "stripe_client_secret", length = 512)
    private String stripeClientSecret;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "subscription_tier", length = 50)
    private String subscriptionTier;
}
