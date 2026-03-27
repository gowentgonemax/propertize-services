package com.propertize.payment.dto.promo;

import com.propertize.payment.enums.DiscountTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
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
}
