package com.propertize.payment.dto.payment.response;

import com.propertize.commons.enums.payment.BankAccountTypeEnum;
import com.propertize.commons.enums.payment.PaymentMethodEnum;
import com.propertize.payment.enums.CardBrandEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentMethodResponse {
    private String id;
    private String organizationId;
    private String tenantId;
    private PaymentMethodEnum methodType;
    private CardBrandEnum cardBrand;
    private String lastFour;
    private Integer expMonth;
    private Integer expYear;
    private String cardholderName;
    private BankAccountTypeEnum bankAccountType;
    private String bankName;
    private String stripePaymentMethodId;
    private String stripeCustomerId;
    private boolean isDefault;
    private boolean isActive;
    private LocalDateTime createdAt;
}
