package com.propertize.payment.dto.payment.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ApplicationFeeRequest {
    private String organizationId;
    private String rentalApplicationId;
    private String applicantId;
    private String applicantEmail;
    private BigDecimal feeAmount;
    private String promoCode;
    private String paymentMethodId;
    private String stripePaymentMethodId;
    private String stripeCustomerId;
    private String notes;
}
