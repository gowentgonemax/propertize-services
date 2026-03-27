package com.propertize.payment.dto.payment.response;

import com.propertize.payment.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private String organizationId;
    private String tenantId;
    private String tenantName;
    private String leaseId;
    private String propertyId;
    private String propertyAddress;
    private String vendorId;
    private String ownerId;
    private BigDecimal amount;
    private BigDecimal lateFee;
    private BigDecimal discount;
    private BigDecimal netAmount;
    private LocalDate paymentDate;
    private LocalDate dueDate;
    private PaymentStatusEnum status;
    private PaymentMethodEnum paymentMethod;
    private PaymentCategoryEnum paymentCategory;
    private PaymentContextEnum paymentContext;
    private PaymentTypeEnum paymentType;
    private PaymentGatewayEnum paymentGateway;
    private String stripePaymentIntentId;
    private String stripeChargeId;
    private String transactionId;
    private String receiptUrl;
    private String notes;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
