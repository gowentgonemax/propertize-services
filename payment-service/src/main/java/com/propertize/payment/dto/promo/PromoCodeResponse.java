package com.propertize.payment.dto.promo;

import com.propertize.payment.enums.DiscountTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromoCodeResponse {
    private Long id;
    private String code;
    private String description;
    private String organizationId;
    private DiscountTypeEnum discountType;
    private BigDecimal discountValue;
    private Integer maxUses;
    private Integer currentUses;
    private LocalDateTime expiresAt;
    private boolean active;
    private boolean expired;
    private boolean depleted;
    private LocalDateTime createdAt;
    private String createdBy;
}
