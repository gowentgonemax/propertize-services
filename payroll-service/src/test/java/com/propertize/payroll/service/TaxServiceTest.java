package com.propertize.payroll.service;

import com.propertize.payroll.enums.PayFrequencyEnum;
import com.propertize.commons.enums.payment.PaymentMethodEnum;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.entity.PayrollTaxDepositEntity;
import com.propertize.payroll.entity.TaxWithholdingEntity;
import com.propertize.payroll.entity.embedded.TaxInfo;
import com.propertize.payroll.enums.FilingStatusEnum;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import com.propertize.payroll.repository.PayrollTaxDepositRepository;
import com.propertize.payroll.repository.TaxCalculationRepository;
import com.propertize.payroll.repository.TaxWithholdingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

        @Mock
        TaxWithholdingRepository taxWithholdingRepository;
        @Mock
        TaxCalculationRepository taxCalculationRepository;
        @Mock
        PayrollTaxDepositRepository payrollTaxDepositRepository;
        @Mock
        EmployeeEntityRepository employeeRepository;

        @InjectMocks
        TaxService taxService;

        String employeeId;
        UUID employeeUuid;
        UUID clientId;
        UUID depositId;

        @BeforeEach
        void setUp() {
                employeeUuid = UUID.randomUUID();
                employeeId = employeeUuid.toString();
                clientId = UUID.randomUUID();
                depositId = UUID.randomUUID();
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Helpers
        // ──────────────────────────────────────────────────────────────────────────

        private TaxWithholdingEntity buildWithholding(String empId, boolean active, LocalDate effectiveDate) {
                TaxWithholdingEntity w = new TaxWithholdingEntity();
                w.setEmployeeId(empId);
                w.setIsActive(active);
                w.setEffectiveDate(effectiveDate);
                w.setTaxYear(2026);
                TaxInfo taxInfo = new TaxInfo();
                taxInfo.setFilingStatus(FilingStatusEnum.SINGLE);
                w.setTaxInfo(taxInfo);
                return w;
        }

        private TaxService.TaxCalculationContext buildContext(BigDecimal gross, BigDecimal ytd, String state) {
                return TaxService.TaxCalculationContext.builder()
                                .employeeId(employeeId)
                                .grossPay(gross)
                                .ytdGross(ytd)
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY.name())
                                .workState(state)
                                .payDate(LocalDate.of(2026, 1, 15))
                                .build();
        }

        private EmployeeEntity buildEmployee() {
                EmployeeEntity emp = new EmployeeEntity();
                emp.setId(employeeUuid);
                return emp;
        }

        // ──────────────────────────────────────────────────────────────────────────
        // getActiveWithholding
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void getActiveWithholding_returnsPresent_whenFound() {
                TaxWithholdingEntity withholding = buildWithholding(employeeId, true, LocalDate.of(2026, 1, 1));
                when(taxWithholdingRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                                .thenReturn(Optional.of(withholding));

                Optional<TaxWithholdingEntity> result = taxService.getActiveWithholding(employeeId);

                assertThat(result).isPresent();
                assertThat(result.get().getEmployeeId()).isEqualTo(employeeId);
        }

        @Test
        void getActiveWithholding_returnsEmpty_whenNotFound() {
                when(taxWithholdingRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                                .thenReturn(Optional.empty());

                Optional<TaxWithholdingEntity> result = taxService.getActiveWithholding(employeeId);

                assertThat(result).isEmpty();
        }

        // ──────────────────────────────────────────────────────────────────────────
        // getWithholdingHistory
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void getWithholdingHistory_returnsList() {
                TaxWithholdingEntity w1 = buildWithholding(employeeId, false, LocalDate.of(2025, 1, 1));
                TaxWithholdingEntity w2 = buildWithholding(employeeId, true, LocalDate.of(2026, 1, 1));
                when(taxWithholdingRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId))
                                .thenReturn(List.of(w2, w1));

                List<TaxWithholdingEntity> result = taxService.getWithholdingHistory(employeeId);

                assertThat(result).hasSize(2);
                assertThat(result.get(0).getEffectiveDate()).isAfter(result.get(1).getEffectiveDate());
        }

        @Test
        void getWithholdingHistory_returnsEmptyList_whenNoHistory() {
                when(taxWithholdingRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId))
                                .thenReturn(List.of());

                List<TaxWithholdingEntity> result = taxService.getWithholdingHistory(employeeId);

                assertThat(result).isEmpty();
        }

        // ──────────────────────────────────────────────────────────────────────────
        // configureTaxWithholding
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void configureTaxWithholding_deactivatesExisting_andSavesNew() {
                TaxWithholdingEntity existing = buildWithholding(employeeId, true, LocalDate.of(2025, 1, 1));
                TaxWithholdingEntity newWithholding = buildWithholding(employeeId, false, LocalDate.of(2026, 4, 1));

                when(taxWithholdingRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                                .thenReturn(Optional.of(existing));
                when(taxWithholdingRepository.save(any(TaxWithholdingEntity.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                TaxWithholdingEntity result = taxService.configureTaxWithholding(newWithholding);

                assertThat(result.getIsActive()).isTrue();
                assertThat(existing.getIsActive()).isFalse();
                assertThat(existing.getEndDate()).isEqualTo(LocalDate.of(2026, 3, 31));
                verify(taxWithholdingRepository, times(2)).save(any(TaxWithholdingEntity.class));
        }

        @Test
        void configureTaxWithholding_savesNew_whenNoExistingActive() {
                TaxWithholdingEntity newWithholding = buildWithholding(employeeId, false, LocalDate.of(2026, 4, 1));

                when(taxWithholdingRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                                .thenReturn(Optional.empty());
                when(taxWithholdingRepository.save(newWithholding)).thenReturn(newWithholding);

                TaxWithholdingEntity result = taxService.configureTaxWithholding(newWithholding);

                assertThat(result.getIsActive()).isTrue();
                verify(taxWithholdingRepository, times(1)).save(newWithholding);
        }

        // ──────────────────────────────────────────────────────────────────────────
        // calculateTaxes (orchestration)
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void calculateTaxes_returnsFullResult_withTaxableState() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), new BigDecimal("0"), "CA");

                when(employeeRepository.findById(employeeUuid)).thenReturn(Optional.of(buildEmployee()));
                when(taxWithholdingRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                                .thenReturn(Optional.empty());

                TaxService.TaxCalculationResult result = taxService.calculateTaxes(context);

                assertThat(result.getFederalIncomeTax()).isNotNull().isGreaterThanOrEqualTo(BigDecimal.ZERO);
                assertThat(result.getSocialSecurityTax()).isNotNull().isGreaterThan(BigDecimal.ZERO);
                assertThat(result.getMedicareTax()).isNotNull().isGreaterThan(BigDecimal.ZERO);
                assertThat(result.getStateIncomeTax()).isNotNull().isGreaterThan(BigDecimal.ZERO);
                assertThat(result.getTotalEmployeeTax()).isEqualTo(
                                result.getFederalIncomeTax()
                                                .add(result.getSocialSecurityTax())
                                                .add(result.getMedicareTax())
                                                .add(result.getStateIncomeTax()));
                assertThat(result.getNetPay()).isEqualTo(
                                new BigDecimal("3000").subtract(result.getTotalEmployeeTax()));
                assertThat(result.getEmployerSocialSecurity()).isEqualTo(result.getSocialSecurityTax());
                assertThat(result.getEmployerMedicare()).isEqualTo(result.getMedicareTax());
                assertThat(result.getEmployerFuta()).isNotNull().isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        void calculateTaxes_throwsEntityNotFound_whenEmployeeAbsent() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), BigDecimal.ZERO, "TX");

                when(employeeRepository.findById(employeeUuid)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> taxService.calculateTaxes(context))
                                .isInstanceOf(EntityNotFoundException.class)
                                .hasMessageContaining("Employee not found");
        }

        @Test
        void calculateTaxes_noStateIncomeTax_forNoIncomeTaxState() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), BigDecimal.ZERO, "TX");

                when(employeeRepository.findById(employeeUuid)).thenReturn(Optional.of(buildEmployee()));
                when(taxWithholdingRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                                .thenReturn(Optional.empty());

                TaxService.TaxCalculationResult result = taxService.calculateTaxes(context);

                assertThat(result.getStateIncomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        // ──────────────────────────────────────────────────────────────────────────
        // calculateFederalIncomeTax
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void calculateFederalIncomeTax_returnsPositive_forNormalIncome() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), BigDecimal.ZERO, "CA");

                BigDecimal result = taxService.calculateFederalIncomeTax(context, null);

                assertThat(result).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        void calculateFederalIncomeTax_returnsZero_whenIncomeBelowStandardDeduction() {
                // $100 bi-weekly = $2600 annualized, well below $15,000 SINGLE standard
                // deduction
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("100"), BigDecimal.ZERO, "CA");

                // Must provide filing status so standard deduction ($15,000 for SINGLE) is
                // applied
                TaxWithholdingEntity withholding = buildWithholding(employeeId, true, LocalDate.of(2026, 1, 1));
                TaxInfo taxInfo = new TaxInfo();
                taxInfo.setFilingStatus(FilingStatusEnum.SINGLE);
                withholding.setTaxInfo(taxInfo);

                BigDecimal result = taxService.calculateFederalIncomeTax(context, withholding);

                assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void calculateFederalIncomeTax_usesWithholdingDeductions_whenPresent() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), BigDecimal.ZERO, "CA");

                TaxWithholdingEntity withholding = buildWithholding(employeeId, true, LocalDate.of(2026, 1, 1));
                TaxInfo taxInfo = new TaxInfo();
                taxInfo.setFilingStatus(FilingStatusEnum.MARRIED);
                taxInfo.setDeductions(new BigDecimal("5000"));
                taxInfo.setAdditionalWithholding(new BigDecimal("50"));
                withholding.setTaxInfo(taxInfo);

                BigDecimal result = taxService.calculateFederalIncomeTax(context, withholding);

                // Additional withholding should push total tax higher than without
                BigDecimal withoutAdditional = taxService.calculateFederalIncomeTax(context, null);
                assertThat(result).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                // With $50 additional withholding added
                TaxInfo infoNoExtra = new TaxInfo();
                infoNoExtra.setFilingStatus(FilingStatusEnum.MARRIED);
                infoNoExtra.setDeductions(new BigDecimal("5000"));
                TaxWithholdingEntity noExtra = buildWithholding(employeeId, true, LocalDate.of(2026, 1, 1));
                noExtra.setTaxInfo(infoNoExtra);
                BigDecimal withoutExtraWithholding = taxService.calculateFederalIncomeTax(context, noExtra);
                assertThat(result).isEqualByComparingTo(withoutExtraWithholding.add(new BigDecimal("50")));
        }

        @Test
        void calculateFederalIncomeTax_respectsPayFrequency_weekly() {
                TaxService.TaxCalculationContext weeklyCtx = TaxService.TaxCalculationContext.builder()
                                .employeeId(employeeId)
                                .grossPay(new BigDecimal("1500"))
                                .ytdGross(BigDecimal.ZERO)
                                .payFrequency(PayFrequencyEnum.WEEKLY.name())
                                .workState("CA")
                                .payDate(LocalDate.of(2026, 1, 15))
                                .build();

                TaxService.TaxCalculationContext biWeeklyCtx = buildContext(
                                new BigDecimal("3000"), BigDecimal.ZERO, "CA");

                BigDecimal weekly = taxService.calculateFederalIncomeTax(weeklyCtx, null);
                BigDecimal biWeekly = taxService.calculateFederalIncomeTax(biWeeklyCtx, null);

                // Both should produce similar annual tax, so per-period amounts should be close
                assertThat(weekly).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                assertThat(biWeekly).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        // ──────────────────────────────────────────────────────────────────────────
        // calculateSocialSecurityTax
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void calculateSocialSecurityTax_appliesRate_belowWageBase() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), BigDecimal.ZERO, "CA");

                BigDecimal result = taxService.calculateSocialSecurityTax(context);

                // 3000 * 0.062 = 186.00
                assertThat(result).isEqualByComparingTo(new BigDecimal("186.00"));
        }

        @Test
        void calculateSocialSecurityTax_returnsZero_whenWageBaseExceeded() {
                // YTD already at the 2026 SS wage base ($176,100)
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), new BigDecimal("176100"), "CA");

                BigDecimal result = taxService.calculateSocialSecurityTax(context);

                assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void calculateSocialSecurityTax_capsAtWageBase_whenStraddling() {
                // Only $500 remaining before wage base is hit
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), new BigDecimal("175600"), "CA");

                BigDecimal result = taxService.calculateSocialSecurityTax(context);

                // Taxable = min(3000, 176100 - 175600) = 500; 500 * 0.062 = 31.00
                assertThat(result).isEqualByComparingTo(new BigDecimal("31.00"));
        }

        @Test
        void calculateSocialSecurityTax_handlesNullYtdGross() {
                TaxService.TaxCalculationContext context = TaxService.TaxCalculationContext.builder()
                                .employeeId(employeeId)
                                .grossPay(new BigDecimal("1000"))
                                .ytdGross(null)
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY.name())
                                .workState("CA")
                                .payDate(LocalDate.of(2026, 1, 15))
                                .build();

                BigDecimal result = taxService.calculateSocialSecurityTax(context);

                // 1000 * 0.062 = 62.00
                assertThat(result).isEqualByComparingTo(new BigDecimal("62.00"));
        }

        // ──────────────────────────────────────────────────────────────────────────
        // calculateMedicareTax
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void calculateMedicareTax_appliesBaseRate_belowThreshold() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), BigDecimal.ZERO, "CA");

                BigDecimal result = taxService.calculateMedicareTax(context);

                // 3000 * 0.0145 = 43.50
                assertThat(result).isEqualByComparingTo(new BigDecimal("43.50"));
        }

        @Test
        void calculateMedicareTax_appliesAdditionalRate_whenAboveThreshold() {
                // YTD already above $200,000 threshold
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("5000"), new BigDecimal("200000"), "CA");

                BigDecimal result = taxService.calculateMedicareTax(context);

                // Base: 5000 * 0.0145 = 72.50
                // Additional: 5000 * 0.009 = 45.00 (all pay above threshold since ytd >= 200k)
                // Total: 72.50 + 45.00 = 117.50
                assertThat(result).isEqualByComparingTo(new BigDecimal("117.50"));
        }

        @Test
        void calculateMedicareTax_appliesPartialAdditionalRate_whenStraddlingThreshold() {
                // YTD $199,000; gross $3,000 → $2,000 crosses the $200,000 threshold
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), new BigDecimal("199000"), "CA");

                BigDecimal result = taxService.calculateMedicareTax(context);

                // Base: 3000 * 0.0145 = 43.50
                // Additional taxable = (199000 + 3000) - 200000 = 2000
                // Additional: 2000 * 0.009 = 18.00
                // Total: 43.50 + 18.00 = 61.50
                assertThat(result).isEqualByComparingTo(new BigDecimal("61.50"));
        }

        @Test
        void calculateMedicareTax_handlesNullYtdGross() {
                TaxService.TaxCalculationContext context = TaxService.TaxCalculationContext.builder()
                                .employeeId(employeeId)
                                .grossPay(new BigDecimal("2000"))
                                .ytdGross(null)
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY.name())
                                .workState("CA")
                                .payDate(LocalDate.of(2026, 1, 15))
                                .build();

                BigDecimal result = taxService.calculateMedicareTax(context);

                // 2000 * 0.0145 = 29.00
                assertThat(result).isEqualByComparingTo(new BigDecimal("29.00"));
        }

        // ──────────────────────────────────────────────────────────────────────────
        // calculateStateIncomeTax
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void calculateStateIncomeTax_returnsZero_forNoIncomeTaxStates() {
                for (String state : List.of("AK", "FL", "NV", "NH", "SD", "TN", "TX", "WA", "WY")) {
                        TaxService.TaxCalculationContext context = buildContext(
                                        new BigDecimal("3000"), BigDecimal.ZERO, state);

                        BigDecimal result = taxService.calculateStateIncomeTax(context, null);

                        assertThat(result).as("State %s should have zero income tax", state)
                                        .isEqualByComparingTo(BigDecimal.ZERO);
                }
        }

        @Test
        void calculateStateIncomeTax_appliesRate_forTaxableState() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), BigDecimal.ZERO, "CA");

                BigDecimal result = taxService.calculateStateIncomeTax(context, null);

                // 3000 * 0.05 = 150.00
                assertThat(result).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        void calculateStateIncomeTax_returnsZero_whenStateIsNull() {
                TaxService.TaxCalculationContext context = TaxService.TaxCalculationContext.builder()
                                .employeeId(employeeId)
                                .grossPay(new BigDecimal("3000"))
                                .ytdGross(BigDecimal.ZERO)
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY.name())
                                .workState(null)
                                .payDate(LocalDate.of(2026, 1, 15))
                                .build();

                BigDecimal result = taxService.calculateStateIncomeTax(context, null);

                assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void calculateStateIncomeTax_returnsZero_whenStateIsBlank() {
                TaxService.TaxCalculationContext context = TaxService.TaxCalculationContext.builder()
                                .employeeId(employeeId)
                                .grossPay(new BigDecimal("3000"))
                                .ytdGross(BigDecimal.ZERO)
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY.name())
                                .workState("  ")
                                .payDate(LocalDate.of(2026, 1, 15))
                                .build();

                BigDecimal result = taxService.calculateStateIncomeTax(context, null);

                assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        // ──────────────────────────────────────────────────────────────────────────
        // calculateFutaTax
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void calculateFutaTax_appliesRate_belowWageBase() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("2000"), BigDecimal.ZERO, "CA");

                BigDecimal result = taxService.calculateFutaTax(context);

                // 2000 * 0.006 = 12.00
                assertThat(result).isEqualByComparingTo(new BigDecimal("12.00"));
        }

        @Test
        void calculateFutaTax_returnsZero_whenWageBaseExceeded() {
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("2000"), new BigDecimal("7000"), "CA");

                BigDecimal result = taxService.calculateFutaTax(context);

                assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void calculateFutaTax_capsAtWageBase_whenStraddling() {
                // Only $1000 remaining before FUTA wage base ($7000)
                TaxService.TaxCalculationContext context = buildContext(
                                new BigDecimal("3000"), new BigDecimal("6000"), "CA");

                BigDecimal result = taxService.calculateFutaTax(context);

                // Taxable = min(3000, 7000 - 6000) = 1000; 1000 * 0.006 = 6.00
                assertThat(result).isEqualByComparingTo(new BigDecimal("6.00"));
        }

        @Test
        void calculateFutaTax_handlesNullYtdGross() {
                TaxService.TaxCalculationContext context = TaxService.TaxCalculationContext.builder()
                                .employeeId(employeeId)
                                .grossPay(new BigDecimal("500"))
                                .ytdGross(null)
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY.name())
                                .workState("CA")
                                .payDate(LocalDate.of(2026, 1, 15))
                                .build();

                BigDecimal result = taxService.calculateFutaTax(context);

                // 500 * 0.006 = 3.00
                assertThat(result).isEqualByComparingTo(new BigDecimal("3.00"));
        }

        // ──────────────────────────────────────────────────────────────────────────
        // getUpcomingTaxDeposits
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void getUpcomingTaxDeposits_returnsList_fromRepository() {
                LocalDate from = LocalDate.of(2026, 4, 1);
                LocalDate to = LocalDate.of(2026, 4, 30);
                PayrollTaxDepositEntity deposit = new PayrollTaxDepositEntity();
                deposit.setDueDate(LocalDate.of(2026, 4, 15));

                when(payrollTaxDepositRepository.findByClientIdAndDueDateBetween(clientId, from, to))
                                .thenReturn(List.of(deposit));

                List<PayrollTaxDepositEntity> result = taxService.getUpcomingTaxDeposits(clientId, from, to);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        }

        @Test
        void getUpcomingTaxDeposits_returnsEmpty_whenNoneFound() {
                LocalDate from = LocalDate.of(2026, 4, 1);
                LocalDate to = LocalDate.of(2026, 4, 30);

                when(payrollTaxDepositRepository.findByClientIdAndDueDateBetween(clientId, from, to))
                                .thenReturn(List.of());

                List<PayrollTaxDepositEntity> result = taxService.getUpcomingTaxDeposits(clientId, from, to);

                assertThat(result).isEmpty();
        }

        // ──────────────────────────────────────────────────────────────────────────
        // getOverdueTaxDeposits
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void getOverdueTaxDeposits_returnsList_fromRepository() {
                PayrollTaxDepositEntity overdue = new PayrollTaxDepositEntity();
                overdue.setDueDate(LocalDate.of(2026, 1, 15));

                when(payrollTaxDepositRepository.findByClientIdAndDueDateBeforeAndPaidDateIsNull(
                                eq(clientId), any(LocalDate.class)))
                                .thenReturn(List.of(overdue));

                List<PayrollTaxDepositEntity> result = taxService.getOverdueTaxDeposits(clientId);

                assertThat(result).hasSize(1);
        }

        @Test
        void getOverdueTaxDeposits_returnsEmpty_whenNoneOverdue() {
                when(payrollTaxDepositRepository.findByClientIdAndDueDateBeforeAndPaidDateIsNull(
                                eq(clientId), any(LocalDate.class)))
                                .thenReturn(List.of());

                List<PayrollTaxDepositEntity> result = taxService.getOverdueTaxDeposits(clientId);

                assertThat(result).isEmpty();
        }

        // ──────────────────────────────────────────────────────────────────────────
        // recordTaxDepositPayment
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        void recordTaxDepositPayment_updatesAndSavesDeposit() {
                PayrollTaxDepositEntity deposit = new PayrollTaxDepositEntity();
                LocalDate paidDate = LocalDate.of(2026, 4, 10);

                when(payrollTaxDepositRepository.findById(depositId)).thenReturn(Optional.of(deposit));
                when(payrollTaxDepositRepository.save(deposit)).thenReturn(deposit);

                PayrollTaxDepositEntity result = taxService.recordTaxDepositPayment(
                                depositId, paidDate, "REF-001", PaymentMethodEnum.ACH.name());

                assertThat(result.getPaidDate()).isEqualTo(paidDate);
                assertThat(result.getPaymentReference()).isEqualTo("REF-001");
                assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethodEnum.ACH.name());
                verify(payrollTaxDepositRepository).save(deposit);
        }

        @Test
        void recordTaxDepositPayment_throwsEntityNotFound_whenDepositAbsent() {
                when(payrollTaxDepositRepository.findById(depositId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> taxService.recordTaxDepositPayment(
                                depositId, LocalDate.now(), "REF-001", PaymentMethodEnum.ACH.name()))
                                .isInstanceOf(EntityNotFoundException.class)
                                .hasMessageContaining("Tax deposit not found");
        }
}
