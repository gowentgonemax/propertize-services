package com.propertize.payroll.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete Strategy: court-ordered garnishment deduction.
 * <p>
 * Current implementation applies no garnishment (no context field yet).
 * When garnishment data is added to {@link PayrollContext}, this strategy
 * will cap at 25% of disposable earnings per federal law (15 USC §1673).
 * </p>
 */
@Component
public class GarnishmentStrategy implements DeductionStrategy {

    /** Federal maximum: 25% of disposable earnings. */
    private static final BigDecimal MAX_GARNISHMENT_RATE = new BigDecimal("0.25");

    @Override
    public String name() {
        return "Garnishment";
    }

    @Override
    public BigDecimal calculate(PayrollContext context) {
        // TODO: add garnishmentAmount / garnishmentPercent fields to PayrollContext
        // For now, return zero — the strategy is registered so it auto-activates
        // when context is enriched.
        return BigDecimal.ZERO;
    }
}

