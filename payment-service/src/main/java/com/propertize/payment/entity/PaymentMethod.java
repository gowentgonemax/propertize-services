package com.propertize.payment.entity;

import com.propertize.payment.entity.base.OrganizationScopedEntity;
import com.propertize.commons.enums.payment.BankAccountTypeEnum;
import com.propertize.payment.enums.CardBrandEnum;
import com.propertize.commons.enums.payment.PaymentMethodEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_method", indexes = {
        @Index(name = "idx_pm_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pm_user", columnList = "user_id"),
        @Index(name = "idx_pm_org", columnList = "organization_id"),
        @Index(name = "idx_pm_stripe_id", columnList = "stripe_payment_method_id"),
        @Index(name = "idx_pm_default", columnList = "is_default")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class PaymentMethod extends OrganizationScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", length = 50, nullable = false)
    private PaymentMethodEnum methodType;

    // Card fields (never store raw card data — only tokenized)
    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand", length = 30)
    private CardBrandEnum cardBrand;

    @Column(name = "last_four", length = 4)
    private String lastFour;

    @Column(name = "cardholder_name", length = 100)
    private String cardholderName;

    @Column(name = "exp_month")
    private Integer expMonth;

    @Column(name = "exp_year")
    private Integer expYear;

    // Stripe tokenization
    @Column(name = "stripe_payment_method_id", length = 255)
    private String stripePaymentMethodId;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    // ACH / Bank fields
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_account_type", length = 30)
    private BankAccountTypeEnum bankAccountType;

    @Column(name = "routing_last_four", length = 4)
    private String routingLastFour;

    // Billing address
    @Column(name = "billing_line1", length = 200)
    private String billingLine1;

    @Column(name = "billing_line2", length = 200)
    private String billingLine2;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 50)
    private String billingState;

    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    @Column(name = "billing_country", length = 2)
    private String billingCountry;

    // Status flags
    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    // Deduplication
    @Column(name = "fingerprint", length = 255)
    private String fingerprint;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
