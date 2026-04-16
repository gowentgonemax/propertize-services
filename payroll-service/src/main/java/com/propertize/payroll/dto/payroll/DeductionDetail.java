package com.propertize.payroll.dto.payroll;

import com.propertize.payroll.enums.DeductionStatusEnum;
import com.propertize.payroll.enums.DeductionTypeEnum;

import com.propertize.commons.enums.employee.PayFrequencyEnum;

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
    private DeductionTypeEnum deductionType;
    private String description;
    private BigDecimal amount;
    private boolean preTax;
    private PayFrequencyEnum frequency; // EVERY_PAYCHECK, MONTHLY, QUARTERLY, ANNUAL
    private Integer remainingOccurrences; // null for ongoing deductions
    private DeductionStatusEnum status;
}
