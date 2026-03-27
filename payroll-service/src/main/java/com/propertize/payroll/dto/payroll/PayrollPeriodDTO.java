package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriodDTO {
    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private LocalDate payDate;

    private String payrollType; // REGULAR, OFF_CYCLE, BONUS, COMMISSION

    private String frequency; // WEEKLY, BI_WEEKLY, SEMI_MONTHLY, MONTHLY

    private Integer periodNumber;

    private String status; // DRAFT, PROCESSING, APPROVED, COMPLETED

    private List<PayrollAdjustment> adjustments;

    private BigDecimal totalGrossPay;
    private BigDecimal totalDeductions;
    private BigDecimal totalTaxes;
    private BigDecimal totalNetPay;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayrollAdjustment {
        private String adjustmentType; // RETROACTIVE_PAY, CORRECTION, BONUS, COMMISSION
        private String description;
        private BigDecimal amount;
        private String category; // EARNINGS, DEDUCTIONS, TAXES
        private LocalDate effectiveDate;
        private Boolean isRecurring;
    }
}
