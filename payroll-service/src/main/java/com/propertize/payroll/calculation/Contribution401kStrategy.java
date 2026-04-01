package com.propertize.payroll.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Concrete Strategy: 401(k) employee contribution deduction. */
@Component
public class Contribution401kStrategy implements DeductionStrategy {

    /** 2025 IRS annual 401(k) contribution limit. */
    private static final BigDecimal ANNUAL_LIMIT = new BigDecimal("23000");

    @Override
    public String name() {
        return "401kContribution";
    }

    @Override
    public BigDecimal calculate(PayrollContext context) {
        if (!context.has401k())
            return BigDecimal.ZERO;

        BigDecimal contribution = context.grossPay()
                .multiply(context.contribution401kRate())
                .setScale(2, RoundingMode.HALF_UP);

        // Cap at remaining annual limit (annualized ytd already contributed
        // approximation)
        BigDecimal remainingLimit = ANNUAL_LIMIT.subtract(context.ytdEarnings()
                .multiply(context.contribution401kRate()));
        if (remainingLimit.compareTo(BigDecimal.ZERO) <= 0)
            return BigDecimal.ZERO;

        return contribution.min(remainingLimit);
    }
}
