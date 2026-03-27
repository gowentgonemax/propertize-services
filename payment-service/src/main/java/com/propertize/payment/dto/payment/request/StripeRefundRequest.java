package com.propertize.payment.dto.payment.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StripeRefundRequest {
    private String paymentIntentId;
    private String chargeId;
    private BigDecimal amount;
    private String reason;
}
