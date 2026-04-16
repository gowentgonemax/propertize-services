package com.propertize.payment.dto.payment.request;

import com.propertize.commons.enums.payment.PaymentStatusEnum;
import lombok.Data;

@Data
public class PaymentUpdateRequest {
    private PaymentStatusEnum status;
    private String notes;
    private String failureReason;
}
