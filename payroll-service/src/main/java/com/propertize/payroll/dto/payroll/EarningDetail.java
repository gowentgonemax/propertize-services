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
public class EarningDetail {
    private String earningType; // REGULAR, OVERTIME, BONUS, COMMISSION, HOLIDAY, VACATION, PTO
    private String description;
    private BigDecimal hours;
    private BigDecimal rate;
    private BigDecimal amount;
    private boolean taxable;
}
