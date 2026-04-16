package com.propertize.payment.dto.payment.request;

import com.propertize.commons.enums.payment.PaymentMethodEnum;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PlatformSubscriptionPaymentRequest {
    private String organizationId;
    private String subscriptionPlanId;
    private BigDecimal amount;
    private PaymentMethodEnum paymentMethod;
    private String stripeCustomerId;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private String notes;
}
