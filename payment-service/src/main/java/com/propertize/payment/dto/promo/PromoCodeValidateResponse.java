package com.propertize.payment.dto.promo;

import com.propertize.payment.enums.DiscountTypeEnum;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromoCodeValidateResponse {
    private boolean valid;
    private String promoCodeId;
    private String code;
    private DiscountTypeEnum discountType;
    private BigDecimal discountValue;
    private String message;
}
