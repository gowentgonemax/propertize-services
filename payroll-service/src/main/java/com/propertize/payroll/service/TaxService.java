package com.propertize.payroll.service;

import com.propertize.commons.enums.employee.PayFrequencyEnum;
import com.propertize.commons.enums.payment.PaymentMethodEnum;

import com.propertize.payroll.entity.*;
import com.propertize.payroll.enums.FilingStatusEnum;
import com.propertize.payroll.enums.TaxTypeEnum;
import com.propertize.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for tax calculation and management.
 * Handles federal, state, and local tax calculations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxService {

    private final TaxWithholdingRepository taxWithholdingRepository;
    private final TaxCalculationRepository taxCalculationRepository;
    private final PayrollTaxDepositRepository payrollTaxDepositRepository;
    private final EmployeeEntityRepository employeeRepository;

    // 2026 Tax Constants - These should ideally come from a configuration or tax
    // table
    private static final BigDecimal SOCIAL_SECURITY_RATE = new BigDecimal("0.062");
    private static final BigDecimal SOCIAL_SECURITY_WAGE_BASE = new BigDecimal("176100"); // 2026 estimate
    private static final BigDecimal MEDICARE_RATE = new BigDecimal("0.0145");
    private static final BigDecimal MEDICARE_ADDITIONAL_RATE = new BigDecimal("0.009");
    private static final BigDecimal MEDICARE_ADDITIONAL_THRESHOLD = new BigDecimal("200000");
    private static final BigDecimal EMPLOYER_FUTA_RATE = new BigDecimal("0.006");
    private static final BigDecimal FUTA_WAGE_BASE = new BigDecimal("7000");

    // ==================== Tax Withholding Operations ====================

    /**
     * Get current active tax withholding for an employee
     */
    public Optional<TaxWithholdingEntity> getActiveWithholding(String employeeId) {
        log.info("Fetching active tax withholding for employee: {}", employeeId);
        return taxWithholdingRepository.findByEmployeeIdAndIsActiveTrue(employeeId);
    }

    /**
     * Get all tax withholdings for an employee (history)
     */
    public List<TaxWithholdingEntity> getWithholdingHistory(String employeeId) {
        return taxWithholdingRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId);
    }

    /**
     * Configure tax withholding for an employee (W-4 data)
     */
    @Transactional
    public TaxWithholdingEntity configureTaxWithholding(TaxWithholdingEntity withholding) {
        log.info("Configuring tax withholding for employee: {}", withholding.getEmployeeId());

        // Deactivate any existing active withholdings
        Optional<TaxWithholdingEntity> existing = getActiveWithholding(withholding.getEmployeeId());
        existing.ifPresent(e -> {
            e.setIsActive(false);
            e.setEndDate(withholding.getEffectiveDate().minusDays(1));
            taxWithholdingRepository.save(e);
        });

        // Activate new withholding
        withholding.setIsActive(true);
        return taxWithholdingRepository.save(withholding);
    }

    // ==================== Tax Calculation Operations ====================

    /**
     * Calculate all taxes for an employee's gross pay
     */
    public TaxCalculationResult calculateTaxes(TaxCalculationContext context) {
        log.info("Calculating taxes for employee: {}, gross pay: {}",
                context.getEmployeeId(), context.getGrossPay());

        TaxCalculationResult result = new TaxCalculationResult();

        // Get employee and their withholding info
        EmployeeEntity employee = employeeRepository.findById(UUID.fromString(context.getEmployeeId()))
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        Optional<TaxWithholdingEntity> withholdingOpt = getActiveWithholding(context.getEmployeeId());

        // Calculate Federal Income Tax
        BigDecimal federalTax = calculateFederalIncomeTax(context, withholdingOpt.orElse(null));
        result.setFederalIncomeTax(federalTax);

        // Calculate Social Security Tax
        BigDecimal socialSecurity = calculateSocialSecurityTax(context);
        result.setSocialSecurityTax(socialSecurity);

        // Calculate Medicare Tax
        BigDecimal medicare = calculateMedicareTax(context);
        result.setMedicareTax(medicare);

        // Calculate State Income Tax
        BigDecimal stateTax = calculateStateIncomeTax(context, withholdingOpt.orElse(null));
        result.setStateIncomeTax(stateTax);

        // Calculate employer taxes
        result.setEmployerSocialSecurity(socialSecurity); // Employer matches employee SS
        result.setEmployerMedicare(medicare); // Employer matches employee Medicare
        result.setEmployerFuta(calculateFutaTax(context));

        // Calculate totals
        BigDecimal totalEmployeeTax = federalTax.add(socialSecurity).add(medicare).add(stateTax);
        result.setTotalEmployeeTax(totalEmployeeTax);
        result.setNetPay(context.getGrossPay().subtract(totalEmployeeTax));

        log.info("Tax calculation complete. Employee total: {}, Net pay: {}",
                totalEmployeeTax, result.getNetPay());

        return result;
    }

    /**
     * Calculate Federal Income Tax using 2026 tax brackets
     */
    public BigDecimal calculateFederalIncomeTax(TaxCalculationContext context, TaxWithholdingEntity withholding) {
        BigDecimal annualizedGross = annualizePayment(context.getGrossPay(), context.getPayFrequency());

        // Apply standard deduction if applicable
        BigDecimal taxableIncome = annualizedGross;
        if (withholding != null && withholding.getTaxInfo() != null) {
            // Get standard deduction based on filing status
            FilingStatusEnum filingStatus = withholding.getTaxInfo().getFilingStatus() != null
                    ? withholding.getTaxInfo().getFilingStatus()
                    : FilingStatusEnum.SINGLE;
            BigDecimal standardDeduction = getStandardDeduction(filingStatus);
            taxableIncome = annualizedGross.subtract(standardDeduction);

            // Apply additional withholding adjustments
            if (withholding.getTaxInfo().getOtherIncome() != null) {
                taxableIncome = taxableIncome.add(withholding.getTaxInfo().getOtherIncome());
            }
            if (withholding.getTaxInfo().getDeductions() != null) {
                taxableIncome = taxableIncome.subtract(withholding.getTaxInfo().getDeductions());
            }
        }

        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Calculate tax using progressive brackets (simplified 2026 brackets)
        BigDecimal annualTax = calculateProgressiveTax(taxableIncome);

        // De-annualize to get per-pay-period tax
        BigDecimal periodTax = deAnnualizePayment(annualTax, context.getPayFrequency());

        // Add any additional withholding requested
        if (withholding != null && withholding.getTaxInfo() != null
                && withholding.getTaxInfo().getAdditionalWithholding() != null) {
            periodTax = periodTax.add(withholding.getTaxInfo().getAdditionalWithholding());
        }

        return periodTax.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Social Security Tax (6.2% up to wage base)
     */
    public BigDecimal calculateSocialSecurityTax(TaxCalculationContext context) {
        // Check if employee has already exceeded annual wage base
        BigDecimal ytdGross = context.getYtdGross() != null ? context.getYtdGross() : BigDecimal.ZERO;

        if (ytdGross.compareTo(SOCIAL_SECURITY_WAGE_BASE) >= 0) {
            // Already exceeded wage base, no more SS tax
            return BigDecimal.ZERO;
        }

        BigDecimal remainingWageBase = SOCIAL_SECURITY_WAGE_BASE.subtract(ytdGross);
        BigDecimal taxableAmount = context.getGrossPay().min(remainingWageBase);

        return taxableAmount.multiply(SOCIAL_SECURITY_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Medicare Tax (1.45% + 0.9% additional over threshold)
     */
    public BigDecimal calculateMedicareTax(TaxCalculationContext context) {
        BigDecimal baseMedicare = context.getGrossPay().multiply(MEDICARE_RATE);

        // Check for additional Medicare tax
        BigDecimal ytdGross = context.getYtdGross() != null ? context.getYtdGross() : BigDecimal.ZERO;
        BigDecimal totalGross = ytdGross.add(context.getGrossPay());

        if (totalGross.compareTo(MEDICARE_ADDITIONAL_THRESHOLD) > 0) {
            BigDecimal additionalTaxableAmount;
            if (ytdGross.compareTo(MEDICARE_ADDITIONAL_THRESHOLD) >= 0) {
                // All of current pay is subject to additional tax
                additionalTaxableAmount = context.getGrossPay();
            } else {
                // Only portion above threshold
                additionalTaxableAmount = totalGross.subtract(MEDICARE_ADDITIONAL_THRESHOLD);
            }
            BigDecimal additionalMedicare = additionalTaxableAmount.multiply(MEDICARE_ADDITIONAL_RATE);
            baseMedicare = baseMedicare.add(additionalMedicare);
        }

        return baseMedicare.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate State Income Tax (simplified - state-specific implementation
     * needed)
     */
    public BigDecimal calculateStateIncomeTax(TaxCalculationContext context, TaxWithholdingEntity withholding) {
        // This is a simplified placeholder
        // Real implementation needs state-specific tax tables
        String state = context.getWorkState();

        if (state == null || state.isBlank()) {
            return BigDecimal.ZERO;
        }

        // States with no income tax
        if (List.of("AK", "FL", "NV", "NH", "SD", "TN", "TX", "WA", "WY").contains(state)) {
            return BigDecimal.ZERO;
        }

        // Simplified flat rate for other states (5% as placeholder)
        // Real implementation should use state-specific brackets
        return context.getGrossPay().multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate FUTA Tax (employer only)
     */
    public BigDecimal calculateFutaTax(TaxCalculationContext context) {
        BigDecimal ytdGross = context.getYtdGross() != null ? context.getYtdGross() : BigDecimal.ZERO;

        if (ytdGross.compareTo(FUTA_WAGE_BASE) >= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal remainingWageBase = FUTA_WAGE_BASE.subtract(ytdGross);
        BigDecimal taxableAmount = context.getGrossPay().min(remainingWageBase);

        return taxableAmount.multiply(EMPLOYER_FUTA_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== Tax Deposit Operations ====================

    /**
     * Get upcoming tax deposits for a client
     */
    public List<PayrollTaxDepositEntity> getUpcomingTaxDeposits(UUID clientId, LocalDate fromDate, LocalDate toDate) {
        log.info("Fetching tax deposits for client: {} between {} and {}", clientId, fromDate, toDate);
        return payrollTaxDepositRepository.findByClientIdAndDueDateBetween(clientId, fromDate, toDate);
    }

    /**
     * Get overdue tax deposits
     */
    public List<PayrollTaxDepositEntity> getOverdueTaxDeposits(UUID clientId) {
        return payrollTaxDepositRepository.findByClientIdAndDueDateBeforeAndPaidDateIsNull(
                clientId, LocalDate.now());
    }

    /**
     * Record a tax deposit payment
     */
    @Transactional
    public PayrollTaxDepositEntity recordTaxDepositPayment(UUID depositId, LocalDate paidDate,
            String paymentReference, PaymentMethodEnum paymentMethod) {
        PayrollTaxDepositEntity deposit = payrollTaxDepositRepository.findById(depositId)
                .orElseThrow(() -> new EntityNotFoundException("Tax deposit not found"));

        log.info("Recording tax deposit payment: {}", depositId);

        deposit.setPaidDate(paidDate);
        deposit.setPaymentReference(paymentReference);
        deposit.setPaymentMethod(paymentMethod);

        return payrollTaxDepositRepository.save(deposit);
    }

    // ==================== Helper Methods ====================

    private BigDecimal annualizePayment(BigDecimal amount, PayFrequencyEnum payFrequency) {
        return switch (payFrequency != null ? payFrequency : PayFrequencyEnum.BI_WEEKLY) {
            case WEEKLY -> amount.multiply(new BigDecimal("52"));
            case BI_WEEKLY -> amount.multiply(new BigDecimal("26"));
            case SEMI_MONTHLY -> amount.multiply(new BigDecimal("24"));
            case MONTHLY -> amount.multiply(new BigDecimal("12"));
            default -> amount.multiply(new BigDecimal("26"));
        };
    }

    private BigDecimal deAnnualizePayment(BigDecimal amount, PayFrequencyEnum payFrequency) {
        return switch (payFrequency != null ? payFrequency : PayFrequencyEnum.BI_WEEKLY) {
            case WEEKLY -> amount.divide(new BigDecimal("52"), 2, RoundingMode.HALF_UP);
            case BI_WEEKLY -> amount.divide(new BigDecimal("26"), 2, RoundingMode.HALF_UP);
            case SEMI_MONTHLY -> amount.divide(new BigDecimal("24"), 2, RoundingMode.HALF_UP);
            case MONTHLY -> amount.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            default -> amount.divide(new BigDecimal("26"), 2, RoundingMode.HALF_UP);
        };
    }

    private BigDecimal getStandardDeduction(FilingStatusEnum filingStatus) {
        // 2026 Standard Deductions (estimated)
        return switch (filingStatus) {
            case MARRIED -> new BigDecimal("30000");
            case MARRIED_SEPARATE -> new BigDecimal("15000");
            case HEAD_OF_HOUSEHOLD -> new BigDecimal("22500");
            default -> new BigDecimal("15000"); // Single or default
        };
    }

    private BigDecimal calculateProgressiveTax(BigDecimal taxableIncome) {
        // 2026 Federal Tax Brackets (Single - estimated)
        // Real implementation should consider filing status
        BigDecimal tax = BigDecimal.ZERO;

        BigDecimal[] brackets = {
                new BigDecimal("11600"),
                new BigDecimal("47150"),
                new BigDecimal("100525"),
                new BigDecimal("191950"),
                new BigDecimal("243725"),
                new BigDecimal("609350")
        };

        BigDecimal[] rates = {
                new BigDecimal("0.10"),
                new BigDecimal("0.12"),
                new BigDecimal("0.22"),
                new BigDecimal("0.24"),
                new BigDecimal("0.32"),
                new BigDecimal("0.35"),
                new BigDecimal("0.37")
        };

        BigDecimal remaining = taxableIncome;
        BigDecimal previousBracket = BigDecimal.ZERO;

        for (int i = 0; i < brackets.length && remaining.compareTo(BigDecimal.ZERO) > 0; i++) {
            BigDecimal bracketAmount = brackets[i].subtract(previousBracket);
            BigDecimal taxableInBracket = remaining.min(bracketAmount);
            tax = tax.add(taxableInBracket.multiply(rates[i]));
            remaining = remaining.subtract(taxableInBracket);
            previousBracket = brackets[i];
        }

        // Remaining income above last bracket
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            tax = tax.add(remaining.multiply(rates[rates.length - 1]));
        }

        return tax;
    }

    // ==================== Inner Classes ====================

    /**
     * Context object for tax calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaxCalculationContext {
        private String employeeId;
        private BigDecimal grossPay;
        private BigDecimal ytdGross;
        private PayFrequencyEnum payFrequency;
        private String workState;
        private String workCity;
        private LocalDate payDate;
    }

    /**
     * Result object for tax calculations
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaxCalculationResult {
        private BigDecimal federalIncomeTax;
        private BigDecimal socialSecurityTax;
        private BigDecimal medicareTax;
        private BigDecimal stateIncomeTax;
        private BigDecimal localIncomeTax;
        private BigDecimal totalEmployeeTax;
        private BigDecimal netPay;

        // Employer taxes
        private BigDecimal employerSocialSecurity;
        private BigDecimal employerMedicare;
        private BigDecimal employerFuta;
        private BigDecimal employerSuta;
    }
}
