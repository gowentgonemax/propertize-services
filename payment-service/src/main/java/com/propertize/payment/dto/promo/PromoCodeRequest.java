package com.propertize.payment.dto.promo;

import com.propertize.payment.enums.DiscountTypeEnum;
import com.propertize.payment.enums.PromoCodeTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PromoCodeRequest {
    @NotBlank
    private String code;

    private String description;

    @NotBlank
    private String organizationId;

    @NotNull
    private DiscountTypeEnum discountType;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal discountValue;

    @Min(1)
    private Integer maxUses;

    private LocalDateTime expiresAt;

    private boolean active = true;

    private PromoCodeTypeEnum promoCodeType;

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

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public PromoCodeTypeEnum getPromoCodeType() {
        return promoCodeType;
    }

    public void setPromoCodeType(PromoCodeTypeEnum promoCodeType) {
        this.promoCodeType = promoCodeType;
    }
}
