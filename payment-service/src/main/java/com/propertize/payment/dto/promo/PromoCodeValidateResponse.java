package com.propertize.payment.dto.promo;

import com.propertize.payment.enums.DiscountTypeEnum;

import java.math.BigDecimal;

public class PromoCodeValidateResponse {
    private boolean valid;
    private String promoCodeId;
    private String code;
    private DiscountTypeEnum discountType;
    private BigDecimal discountValue;
    private String message;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getPromoCodeId() {
        return promoCodeId;
    }

    public void setPromoCodeId(String promoCodeId) {
        this.promoCodeId = promoCodeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
