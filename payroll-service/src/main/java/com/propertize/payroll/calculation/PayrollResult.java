package com.propertize.payroll.calculation;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Immutable result of a single payroll calculation run.
 * Java 21 record — value semantics, serialisation-friendly.
 */
public record PayrollResult(
        BigDecimal grossPay,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        Map<String, BigDecimal> deductionBreakdown) {
    /** Canonical constructor — defensive copy of breakdown map. */
    public PayrollResult {
        deductionBreakdown = Map.copyOf(deductionBreakdown);
    }
}
