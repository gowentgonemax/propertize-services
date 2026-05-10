package com.propertize.payment.entity;

import com.propertize.payment.entity.base.OrganizationScopedEntity;
import com.propertize.payment.enums.DiscountTypeEnum;
import com.propertize.payment.enums.PromoCodeTypeEnum;
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
@Table(name = "promo_code", uniqueConstraints = {
                @UniqueConstraint(name = "uk_promo_code_org", columnNames = { "code", "organization_id" })
}, indexes = {
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

        @Column(name = "code", nullable = false, length = 50)
        private String code;

        @Column(name = "description", length = 500)
        private String description;

        @Enumerated(EnumType.STRING)
        @Column(name = "promo_code_type", length = 30)
        private PromoCodeTypeEnum promoCodeType = PromoCodeTypeEnum.GENERAL;

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

        public String getId() {
                return id;
        }

        public void setId(String id) {
                this.id = id;
        }

        public String getCode() {
                return code;
        }

        public void setCode(String code) {
                this.code = code;
        }

        public String getDescription() {
                return description;
        }

        public void setDescription(String description) {
                this.description = description;
        }

        public PromoCodeTypeEnum getPromoCodeType() {
                return promoCodeType;
        }

        public void setPromoCodeType(PromoCodeTypeEnum promoCodeType) {
                this.promoCodeType = promoCodeType;
        }

        public DiscountTypeEnum getDiscountType() {
                return discountType;
        }

        public void setDiscountType(DiscountTypeEnum discountType) {
                this.discountType = discountType;
        }

        public BigDecimal getDiscountValue() {
                return discountValue;
        }

        public void setDiscountValue(BigDecimal discountValue) {
                this.discountValue = discountValue;
        }

        public Integer getMaxUses() {
                return maxUses;
        }

        public void setMaxUses(Integer maxUses) {
                this.maxUses = maxUses;
        }

        public Integer getCurrentUses() {
                return currentUses;
        }

        public void setCurrentUses(Integer currentUses) {
                this.currentUses = currentUses;
        }

        public LocalDateTime getExpiresAt() {
                return expiresAt;
        }

        public void setExpiresAt(LocalDateTime expiresAt) {
                this.expiresAt = expiresAt;
        }

        public Boolean getActive() {
                return active;
        }

        public void setActive(Boolean active) {
                this.active = active;
        }
}
