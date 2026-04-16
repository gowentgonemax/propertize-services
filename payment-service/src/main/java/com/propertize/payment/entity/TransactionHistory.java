package com.propertize.payment.entity;

import com.propertize.payment.config.PaymentConstants;
import com.propertize.payment.entity.base.OrganizationScopedEntity;
import com.propertize.payment.enums.PaymentGatewayEnum;
import com.propertize.commons.enums.payment.PaymentMethodEnum;
import com.propertize.commons.enums.payment.TransactionStatusEnum;
import com.propertize.commons.enums.payment.TransactionTypeEnum;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "transaction_history", indexes = {
        @Index(name = "idx_txn_org", columnList = "organization_id"),
        @Index(name = "idx_txn_tenant", columnList = "tenant_id"),
        @Index(name = "idx_txn_property", columnList = "property_id"),
        @Index(name = "idx_txn_lease", columnList = "lease_id"),
        @Index(name = "idx_txn_ref", columnList = "reference_number"),
        @Index(name = "idx_txn_status", columnList = "status"),
        @Index(name = "idx_txn_date", columnList = "transaction_date")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class TransactionHistory extends OrganizationScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "reference_number", unique = true, length = 50)
    private String referenceNumber;

    // Cross-domain IDs
    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "property_id", length = 36)
    private String propertyId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "lease_id", length = 36)
    private String leaseId;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Column(name = "application_fee_id", length = 36)
    private String applicationFeeId;

    @Column(name = "rental_application_id", length = 36)
    private String rentalApplicationId;

    @Column(name = "organization_application_id", length = 36)
    private String organizationApplicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 50, nullable = false)
    private TransactionTypeEnum transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TransactionStatusEnum status = TransactionStatusEnum.PENDING;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency = PaymentConstants.DEFAULT_CURRENCY;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "description", length = 500)
    private String description;

    // Gateway
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", length = 50)
    private PaymentGatewayEnum paymentGateway;

    @Column(name = "provider_reference_id", length = 255)
    private String providerReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethodEnum paymentMethod;

    @Column(name = "card_brand", length = 30)
    private String cardBrand;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    // Balance tracking
    @Column(name = "balance_before", precision = 12, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    // Fees
    @Column(name = "processing_fee", precision = 8, scale = 2)
    private BigDecimal processingFee;

    @Column(name = "platform_fee", precision = 8, scale = 2)
    private BigDecimal platformFee;

    // Refund tracking
    @Column(name = "original_transaction_id", length = 36)
    private String originalTransactionId;

    @Column(name = "refund_transaction_id", length = 36)
    private String refundTransactionId;

    // Error details
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    // Audit
    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
