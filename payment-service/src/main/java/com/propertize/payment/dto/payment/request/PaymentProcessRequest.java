package com.propertize.payment.dto.payment.request;

import lombok.Data;

@Data
public class PaymentProcessRequest {
    private String paymentMethodId;
    private String stripePaymentMethodId;
    private String stripePaymentIntentId;
    private String notes;
}
