package com.propertize.payment.dto.payment.request;

import com.propertize.payment.enums.PaymentStatusEnum;
import lombok.Data;

@Data
public class PaymentUpdateRequest {
    private PaymentStatusEnum status;
    private String notes;
    private String failureReason;
}
