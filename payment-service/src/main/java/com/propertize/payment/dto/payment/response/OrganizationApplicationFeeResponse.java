package com.propertize.payment.dto.payment.response;

import com.propertize.payment.enums.PaymentStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrganizationApplicationFeeResponse {
    private Long id;
    private String trackingId;
    private String organizationName;
    private String organizationEmail;
    private BigDecimal feeAmount;
    private PaymentStatusEnum paymentStatus;
    private String stripePaymentIntentId;
    private String stripeClientSecret;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
