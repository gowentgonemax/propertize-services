package com.propertize.payment.entity;

import com.propertize.payment.entity.base.OrganizationScopedEntity;
import com.propertize.payment.enums.PaymentMethodType;
import com.propertize.payment.enums.PaymentStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "application_fee", indexes = {
        @Index(name = "idx_app_fee_application", columnList = "rental_application_id"),
        @Index(name = "idx_app_fee_org", columnList = "organization_id"),
        @Index(name = "idx_app_fee_status", columnList = "payment_status"),
        @Index(name = "idx_app_fee_property", columnList = "property_id")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class ApplicationFee extends OrganizationScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "rental_application_id", nullable = false, length = 36)
    private String rentalApplicationId;

    @Column(name = "property_id", nullable = false, length = 36)
    private String propertyId;

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
    @Column(name = "payment_method", length = 30)
    private PaymentMethodType paymentMethod;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    // Applicant details (denormalized)
    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "applicant_first_name", length = 100)
    private String applicantFirstName;

    @Column(name = "applicant_last_name", length = 100)
    private String applicantLastName;

    @Column(name = "applicant_name", length = 200)
    private String applicantName;

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

    // Promo code / discount
    @Column(name = "promo_code_used", length = 50)
    private String promoCodeUsed;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "paid_at")
    private java.time.LocalDateTime paidAt;

    @Column(name = "notes", length = 1000)
    private String notes;
}
