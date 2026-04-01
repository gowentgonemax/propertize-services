package com.propertize.payroll.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete Strategy: federal income tax withholding (simplified bracket model).
 *
 * Replace with full IRS tax table lookup for production. This example shows
 * the structural pattern.
 */
@Component
public class FederalTaxStrategy implements DeductionStrategy {

    // Simplified 2025 single-filer brackets (annualized)
    private static final BigDecimal RATE_10 = new BigDecimal("0.10");
    private static final BigDecimal RATE_22 = new BigDecimal("0.22");
    private static final BigDecimal RATE_24 = new BigDecimal("0.24");
    private static final BigDecimal BRACKET_12 = new BigDecimal("11600");
    private static final BigDecimal BRACKET_22 = new BigDecimal("47150");
    private static final BigDecimal BRACKET_24 = new BigDecimal("100525");

    @Override
    public String name() {
        return "FederalIncomeTax";
    }

    @Override
    public BigDecimal calculate(PayrollContext context) {
        if ("CONTRACTOR".equals(context.employmentType())) {
            return BigDecimal.ZERO; // contractors self-withhold
        }

        // Annualize the paycheck gross (assumes bi-weekly, 26 periods)
        BigDecimal annualized = context.grossPay().multiply(BigDecimal.valueOf(26));

        BigDecimal tax = switch (annualized.compareTo(BRACKET_22)) {
            case -1 -> annualized.compareTo(BRACKET_12) < 0
                    ? annualized.multiply(RATE_10)
                    : BRACKET_12.multiply(RATE_10)
                            .add(annualized.subtract(BRACKET_12).multiply(RATE_22));
            default -> BRACKET_12.multiply(RATE_10)
                    .add(BRACKET_22.subtract(BRACKET_12).multiply(RATE_22))
                    .add(annualized.subtract(BRACKET_22).multiply(RATE_24));
        };

        // De-annualize back to one pay period
        return tax.divide(BigDecimal.valueOf(26), 2, RoundingMode.HALF_UP);
    }
}
