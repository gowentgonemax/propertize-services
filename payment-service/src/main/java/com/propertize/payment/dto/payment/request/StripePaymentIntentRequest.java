package com.propertize.payment.dto.payment.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class StripePaymentIntentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String currency = "usd";

    @NotBlank(message = "Organization ID is required")
    private String organizationId;

    private String customerId;
    private String paymentMethodId;
    private String description;
    private String receiptEmail;
    private boolean automaticCapture = true;
    private String paymentId;
    private Map<String, String> metadata;
}
