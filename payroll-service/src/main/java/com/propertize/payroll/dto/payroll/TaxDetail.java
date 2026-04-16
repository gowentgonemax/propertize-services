package com.propertize.payroll.dto.payroll;

import com.propertize.payroll.enums.TaxTypeEnum;

import com.propertize.payroll.enums.FilingStatusEnum;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxDetail {
    private TaxTypeEnum taxType;
    private String jurisdiction;
    private FilingStatusEnum filingStatus;
    private Integer allowances;
    private BigDecimal taxableWages;
    private BigDecimal amount;
    private BigDecimal rate;
    private String calculationMethod; // PERCENTAGE, FLAT_AMOUNT, TAX_TABLE
}
