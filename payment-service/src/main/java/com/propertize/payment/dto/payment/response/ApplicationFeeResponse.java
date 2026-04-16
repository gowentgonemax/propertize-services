package com.propertize.payment.dto.payment.response;

import com.propertize.commons.enums.payment.PaymentStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ApplicationFeeResponse {
    private Long id;
    private String organizationId;
    private String rentalApplicationId;
    private String applicantId;
    private String applicantEmail;
    private BigDecimal feeAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String promoCodeUsed;
    private PaymentStatusEnum paymentStatus;
    private String stripePaymentIntentId;
    private String stripeChargeId;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
