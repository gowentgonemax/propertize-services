package com.propertize.payment.dto.payment.request;

import com.propertize.commons.enums.payment.BankAccountTypeEnum;
import lombok.Data;

@Data
public class CreateACHPaymentMethodRequest {
    private String organizationId;
    private String tenantId;
    private Long userId;
    private String stripePaymentMethodId;
    private String stripeCustomerId;
    private String bankName;
    private BankAccountTypeEnum accountType;
    private String lastFour;
    private String routingLastFour;
}
