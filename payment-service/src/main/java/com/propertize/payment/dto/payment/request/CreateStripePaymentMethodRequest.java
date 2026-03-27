package com.propertize.payment.dto.payment.request;

import lombok.Data;

@Data
public class CreateStripePaymentMethodRequest {
    private String organizationId;
    private String tenantId;
    private Long userId;
    private String stripePaymentMethodId;
    private String stripeCustomerId;
    private String brand;
    private String lastFour;
    private Integer expMonth;
    private Integer expYear;
    private String fingerprint;
    private String cardholderName;
}
