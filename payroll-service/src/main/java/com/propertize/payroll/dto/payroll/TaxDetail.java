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
public class TaxDetail {
    private String taxType; // FEDERAL, STATE, LOCAL, FICA_SS, FICA_MEDICARE
    private String jurisdiction;
    private String filingStatus;
    private Integer allowances;
    private BigDecimal taxableWages;
    private BigDecimal amount;
    private BigDecimal rate;
    private String calculationMethod; // PERCENTAGE, FLAT_AMOUNT, TAX_TABLE
}
