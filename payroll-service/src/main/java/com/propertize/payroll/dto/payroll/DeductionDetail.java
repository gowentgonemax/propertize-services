package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductionDetail {
    private String deductionType; // HEALTH_INSURANCE, DENTAL, VISION, 401K, LOAN_REPAYMENT, GARNISHMENT
    private String description;
    private BigDecimal amount;
    private boolean preTax;
    private String frequency; // EVERY_PAYCHECK, MONTHLY, QUARTERLY, ANNUAL
    private Integer remainingOccurrences; // null for ongoing deductions
    private String status; // ACTIVE, PENDING, COMPLETED
}
