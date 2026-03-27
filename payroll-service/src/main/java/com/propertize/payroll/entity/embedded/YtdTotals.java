package com.propertize.payroll.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Embeddable value object representing year-to-date totals.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class YtdTotals {

    @Column(name = "ytd_gross_pay", precision = 15, scale = 2)
    private BigDecimal ytdGrossPay = BigDecimal.ZERO;

    @Column(name = "ytd_net_pay", precision = 15, scale = 2)
    private BigDecimal ytdNetPay = BigDecimal.ZERO;

    @Column(name = "ytd_taxes", precision = 15, scale = 2)
    private BigDecimal ytdTaxes = BigDecimal.ZERO;

    @Column(name = "ytd_deductions", precision = 15, scale = 2)
    private BigDecimal ytdDeductions = BigDecimal.ZERO;

    @Column(name = "ytd_regular_hours", precision = 10, scale = 2)
    private BigDecimal ytdRegularHours = BigDecimal.ZERO;

    @Column(name = "ytd_overtime_hours", precision = 10, scale = 2)
    private BigDecimal ytdOvertimeHours = BigDecimal.ZERO;

    /**
     * Updates YTD totals by adding the current period values.
     */
    public void addPeriod(PayrollTotals periodTotals, BigDecimal regularHours, BigDecimal overtimeHours) {
        if (periodTotals != null) {
            this.ytdGrossPay = safeAdd(this.ytdGrossPay, periodTotals.getTotalGrossPay());
            this.ytdNetPay = safeAdd(this.ytdNetPay, periodTotals.getTotalNetPay());
            this.ytdTaxes = safeAdd(this.ytdTaxes, periodTotals.getTotalTaxes());
            this.ytdDeductions = safeAdd(this.ytdDeductions, periodTotals.getTotalDeductions());
        }
        this.ytdRegularHours = safeAdd(this.ytdRegularHours, regularHours);
        this.ytdOvertimeHours = safeAdd(this.ytdOvertimeHours, overtimeHours);
    }

    private BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.add(b);
    }
}
