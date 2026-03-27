package com.propertize.payment.dto.payment.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VendorPaymentRequest {
    private String organizationId;
    private String vendorId;
    private String maintenanceRequestId;
    private BigDecimal amount;
    private String paymentMethod;
    private String description;
    private String notes;
}
