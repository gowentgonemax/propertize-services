package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YTDTotals {
    private BigDecimal ytdGrossEarnings;
    private BigDecimal ytdTaxableWages;
    private BigDecimal ytdNetPay;
    private Map<String, BigDecimal> ytdEarningsByType; // Regular, Overtime, Bonus, etc.
    private Map<String, BigDecimal> ytdDeductionsByType; // Health Insurance, 401k, etc.
    private Map<String, BigDecimal> ytdTaxesByType; // Federal, State, FICA, etc.
    private BigDecimal ytdTotalTaxes;
    private BigDecimal ytdTotalDeductions;

    // Time Off Tracking
    private Double vacationHoursUsed;
    private Double vacationHoursRemaining;
    private Double sickHoursUsed;
    private Double sickHoursRemaining;
    private Double ptoHoursUsed;
    private Double ptoHoursRemaining;
}
