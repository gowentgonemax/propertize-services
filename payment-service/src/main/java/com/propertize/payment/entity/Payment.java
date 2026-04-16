package com.propertize.payment.entity;

import com.propertize.payment.entity.base.OrganizationScopedEntity;
import com.propertize.commons.enums.payment.PaymentCategoryEnum;
import com.propertize.commons.enums.payment.PaymentContextEnum;
import com.propertize.commons.enums.payment.PaymentMethodEnum;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.commons.enums.payment.PaymentTypeEnum;
import com.propertize.payment.enums.PaymentGatewayEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payment entity — generic multi-context payment record.
 *
 * Cross-domain entities (Tenant, Lease, Vendor, Organization, Invoice) are
 * referenced by their String IDs only. No JPA joins across service boundaries.
 */
@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_tenant", columnList = "tenant_id"),
        @Index(name = "idx_payment_lease", columnList = "lease_id"),
        @Index(name = "idx_payment_organization", columnList = "organization_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_date", columnList = "payment_date"),
        @Index(name = "idx_payment_category", columnList = "payment_category"),
        @Index(name = "idx_payment_context", columnList = "payment_context"),
        @Index(name = "idx_payment_vendor", columnList = "vendor_id"),
        @Index(name = "idx_payment_owner", columnList = "owner_id")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class Payment extends OrganizationScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    // ── Cross-domain references (IDs only — no JPA joins) ──────────────

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "lease_id", length = 36)
    private String leaseId;

    @Column(name = "vendor_id", length = 36)
    private String vendorId;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "owner_username", length = 100)
    private String ownerUsername;

    @Column(name = "processed_by_user_id")
    private Long processedByUserId;

    @Column(name = "processed_by_username", length = 100)
    private String processedByUsername;

    // ── Payment Classification ──────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_category", length = 50, nullable = false)
    private PaymentCategoryEnum paymentCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_context", length = 50, nullable = false)
    private PaymentContextEnum paymentContext;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethodEnum paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PaymentStatusEnum status = PaymentStatusEnum.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 50)
    private PaymentTypeEnum paymentType;

    @Column(name = "subscription_plan_id", length = 36)
    private String subscriptionPlanId;

    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDate billingPeriodEnd;

    @Column(name = "property_id", length = 36)
    private String propertyId;

    @Column(name = "unit_id", length = 36)
    private String unitId;

    @Column(name = "maintenance_request_id", length = 36)
    private String maintenanceRequestId;

    // ── Gateway / Stripe fields ─────────────────────────────────────────

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "stripe_charge_id", length = 255)
    private String stripeChargeId;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "receipt_url", length = 512)
    private String receiptUrl;

    // ── Promo Code ──────────────────────────────────────────────────────

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "late_fee", precision = 10, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", length = 50)
    private PaymentGatewayEnum paymentGateway;

    // ── Notes ───────────────────────────────────────────────────────────

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
