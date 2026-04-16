package com.propertize.payroll.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete Strategy: simplified state income tax withholding.
 * Uses a flat rate lookup by state. Replace with real state tax tables for production.
 */
@Component
public class StateTaxStrategy implements DeductionStrategy {

    @Override
    public String name() {
        return "StateIncomeTax";
    }

    @Override
    public BigDecimal calculate(PayrollContext context) {
        if (context.taxState() == null || context.taxState().isBlank()) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = switch (context.taxState().toUpperCase()) {
            case "CA" -> new BigDecimal("0.0725");
            case "NY" -> new BigDecimal("0.0685");
            case "TX", "FL", "NV", "WA", "WY", "SD", "AK", "NH", "TN" -> BigDecimal.ZERO;
            case "NJ" -> new BigDecimal("0.0637");
            case "IL" -> new BigDecimal("0.0495");
            case "PA" -> new BigDecimal("0.0307");
            default -> new BigDecimal("0.05"); // default ~5%
        };

        return context.grossPay().multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}

