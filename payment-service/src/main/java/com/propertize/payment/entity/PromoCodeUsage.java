package com.propertize.payment.entity;

import com.propertize.payment.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PromoCodeUsage — audit record for every time a promo code is applied.
 */
@Entity
@Table(name = "promo_code_usage", indexes = {
        @Index(name = "idx_pcu_promo_id", columnList = "promo_code_id"),
        @Index(name = "idx_pcu_application", columnList = "application_id"),
        @Index(name = "idx_pcu_email", columnList = "applicant_email"),
        @Index(name = "idx_pcu_org", columnList = "organization_id")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class PromoCodeUsage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "promo_code_id", nullable = false, length = 36)
    private String promoCodeId;

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "organization_id", length = 36)
    private String organizationId;

    @Column(name = "application_id", length = 36)
    private String applicationId;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "applicant_name", length = 200)
    private String applicantName;

    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
