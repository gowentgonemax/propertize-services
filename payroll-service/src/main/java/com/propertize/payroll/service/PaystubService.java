package com.propertize.payroll.service;

import com.propertize.payroll.entity.*;
import com.propertize.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing paystubs and generating pay statements.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaystubService {

    private final PaystubRepository paystubRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final EmployeeEntityRepository employeeRepository;
    private final TaxService taxService;
    private final BenefitService benefitService;
    private final DeductionRepository deductionRepository;

    // ==================== Paystub Retrieval ====================

    /**
     * Get a paystub by ID
     */
    public Paystub getPaystubById(UUID id) {
        return paystubRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Paystub not found with id: " + id));
    }

    /**
     * Get paystubs for an employee
     */
    public Page<Paystub> getEmployeePaystubs(String employeeId, Pageable pageable) {
        log.info("Fetching paystubs for employee: {}", employeeId);
        return paystubRepository.findByEmployeeIdOrderByPayDateDesc(employeeId, pageable);
    }

    /**
     * Get paystubs for a payroll run
     */
    public List<Paystub> getPaystubsByPayrollRun(UUID payrollRunId) {
        log.info("Fetching paystubs for payroll run: {}", payrollRunId);
        return paystubRepository.findByPayrollRunId(payrollRunId);
    }

    /**
     * Get paystub for specific employee and payroll run
     */
    public Optional<Paystub> getPaystubForEmployeeAndRun(String employeeId, UUID payrollRunId) {
        return paystubRepository.findByEmployeeIdAndPayrollRunId(employeeId, payrollRunId);
    }

    /**
     * Get paystubs for a date range
     */
    public List<Paystub> getEmployeePaystubsForDateRange(String employeeId, LocalDate startDate, LocalDate endDate) {
        return paystubRepository.findByEmployeeIdAndPayDateBetween(employeeId, startDate, endDate);
    }

    // ==================== Paystub Generation ====================

    /**
     * Generate all paystubs for a payroll run
     */
    @Transactional
    public List<Paystub> generatePaystubsForPayrollRun(UUID payrollRunId) {
        log.info("Generating paystubs for payroll run: {}", payrollRunId);

        PayrollRun payrollRun = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new EntityNotFoundException("Payroll run not found"));

        // Get all active employees for the client
        List<EmployeeEntity> employees = employeeRepository.findByClientIdAndStatus(
                payrollRun.getClient().getId(),
                com.propertize.payroll.enums.EmployeeStatusEnum.ACTIVE);

        List<Paystub> paystubs = new ArrayList<>();

        for (EmployeeEntity employee : employees) {
            try {
                Paystub paystub = generatePaystub(payrollRun, employee);
                paystubs.add(paystub);
            } catch (Exception e) {
                log.error("Failed to generate paystub for employee: {} - Error: {}",
                        employee.getId(), e.getMessage());
            }
        }

        log.info("Generated {} paystubs for payroll run: {}", paystubs.size(), payrollRunId);
        return paystubs;
    }

    /**
     * Generate a single paystub
     */
    @Transactional
    public Paystub generatePaystub(PayrollRun payrollRun, EmployeeEntity employee) {
        log.info("Generating paystub for employee: {} in payroll run: {}",
                employee.getId(), payrollRun.getId());

        // Check if paystub already exists
        Optional<Paystub> existingPaystub = getPaystubForEmployeeAndRun(
                employee.getId().toString(), payrollRun.getId());

        if (existingPaystub.isPresent()) {
            log.warn("Paystub already exists for employee: {} in payroll run: {}",
                    employee.getId(), payrollRun.getId());
            return existingPaystub.get();
        }

        Paystub paystub = new Paystub();
        paystub.setPayrollRun(payrollRun);
        paystub.setEmployee(employee);
        paystub.setPayDate(payrollRun.getPayDate());
        paystub.setPayPeriodStart(payrollRun.getPayPeriod().getStartDate());
        paystub.setPayPeriodEnd(payrollRun.getPayPeriod().getEndDate());

        // Calculate earnings
        calculateEarnings(paystub, employee, payrollRun);

        // Calculate taxes
        calculateTaxes(paystub, employee);

        // Calculate deductions
        calculateDeductions(paystub, employee);

        // Calculate totals
        calculateTotals(paystub);

        // Calculate YTD totals
        calculateYtdTotals(paystub, employee);

        // Generate check/direct deposit number
        paystub.setCheckNumber(generateCheckNumber(payrollRun, employee));

        return paystubRepository.save(paystub);
    }

    /**
     * Regenerate a paystub (e.g., after correction)
     */
    @Transactional
    public Paystub regeneratePaystub(UUID paystubId) {
        Paystub existingPaystub = getPaystubById(paystubId);
        log.info("Regenerating paystub: {}", paystubId);

        // Clear existing calculations
        existingPaystub.getEarnings().clear();
        existingPaystub.getDeductions().clear();
        existingPaystub.getTaxes().clear();

        // Recalculate
        calculateEarnings(existingPaystub, existingPaystub.getEmployee(), existingPaystub.getPayrollRun());
        calculateTaxes(existingPaystub, existingPaystub.getEmployee());
        calculateDeductions(existingPaystub, existingPaystub.getEmployee());
        calculateTotals(existingPaystub);
        calculateYtdTotals(existingPaystub, existingPaystub.getEmployee());

        return paystubRepository.save(existingPaystub);
    }

    // ==================== YTD Summary ====================

    /**
     * Get YTD summary for an employee
     */
    public YtdSummary getYtdSummary(String employeeId, Integer year) {
        log.info("Calculating YTD summary for employee: {} year: {}", employeeId, year);

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        List<Paystub> paystubs = getEmployeePaystubsForDateRange(employeeId, startOfYear, endOfYear);

        YtdSummary summary = new YtdSummary();
        summary.setEmployeeId(employeeId);
        summary.setYear(year);
        summary.setPaystubCount(paystubs.size());

        BigDecimal grossEarnings = BigDecimal.ZERO;
        BigDecimal netPay = BigDecimal.ZERO;
        BigDecimal totalTaxes = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal federalTax = BigDecimal.ZERO;
        BigDecimal stateTax = BigDecimal.ZERO;
        BigDecimal socialSecurity = BigDecimal.ZERO;
        BigDecimal medicare = BigDecimal.ZERO;

        for (Paystub paystub : paystubs) {
            grossEarnings = grossEarnings.add(safeGet(paystub.getGrossEarnings()));
            netPay = netPay.add(safeGet(paystub.getNetPay()));
            totalTaxes = totalTaxes.add(safeGet(paystub.getTotalTaxes()));
            totalDeductions = totalDeductions.add(safeGet(paystub.getTotalDeductions()));
            federalTax = federalTax.add(safeGet(paystub.getFederalTax()));
            stateTax = stateTax.add(safeGet(paystub.getStateTax()));
            socialSecurity = socialSecurity.add(safeGet(paystub.getSocialSecurityTax()));
            medicare = medicare.add(safeGet(paystub.getMedicareTax()));
        }

        summary.setGrossEarnings(grossEarnings);
        summary.setNetPay(netPay);
        summary.setTotalTaxes(totalTaxes);
        summary.setTotalDeductions(totalDeductions);
        summary.setFederalTax(federalTax);
        summary.setStateTax(stateTax);
        summary.setSocialSecurityTax(socialSecurity);
        summary.setMedicareTax(medicare);

        return summary;
    }

    /**
     * Get YTD summary for an employee, optionally excluding one paystub by ID.
     */
    public YtdSummary getYtdSummary(String employeeId, Integer year, UUID excludeId) {
        if (excludeId == null) {
            return getYtdSummary(employeeId, year);
        }
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        List<Paystub> paystubs = paystubRepository.findByEmployeeIdAndPayDateBetweenExcluding(
                employeeId, startOfYear, endOfYear, excludeId);
        YtdSummary summary = new YtdSummary();
        summary.setEmployeeId(employeeId);
        summary.setYear(year);
        summary.setPaystubCount(paystubs.size());
        paystubs.forEach(p -> {
            summary.setGrossEarnings(summary.getGrossEarnings() == null ? p.getGrossPay()
                    : summary.getGrossEarnings().add(p.getGrossPay() == null ? BigDecimal.ZERO : p.getGrossPay()));
            summary.setNetPay(summary.getNetPay() == null ? p.getNetPay()
                    : summary.getNetPay().add(p.getNetPay() == null ? BigDecimal.ZERO : p.getNetPay()));
        });
        return summary;
    }

    // ==================== PDF Generation ====================

    /**
     * Generate PDF for a paystub
     * Note: Actual PDF generation requires additional library (iText, PDFBox, etc.)
     */
    public byte[] generatePaystubPdf(UUID paystubId) {
        Paystub paystub = getPaystubById(paystubId);
        log.info("Generating PDF for paystub: {}", paystubId);

        // This is a placeholder - actual implementation would use a PDF library
        // like iText, Apache PDFBox, or JasperReports

        StringBuilder content = new StringBuilder();
        content.append("PAYSTUB\n");
        content.append("===============================\n\n");
        content.append("Employee: ").append(paystub.getEmployee().getFullName()).append("\n");
        content.append("Pay Period: ").append(paystub.getPayPeriodStart())
                .append(" - ").append(paystub.getPayPeriodEnd()).append("\n");
        content.append("Pay Date: ").append(paystub.getPayDate()).append("\n\n");

        content.append("EARNINGS\n");
        content.append("-------------------------------\n");
        content.append("Gross Earnings: $").append(paystub.getGrossEarnings()).append("\n\n");

        content.append("TAXES\n");
        content.append("-------------------------------\n");
        content.append("Federal Tax: $").append(paystub.getFederalTax()).append("\n");
        content.append("State Tax: $").append(paystub.getStateTax()).append("\n");
        content.append("Social Security: $").append(paystub.getSocialSecurityTax()).append("\n");
        content.append("Medicare: $").append(paystub.getMedicareTax()).append("\n");
        content.append("Total Taxes: $").append(paystub.getTotalTaxes()).append("\n\n");

        content.append("DEDUCTIONS\n");
        content.append("-------------------------------\n");
        content.append("Total Deductions: $").append(paystub.getTotalDeductions()).append("\n\n");

        content.append("===============================\n");
        content.append("NET PAY: $").append(paystub.getNetPay()).append("\n");

        // Return as bytes (in real implementation, would be actual PDF bytes)
        return content.toString().getBytes();
    }

    // ==================== Private Helper Methods ====================

    private void calculateEarnings(Paystub paystub, EmployeeEntity employee, PayrollRun payrollRun) {
        BigDecimal regularHours = new BigDecimal("80"); // Default bi-weekly
        BigDecimal overtimeHours = BigDecimal.ZERO;

        // TODO: Get actual hours from timesheets

        BigDecimal hourlyRate = employee.getEffectivePayRate();

        // Regular earnings
        PaystubEarning regularEarning = new PaystubEarning();
        regularEarning.setPaystub(paystub);
        regularEarning.setEarningType(com.propertize.payroll.enums.EarningTypeEnum.REGULAR);
        regularEarning.setDescription("Regular Pay");
        regularEarning.setHours(regularHours);
        regularEarning.setRate(hourlyRate);
        regularEarning.setAmount(regularHours.multiply(hourlyRate));
        paystub.getEarnings().add(regularEarning);

        // Overtime earnings
        if (overtimeHours.compareTo(BigDecimal.ZERO) > 0 && employee.getOvertimeEligible()) {
            BigDecimal overtimeRate = hourlyRate.multiply(employee.getOvertimeMultiplier());
            PaystubEarning overtimeEarning = new PaystubEarning();
            overtimeEarning.setPaystub(paystub);
            overtimeEarning.setEarningType(com.propertize.payroll.enums.EarningTypeEnum.OVERTIME);
            overtimeEarning.setDescription("Overtime Pay");
            overtimeEarning.setHours(overtimeHours);
            overtimeEarning.setRate(overtimeRate);
            overtimeEarning.setAmount(overtimeHours.multiply(overtimeRate));
            paystub.getEarnings().add(overtimeEarning);
        }

        // Calculate gross earnings
        BigDecimal grossEarnings = paystub.getEarnings().stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        paystub.setGrossEarnings(grossEarnings);
    }

    private void calculateTaxes(Paystub paystub, EmployeeEntity employee) {
        // Get YTD gross for tax calculations
        YtdSummary ytdSummary = getYtdSummary(employee.getId().toString(), paystub.getPayDate().getYear());

        TaxService.TaxCalculationContext context = TaxService.TaxCalculationContext.builder()
                .employeeId(employee.getId().toString())
                .grossPay(paystub.getGrossEarnings())
                .ytdGross(ytdSummary.getGrossEarnings())
                .payFrequency(employee.getPayFrequency() != null ? employee.getPayFrequency().name() : "SEMI_MONTHLY")
                .workState(employee.getHomeAddress() != null ? employee.getHomeAddress().getState() : null)
                .payDate(paystub.getPayDate())
                .build();

        TaxService.TaxCalculationResult taxResult = taxService.calculateTaxes(context);

        // Federal Tax
        PaystubTax federalTax = new PaystubTax();
        federalTax.setPaystub(paystub);
        federalTax.setTaxType(com.propertize.payroll.enums.TaxTypeEnum.FEDERAL);
        federalTax.setName("Federal Income Tax");
        federalTax.setAmount(taxResult.getFederalIncomeTax());
        paystub.getTaxes().add(federalTax);
        paystub.setFederalTax(taxResult.getFederalIncomeTax());

        // State Tax
        PaystubTax stateTax = new PaystubTax();
        stateTax.setPaystub(paystub);
        stateTax.setTaxType(com.propertize.payroll.enums.TaxTypeEnum.STATE);
        stateTax.setName("State Income Tax");
        stateTax.setAmount(taxResult.getStateIncomeTax());
        paystub.getTaxes().add(stateTax);
        paystub.setStateTax(taxResult.getStateIncomeTax());

        // Social Security
        PaystubTax ssTax = new PaystubTax();
        ssTax.setPaystub(paystub);
        ssTax.setTaxType(com.propertize.payroll.enums.TaxTypeEnum.SOCIAL_SECURITY);
        ssTax.setName("Social Security");
        ssTax.setAmount(taxResult.getSocialSecurityTax());
        paystub.getTaxes().add(ssTax);
        paystub.setSocialSecurityTax(taxResult.getSocialSecurityTax());

        // Medicare
        PaystubTax medicareTax = new PaystubTax();
        medicareTax.setPaystub(paystub);
        medicareTax.setTaxType(com.propertize.payroll.enums.TaxTypeEnum.MEDICARE);
        medicareTax.setName("Medicare");
        medicareTax.setAmount(taxResult.getMedicareTax());
        paystub.getTaxes().add(medicareTax);
        paystub.setMedicareTax(taxResult.getMedicareTax());

        // Total taxes
        paystub.setTotalTaxes(taxResult.getTotalEmployeeTax());
    }

    private void calculateDeductions(Paystub paystub, EmployeeEntity employee) {
        String employeeId = employee.getId().toString();

        // Get active deductions for employee
        List<Deduction> deductions = deductionRepository.findByEmployeeIdAndStatus(
                employee.getId(), com.propertize.payroll.enums.DeductionStatusEnum.ACTIVE);

        BigDecimal totalDeductions = BigDecimal.ZERO;

        for (Deduction deduction : deductions) {
            PaystubDeduction paystubDeduction = new PaystubDeduction();
            paystubDeduction.setPaystub(paystub);
            paystubDeduction.setDeductionType(com.propertize.payroll.enums.DeductionTypeEnum.OTHER);
            paystubDeduction.setName(deduction.getName());
            paystubDeduction.setDescription(deduction.getName());

            BigDecimal amount = calculateDeductionAmount(deduction, paystub.getGrossEarnings());
            paystubDeduction.setAmount(amount);

            paystub.getDeductions().add(paystubDeduction);
            totalDeductions = totalDeductions.add(amount);
        }

        // Add benefit deductions
        BigDecimal benefitDeductions = benefitService.calculateTotalEmployeeDeductions(employeeId);
        if (benefitDeductions.compareTo(BigDecimal.ZERO) > 0) {
            PaystubDeduction benefitDeduction = new PaystubDeduction();
            benefitDeduction.setPaystub(paystub);
            benefitDeduction.setDeductionType(com.propertize.payroll.enums.DeductionTypeEnum.BENEFITS);
            benefitDeduction.setName("Employee Benefits");
            benefitDeduction.setDescription("Employee Benefits");
            benefitDeduction.setAmount(benefitDeductions);
            paystub.getDeductions().add(benefitDeduction);
            totalDeductions = totalDeductions.add(benefitDeductions);
        }

        paystub.setTotalDeductions(totalDeductions);
    }

    private BigDecimal calculateDeductionAmount(Deduction deduction, BigDecimal grossPay) {
        if (deduction.getMethod() == com.propertize.payroll.enums.DeductionMethodEnum.PERCENTAGE) {
            return grossPay.multiply(deduction.getAmount().divide(new BigDecimal("100"), 4,
                    java.math.RoundingMode.HALF_UP));
        }
        return deduction.getAmount() != null ? deduction.getAmount() : BigDecimal.ZERO;
    }

    private void calculateTotals(Paystub paystub) {
        BigDecimal grossEarnings = safeGet(paystub.getGrossEarnings());
        BigDecimal totalTaxes = safeGet(paystub.getTotalTaxes());
        BigDecimal totalDeductions = safeGet(paystub.getTotalDeductions());

        BigDecimal netPay = grossEarnings.subtract(totalTaxes).subtract(totalDeductions);
        paystub.setNetPay(netPay);
    }

    private void calculateYtdTotals(Paystub paystub, EmployeeEntity employee) {
        YtdSummary ytdSummary = getYtdSummary(employee.getId().toString(), paystub.getPayDate().getYear());

        paystub.setYtdGrossEarnings(ytdSummary.getGrossEarnings().add(paystub.getGrossEarnings()));
        paystub.setYtdNetPay(ytdSummary.getNetPay().add(paystub.getNetPay()));
        paystub.setYtdTaxes(ytdSummary.getTotalTaxes().add(paystub.getTotalTaxes()));
        paystub.setYtdDeductions(ytdSummary.getTotalDeductions().add(paystub.getTotalDeductions()));
        paystub.setYtdFederalTax(ytdSummary.getFederalTax().add(safeGet(paystub.getFederalTax())));
        paystub.setYtdStateTax(ytdSummary.getStateTax().add(safeGet(paystub.getStateTax())));
        paystub.setYtdSocialSecurityTax(ytdSummary.getSocialSecurityTax().add(safeGet(paystub.getSocialSecurityTax())));
        paystub.setYtdMedicareTax(ytdSummary.getMedicareTax().add(safeGet(paystub.getMedicareTax())));
    }

    private String generateCheckNumber(PayrollRun payrollRun, EmployeeEntity employee) {
        String runIdPrefix = payrollRun.getId() != null
                ? payrollRun.getId().toString().substring(0, Math.min(8, payrollRun.getId().toString().length()))
                        .toUpperCase()
                : "UNKNOWN";
        return String.format("CHK-%s-%s-%d",
                runIdPrefix,
                employee.getEmployeeNumber(),
                System.currentTimeMillis() % 10000);
    }

    private BigDecimal safeGet(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ==================== Inner Classes ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class YtdSummary {
        private String employeeId;
        private Integer year;
        private Integer paystubCount;
        private BigDecimal grossEarnings;
        private BigDecimal netPay;
        private BigDecimal totalTaxes;
        private BigDecimal totalDeductions;
        private BigDecimal federalTax;
        private BigDecimal stateTax;
        private BigDecimal socialSecurityTax;
        private BigDecimal medicareTax;
    }
}
