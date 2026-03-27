package com.propertize.payment.dto.payment.response;

import com.propertize.payment.enums.TransactionStatusEnum;
import com.propertize.payment.enums.TransactionTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private String id;
    private String organizationId;
    private String referenceNumber;
    private String paymentId;
    private String tenantId;
    private String leaseId;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private String currency;
    private TransactionTypeEnum transactionType;
    private TransactionStatusEnum status;
    private String providerReferenceId;
    private String paymentGateway;
    private String description;
    private LocalDateTime transactionDate;
    private String createdBy;
}
