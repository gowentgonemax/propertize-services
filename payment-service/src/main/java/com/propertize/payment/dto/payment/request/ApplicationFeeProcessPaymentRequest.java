package com.propertize.payment.dto.payment.request;

import lombok.Data;

@Data
public class ApplicationFeeProcessPaymentRequest {
    private String stripePaymentMethodId;
    private String stripeCustomerId;
    private String paymentMethodId;
    private String promoCode;
}
