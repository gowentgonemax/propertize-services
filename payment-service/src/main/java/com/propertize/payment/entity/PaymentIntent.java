package com.propertize.payment.entity;

import com.propertize.payment.entity.base.OrganizationScopedEntity;
import com.propertize.payment.enums.PaymentIntentStatus;
import com.propertize.payment.enums.PaymentMethodEnum;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "payment_intent", indexes = {
        @Index(name = "idx_pi_stripe_id", columnList = "stripe_payment_intent_id"),
        @Index(name = "idx_pi_org", columnList = "organization_id"),
        @Index(name = "idx_pi_status", columnList = "status"),
        @Index(name = "idx_pi_confirmation", columnList = "confirmation_number")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class PaymentIntent extends OrganizationScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PaymentIntentStatus status = PaymentIntentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethodEnum paymentMethod;

    // Stripe fields
    @Column(name = "stripe_payment_intent_id", unique = true, length = 255)
    private String stripePaymentIntentId;

    @Column(name = "stripe_client_secret", length = 512)
    private String stripeClientSecret;

    @Column(name = "stripe_charge_id", length = 255)
    private String stripeChargeId;

    // Processor fields
    @Column(name = "processor_name", length = 50)
    private String processorName;

    @Column(name = "processor_transaction_id", length = 255)
    private String processorTransactionId;

    // Lifecycle timestamps
    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Error details
    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    // Receipt
    @Column(name = "receipt_url", length = 512)
    private String receiptUrl;

    @Column(name = "receipt_email", length = 255)
    private String receiptEmail;

    @Column(name = "confirmation_number", unique = true, length = 50)
    private String confirmationNumber;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
