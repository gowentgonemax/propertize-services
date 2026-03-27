package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollEmployeeDetail {
    private Long employeeId;
    private String employeeName;
    private String employmentType; // FULL_TIME, PART_TIME, CONTRACT
    private String payType; // SALARY, HOURLY, COMMISSION
    private BigDecimal regularHours;
    private BigDecimal overtimeHours;
    private BigDecimal regularRate;
    private BigDecimal overtimeRate;
    private BigDecimal basePay;
    private BigDecimal grossPay;
    private BigDecimal netPay;
    private List<EarningDetail> earnings;
    private List<DeductionDetail> deductions;
    private List<TaxDetail> taxes;
    private BankingDetail bankingInfo;
    private YTDTotals ytdTotals;
}
