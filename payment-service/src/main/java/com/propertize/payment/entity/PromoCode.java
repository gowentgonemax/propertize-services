package com.propertize.payment.entity;

import com.propertize.payment.entity.base.OrganizationScopedEntity;
import com.propertize.payment.enums.DiscountTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PromoCode entity — Promotional codes for application fee discounts and other
 * offers.
 *
 * Org-scoped: each promo code belongs to a specific organization.
 * Platform-level codes have a null organizationId (override required).
 */
@Entity
@Table(name = "promo_code", indexes = {
        @Index(name = "idx_promo_code", columnList = "code"),
        @Index(name = "idx_promo_org", columnList = "organization_id"),
        @Index(name = "idx_promo_active", columnList = "active"),
        @Index(name = "idx_promo_expires", columnList = "expires_at")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class PromoCode extends OrganizationScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountTypeEnum discountType;

    /** Discount value: percentage (0-100) or fixed dollar amount */
    @Column(name = "discount_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;

    /** 0 = unlimited uses */
    @Column(name = "max_uses")
    private Integer maxUses = 0;

    @Column(name = "current_uses")
    private Integer currentUses = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "active")
    private Boolean active = true;

    /** Minimum order amount required to apply this promo */
    @Column(name = "min_amount", precision = 10, scale = 2)
    private BigDecimal minAmount;

    /** Maximum discount cap (for percentage discounts) */
    @Column(name = "max_discount_cap", precision = 10, scale = 2)
    private BigDecimal maxDiscountCap;
}
