package com.propertize.payroll.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete Strategy: health/dental/vision benefits pre-tax deduction.
 * Assumes a fixed bi-weekly benefits cost if employee is enrolled.
 */
@Component
public class BenefitsDeductionStrategy implements DeductionStrategy {

    /** Default bi-weekly benefits cost (health + dental + vision). */
    private static final BigDecimal BI_WEEKLY_BENEFITS = new BigDecimal("185.00");

    @Override
    public String name() {
        return "HealthBenefits";
    }

    @Override
    public BigDecimal calculate(PayrollContext context) {
        if (!context.hasHealthBenefits()) {
            return BigDecimal.ZERO;
        }
        // Cap at 15% of gross pay to prevent over-deduction for low earners
        BigDecimal maxDeduction = context.grossPay()
                .multiply(new BigDecimal("0.15"))
                .setScale(2, RoundingMode.HALF_UP);

        return BI_WEEKLY_BENEFITS.min(maxDeduction);
    }
}

