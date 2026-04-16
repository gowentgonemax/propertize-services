package com.propertize.payment.dto.payment.request;

import com.propertize.commons.enums.payment.PaymentCategoryEnum;
import com.propertize.commons.enums.payment.PaymentContextEnum;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentFilterRequest {
    private String organizationId;
    private String tenantId;
    private String vendorId;
    private String ownerId;
    private String leaseId;
    private String propertyId;
    private PaymentStatusEnum status;
    private PaymentCategoryEnum paymentCategory;
    private PaymentContextEnum paymentContext;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int page = 1;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortOrder = "desc";
}
