package com.propertize.payroll.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Embeddable value object representing payroll totals (gross, net, taxes, deductions).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PayrollTotals {

    @Column(name = "total_gross_pay", precision = 15, scale = 2)
    private BigDecimal totalGrossPay = BigDecimal.ZERO;

    @Column(name = "total_net_pay", precision = 15, scale = 2)
    private BigDecimal totalNetPay = BigDecimal.ZERO;

    @Column(name = "total_taxes", precision = 15, scale = 2)
    private BigDecimal totalTaxes = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 15, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    /**
     * Adds another PayrollTotals to this one.
     */
    public void add(PayrollTotals other) {
        if (other == null) return;

        this.totalGrossPay = safeAdd(this.totalGrossPay, other.totalGrossPay);
        this.totalNetPay = safeAdd(this.totalNetPay, other.totalNetPay);
        this.totalTaxes = safeAdd(this.totalTaxes, other.totalTaxes);
        this.totalDeductions = safeAdd(this.totalDeductions, other.totalDeductions);
    }

    private BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.add(b);
    }

    /**
     * Validates that gross pay equals net pay + taxes + deductions.
     */
    public boolean isBalanced() {
        BigDecimal expected = safeAdd(totalNetPay, safeAdd(totalTaxes, totalDeductions));
        return totalGrossPay.compareTo(expected) == 0;
    }
}
