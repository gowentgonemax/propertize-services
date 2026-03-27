package com.propertize.payment.dto.payment.request;

import com.propertize.payment.enums.PaymentCategoryEnum;
import com.propertize.payment.enums.PaymentContextEnum;
import com.propertize.payment.enums.PaymentMethodEnum;
import com.propertize.payment.enums.PaymentTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentCreateRequest {

    @NotNull(message = "Payment category is required")
    private PaymentCategoryEnum paymentCategory;

    @NotNull(message = "Payment context is required")
    private PaymentContextEnum paymentContext;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    private PaymentMethodEnum paymentMethod;
    private PaymentTypeEnum paymentType;

    private String tenantId;
    private String leaseId;
    private String vendorId;
    private String propertyId;
    private String unitId;
    private String maintenanceRequestId;
    private String invoiceId;
    private String subscriptionPlanId;
    private Long ownerId;
    private String ownerUsername;
    private String organizationId;

    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;

    private String promoCode;
    private String description;
    private String notes;
}
