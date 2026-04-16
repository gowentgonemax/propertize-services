package com.propertize.payment.dto.payment.request;

import com.propertize.commons.enums.payment.PaymentMethodEnum;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OwnerPayoutRequest {
    private String organizationId;
    private Long ownerId;
    private String ownerUsername;
    private String propertyId;
    private BigDecimal amount;
    private PaymentMethodEnum paymentMethod;
    private String stripeCustomerId;
    private String description;
    private String notes;
}
