package com.propertize.payroll.calculation;

import java.math.BigDecimal;

/**
 * Java 21 record — immutable, zero-boilerplate DTO for payroll calculation
 * context.
 *
 * Records automatically generate: constructor, getters (component name),
 * equals(), hashCode(), toString(). Perfect for Strategy method signatures
 * and pattern matching.
 *
 * Usage in switch (Java 21 pattern matching):
 * 
 * <pre>{@code
 * switch (employmentType) {
 *     case "FULL_TIME" -> new FederalTaxStrategy();
 *     case "CONTRACTOR" -> new ContractorWithholdingStrategy();
 *     default -> DeductionStrategy.NONE;
 * }
 * }</pre>
 */
public record PayrollContext(
        Long employeeId,
        String employmentType, // FULL_TIME, PART_TIME, CONTRACTOR
        BigDecimal grossPay,
        BigDecimal ytdEarnings,
        int regularHours,
        int overtimeHours,
        String taxState,
        boolean hasHealthBenefits,
        boolean has401k,
        BigDecimal contribution401kRate // e.g. 0.05 for 5%
) {
    /** Compact constructor — validate invariants. */
    public PayrollContext {
        if (grossPay == null || grossPay.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("grossPay must be ≥ 0");
        if (ytdEarnings == null || ytdEarnings.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("ytdEarnings must be ≥ 0");
    }
}
