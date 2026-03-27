package com.propertize.payment.dto.payment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttachPaymentMethodRequest {

    @NotBlank
    private String stripePaymentMethodId;

    @NotBlank
    private String stripeCustomerId;

    private String tenantId;
    private String organizationId;
}
