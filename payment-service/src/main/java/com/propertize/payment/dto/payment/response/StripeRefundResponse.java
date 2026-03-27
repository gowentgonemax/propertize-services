package com.propertize.payment.dto.payment.response;

import lombok.Data;

@Data
public class StripeRefundResponse {
    private String id;
    private Long amount;
    private String currency;
    private String status;
    private String chargeId;
    private String paymentIntentId;
    private String reason;
    private Long created;
}
