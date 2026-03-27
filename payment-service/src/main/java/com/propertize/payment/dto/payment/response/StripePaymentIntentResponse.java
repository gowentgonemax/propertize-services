package com.propertize.payment.dto.payment.response;

import lombok.Data;

@Data
public class StripePaymentIntentResponse {
    private String id;
    private String clientSecret;
    private String status;
    private Long amount;
    private String currency;
    private String paymentMethodId;
    private String customerId;
    private String description;
    private String receiptEmail;
}
