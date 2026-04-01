package com.propertize.payroll.calculation;

import java.math.BigDecimal;

/**
 * Strategy pattern — defines the contract for a payroll deduction calculation.
 *
 * Each deduction type (federal tax, state tax, benefits, 401k, garnishment, …)
 * is a separate Strategy. The PayrollCalculationEngine holds a list of active
 * strategies and applies them in sequence, making it trivially easy to add or
 * remove deduction types without touching the core engine.
 *
 * Java 21: this interface uses a record as the context parameter, giving us
 * immutable, self-documenting method signatures with zero boilerplate.
 */
@FunctionalInterface
public interface DeductionStrategy {

    /**
     * Calculate the deduction amount for a given payroll context.
     *
     * @param context immutable snapshot of the employee's payroll data
     * @return the deduction amount (must be ≥ 0); return BigDecimal.ZERO to skip
     */
    BigDecimal calculate(PayrollContext context);

    /** Human-readable name for logging and audit trails. */
    default String name() {
        return getClass().getSimpleName();
    }
}
