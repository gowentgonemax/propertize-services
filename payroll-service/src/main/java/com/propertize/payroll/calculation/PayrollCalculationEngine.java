package com.propertize.payroll.calculation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory / Orchestrator (GoF Factory Method + Strategy).
 *
 * <p>
 * Spring auto-discovers every {@link DeductionStrategy} {@code @Component}
 * and injects them as an ordered list. Adding a new deduction type only
 * requires creating a new {@code @Component} that implements
 * {@link DeductionStrategy} — no changes here.
 * </p>
 *
 * <p>
 * Uses Java 21 virtual threads (configured via {@code VirtualThreadConfig})
 * for I/O-bound upstream calls that may be introduced by future strategies.
 * Calculation itself is CPU-bound and runs on the calling thread.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollCalculationEngine {

    /**
     * All {@link DeductionStrategy} beans registered in the application context.
     */
    private final List<DeductionStrategy> strategies;

    /**
     * Applies all deduction strategies in registration order and returns an
     * immutable {@link PayrollResult}.
     *
     * @param context payroll inputs (validated by the record compact constructor)
     * @return breakdown of gross pay, total deductions, and net pay
     */
    public PayrollResult calculate(PayrollContext context) {
        log.debug("Calculating payroll for employee={} grossPay={}",
                context.employeeId(), context.grossPay());

        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();

        BigDecimal totalDeductions = strategies.stream()
                .map(strategy -> {
                    BigDecimal amount = strategy.calculate(context)
                            .setScale(2, RoundingMode.HALF_UP);
                    breakdown.put(strategy.name(), amount);
                    log.trace("Strategy '{}' → {}", strategy.name(), amount);
                    return amount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netPay = context.grossPay()
                .subtract(totalDeductions)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Payroll complete employee={} gross={} deductions={} net={}",
                context.employeeId(), context.grossPay(), totalDeductions, netPay);

        return new PayrollResult(context.grossPay(), totalDeductions, netPay, breakdown);
    }
}
