package com.propertize.payment.dto.payment.request;

import com.propertize.commons.enums.payment.PaymentMethodEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BulkPaymentRequest {
    private String organizationId;
    private List<String> tenantIds;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethodEnum paymentMethod;
    private String notes;
}
