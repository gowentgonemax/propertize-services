package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankingDetail {
    private String bankName;
    private String accountType; // CHECKING, SAVINGS
    private String accountLastFour;
    private String routingNumber;
    private Integer distributionPriority;
    private String distributionType; // PERCENTAGE, FIXED_AMOUNT
    private String amount; // Can be either percentage or fixed amount
    private String status; // ACTIVE, PENDING_VERIFICATION, INACTIVE
}
