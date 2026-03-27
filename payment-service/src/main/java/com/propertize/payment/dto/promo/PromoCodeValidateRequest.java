package com.propertize.payment.dto.promo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromoCodeValidateRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String organizationId;

    private String applicationId;
    private String applicantEmail;
}
