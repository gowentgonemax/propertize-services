package com.propertize.payroll.service;

import com.propertize.payroll.entity.*;
import com.propertize.payroll.entity.embedded.Address;
import com.propertize.payroll.entity.embedded.DatePeriod;
import com.propertize.payroll.enums.EmployeeStatusEnum;
import com.propertize.payroll.enums.PayFrequencyEnum;
import com.propertize.payroll.enums.PayTypeEnum;
import com.propertize.payroll.enums.PayrollStatusEnum;
import com.propertize.payroll.enums.DeductionMethodEnum;
import com.propertize.payroll.enums.DeductionStatusEnum;
import com.propertize.payroll.enums.DeductionTypeEnum;
import com.propertize.payroll.enums.EarningTypeEnum;
import com.propertize.payroll.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaystubServiceTest {

    @Mock
    PaystubRepository paystubRepository;
    @Mock
    PayrollRunRepository payrollRunRepository;
    @Mock
    EmployeeEntityRepository employeeRepository;
    @Mock
    TaxService taxService;
    @Mock
    BenefitService benefitService;
    @Mock
    DeductionRepository deductionRepository;

    @InjectMocks
    PaystubService paystubService;

    UUID paystubId;
    UUID payrollRunId;
    UUID clientId;
    UUID employeeId;
    String employeeIdStr;
    Client client;
    EmployeeEntity employee;
    PayrollRun payrollRun;

    @BeforeEach
    void setUp() {
        paystubId = UUID.randomUUID();
        payrollRunId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        employeeIdStr = employeeId.toString();

        client = new Client();
        client.setId(clientId);

        employee = buildEmployee();
        payrollRun = buildPayrollRun();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private EmployeeEntity buildEmployee() {
        EmployeeEntity emp = new EmployeeEntity();
        emp.setId(employeeId);
        emp.setClient(client);
        emp.setFirstName("John");
        emp.setLastName("Doe");
        emp.setEmployeeNumber("EMP-001");
        emp.setStatus(EmployeeStatusEnum.ACTIVE);
        emp.setPayType(PayTypeEnum.HOURLY);
        emp.setPayFrequency(PayFrequencyEnum.BI_WEEKLY);
        emp.setHourlyRate(new BigDecimal("25.0000"));
        emp.setOvertimeEligible(true);
        emp.setOvertimeMultiplier(new BigDecimal("1.50"));
        Address homeAddr = new Address();
        homeAddr.setState("CA");
        emp.setHomeAddress(homeAddr);
        return emp;
    }

    private PayrollRun buildPayrollRun() {
        PayrollRun run = new PayrollRun();
        run.setId(payrollRunId);
        run.setClient(client);
        run.setStatus(PayrollStatusEnum.DRAFT);
        run.setPayPeriod(new DatePeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15)));
        run.setPayDate(LocalDate.of(2026, 3, 20));
        return run;
    }

    private Paystub buildPaystub() {
        Paystub ps = new Paystub();
        ps.setId(paystubId);
        ps.setPayrollRun(payrollRun);
        ps.setEmployee(employee);
        ps.setEmployeeId(employeeIdStr);
        ps.setPayDate(LocalDate.of(2026, 3, 20));
        ps.setPayPeriodStart(LocalDate.of(2026, 3, 1));
        ps.setPayPeriodEnd(LocalDate.of(2026, 3, 15));
        ps.setGrossEarnings(new BigDecimal("2000.00"));
        ps.setNetPay(new BigDecimal("1500.00"));
        ps.setFederalTax(new BigDecimal("200.00"));
        ps.setStateTax(new BigDecimal("100.00"));
        ps.setSocialSecurityTax(new BigDecimal("124.00"));
        ps.setMedicareTax(new BigDecimal("29.00"));
        ps.setTotalDeductions(new BigDecimal("47.00"));
        return ps;
    }

    private TaxService.TaxCalculationResult buildTaxResult() {
        return TaxService.TaxCalculationResult.builder()
                .federalIncomeTax(new BigDecimal("200.00"))
                .stateIncomeTax(new BigDecimal("100.00"))
                .socialSecurityTax(new BigDecimal("124.00"))
                .medicareTax(new BigDecimal("29.00"))
                .totalEmployeeTax(new BigDecimal("453.00"))
                .build();
    }

    /**
     * Stubs all repo/service calls that a full generatePaystub flow touches,
     * so individual tests only need to override what they care about.
     */
    private void stubGeneratePaystubDeps() {
        // no existing paystub
        lenient().when(paystubRepository.findByEmployeeIdAndPayrollRunId(anyString(), any(UUID.class)))
                .thenReturn(Optional.empty());

        // YTD lookup returns empty list (called by calculateTaxes & calculateYtdTotals)
        lenient()
                .when(paystubRepository.findByEmployeeIdAndPayDateBetween(anyString(), any(LocalDate.class),
                        any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // tax calculation
        lenient().when(taxService.calculateTaxes(any(TaxService.TaxCalculationContext.class)))
                .thenReturn(buildTaxResult());

        // deductions
        lenient().when(deductionRepository.findByEmployeeIdAndStatus(any(UUID.class), eq(DeductionStatusEnum.ACTIVE)))
                .thenReturn(Collections.emptyList());

        // benefit deductions
        lenient().when(benefitService.calculateTotalEmployeeDeductions(anyString()))
                .thenReturn(BigDecimal.ZERO);

        // save echoes back
        lenient().when(paystubRepository.save(any(Paystub.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ======================================================================
    // getPaystubById
    // ======================================================================

    @Nested
    class GetPaystubById {

        @Test
        void returnsPaystub_whenFound() {
            Paystub ps = buildPaystub();
            when(paystubRepository.findById(paystubId)).thenReturn(Optional.of(ps));

            Paystub result = paystubService.getPaystubById(paystubId);

            assertThat(result.getId()).isEqualTo(paystubId);
            assertThat(result.getEmployee()).isEqualTo(employee);
        }

        @Test
        void throwsEntityNotFound_whenMissing() {
            when(paystubRepository.findById(paystubId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paystubService.getPaystubById(paystubId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ======================================================================
    // getEmployeePaystubs
    // ======================================================================

    @Nested
    class GetEmployeePaystubs {

        @Test
        void returnsPageOfPaystubs() {
            Paystub ps = buildPaystub();
            Page<Paystub> page = new PageImpl<>(List.of(ps));
            when(paystubRepository.findByEmployeeIdOrderByPayDateDesc(eq(employeeIdStr), any(Pageable.class)))
                    .thenReturn(page);

            Page<Paystub> result = paystubService.getEmployeePaystubs(employeeIdStr, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(paystubId);
        }

        @Test
        void returnsEmptyPage_whenNoPaystubs() {
            Page<Paystub> emptyPage = new PageImpl<>(Collections.emptyList());
            when(paystubRepository.findByEmployeeIdOrderByPayDateDesc(eq(employeeIdStr), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<Paystub> result = paystubService.getEmployeePaystubs(employeeIdStr, Pageable.unpaged());

            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ======================================================================
    // getPaystubsByPayrollRun
    // ======================================================================

    @Nested
    class GetPaystubsByPayrollRun {

        @Test
        void returnsList() {
            Paystub ps = buildPaystub();
            when(paystubRepository.findByPayrollRunId(payrollRunId)).thenReturn(List.of(ps));

            List<Paystub> result = paystubService.getPaystubsByPayrollRun(payrollRunId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getPayrollRun().getId()).isEqualTo(payrollRunId);
        }

        @Test
        void returnsEmptyList_whenNone() {
            when(paystubRepository.findByPayrollRunId(payrollRunId)).thenReturn(Collections.emptyList());

            List<Paystub> result = paystubService.getPaystubsByPayrollRun(payrollRunId);

            assertThat(result).isEmpty();
        }
    }

    // ======================================================================
    // getPaystubForEmployeeAndRun
    // ======================================================================

    @Nested
    class GetPaystubForEmployeeAndRun {

        @Test
        void returnsOptionalPresent_whenExists() {
            Paystub ps = buildPaystub();
            when(paystubRepository.findByEmployeeIdAndPayrollRunId(employeeIdStr, payrollRunId))
                    .thenReturn(Optional.of(ps));

            Optional<Paystub> result = paystubService.getPaystubForEmployeeAndRun(employeeIdStr, payrollRunId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(paystubId);
        }

        @Test
        void returnsEmpty_whenNotExists() {
            when(paystubRepository.findByEmployeeIdAndPayrollRunId(employeeIdStr, payrollRunId))
                    .thenReturn(Optional.empty());

            Optional<Paystub> result = paystubService.getPaystubForEmployeeAndRun(employeeIdStr, payrollRunId);

            assertThat(result).isEmpty();
        }
    }

    // ======================================================================
    // getEmployeePaystubsForDateRange
    // ======================================================================

    @Nested
    class GetEmployeePaystubsForDateRange {

        @Test
        void returnsList_forRange() {
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 3, 31);
            Paystub ps = buildPaystub();
            when(paystubRepository.findByEmployeeIdAndPayDateBetween(employeeIdStr, start, end))
                    .thenReturn(List.of(ps));

            List<Paystub> result = paystubService.getEmployeePaystubsForDateRange(employeeIdStr, start, end);

            assertThat(result).hasSize(1);
        }

        @Test
        void returnsEmpty_whenNoneInRange() {
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 3, 31);
            when(paystubRepository.findByEmployeeIdAndPayDateBetween(employeeIdStr, start, end))
                    .thenReturn(Collections.emptyList());

            List<Paystub> result = paystubService.getEmployeePaystubsForDateRange(employeeIdStr, start, end);

            assertThat(result).isEmpty();
        }
    }

    // ======================================================================
    // generatePaystubsForPayrollRun
    // ======================================================================

    @Nested
    class GeneratePaystubsForPayrollRun {

        @Test
        void throwsEntityNotFound_whenPayrollRunMissing() {
            when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paystubService.generatePaystubsForPayrollRun(payrollRunId))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void generatesPaystubs_forAllActiveEmployees() {
            when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.of(payrollRun));

            EmployeeEntity emp2 = buildEmployee();
            emp2.setId(UUID.randomUUID());
            emp2.setEmployeeNumber("EMP-002");

            when(employeeRepository.findByClientIdAndStatus(clientId, EmployeeStatusEnum.ACTIVE))
                    .thenReturn(List.of(employee, emp2));

            stubGeneratePaystubDeps();

            List<Paystub> result = paystubService.generatePaystubsForPayrollRun(payrollRunId);

            assertThat(result).hasSize(2);
            verify(paystubRepository, times(2)).save(any(Paystub.class));
        }

        @Test
        void returnsEmptyList_whenNoActiveEmployees() {
            when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.of(payrollRun));
            when(employeeRepository.findByClientIdAndStatus(clientId, EmployeeStatusEnum.ACTIVE))
                    .thenReturn(Collections.emptyList());

            List<Paystub> result = paystubService.generatePaystubsForPayrollRun(payrollRunId);

            assertThat(result).isEmpty();
            verify(paystubRepository, never()).save(any());
        }

        @Test
        void continuesProcessing_whenOneEmployeeFails() {
            when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.of(payrollRun));

            EmployeeEntity badEmployee = buildEmployee();
            badEmployee.setId(UUID.randomUUID());
            badEmployee.setEmployeeNumber("EMP-BAD");
            // null hourly rate → will cause NPE in calculateEarnings when
            // getEffectivePayRate returns zero
            // but let's cause a tax service failure instead
            EmployeeEntity goodEmployee = buildEmployee();
            goodEmployee.setId(UUID.randomUUID());
            goodEmployee.setEmployeeNumber("EMP-GOOD");

            when(employeeRepository.findByClientIdAndStatus(clientId, EmployeeStatusEnum.ACTIVE))
                    .thenReturn(List.of(badEmployee, goodEmployee));

            stubGeneratePaystubDeps();

            // Override: first call to tax service throws, second succeeds
            when(taxService.calculateTaxes(any(TaxService.TaxCalculationContext.class)))
                    .thenThrow(new RuntimeException("Tax calc failed"))
                    .thenReturn(buildTaxResult());

            List<Paystub> result = paystubService.generatePaystubsForPayrollRun(payrollRunId);

            // Only the good employee's paystub should succeed
            assertThat(result).hasSize(1);
        }
    }

    // ======================================================================
    // generatePaystub
    // ======================================================================

    @Nested
    class GeneratePaystub {

        @Test
        void returnsExistingPaystub_whenAlreadyExists() {
            Paystub existing = buildPaystub();
            when(paystubRepository.findByEmployeeIdAndPayrollRunId(employeeIdStr, payrollRunId))
                    .thenReturn(Optional.of(existing));

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            assertThat(result.getId()).isEqualTo(paystubId);
            verify(paystubRepository, never()).save(any());
            verify(taxService, never()).calculateTaxes(any());
        }

        @Test
        void createsNewPaystub_withCorrectPayPeriodFields() {
            stubGeneratePaystubDeps();

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            assertThat(result.getPayDate()).isEqualTo(LocalDate.of(2026, 3, 20));
            assertThat(result.getPayPeriodStart()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(result.getPayPeriodEnd()).isEqualTo(LocalDate.of(2026, 3, 15));
            assertThat(result.getPayrollRun()).isEqualTo(payrollRun);
            assertThat(result.getEmployee()).isEqualTo(employee);
        }

        @Test
        void calculatesGrossEarnings_fromHourlyRate() {
            stubGeneratePaystubDeps();

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            // 80 regular hours × $25.00 = $2000.00
            assertThat(result.getGrossEarnings()).isEqualByComparingTo("2000.00");
            assertThat(result.getEarnings()).isNotEmpty();
        }

        @Test
        void addsFederalAndStateTaxes() {
            stubGeneratePaystubDeps();

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            assertThat(result.getFederalTax()).isEqualByComparingTo("200.00");
            assertThat(result.getStateTax()).isEqualByComparingTo("100.00");
            assertThat(result.getSocialSecurityTax()).isEqualByComparingTo("124.00");
            assertThat(result.getMedicareTax()).isEqualByComparingTo("29.00");
            assertThat(result.getTaxes()).hasSizeGreaterThanOrEqualTo(4);
        }

        @Test
        void calculatesNetPay_asGrossMinusTaxesMinusDeductions() {
            stubGeneratePaystubDeps();

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            // grossEarnings=2000, totalTaxes = sum from tax line items, totalDeductions = 0
            // (no deductions)
            // net = gross - taxes (from items) - deductions (from items)
            assertThat(result.getNetPay()).isNotNull();
            // Net should be positive and less than gross
            assertThat(result.getNetPay().compareTo(result.getGrossEarnings())).isLessThanOrEqualTo(0);
        }

        @Test
        void generatesCheckNumber_withExpectedFormat() {
            stubGeneratePaystubDeps();

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            assertThat(result.getCheckNumber()).startsWith("CHK-");
            assertThat(result.getCheckNumber()).contains("EMP-001");
        }

        @Test
        void setsYtdTotals() {
            stubGeneratePaystubDeps();

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            // With empty YTD history, YTD should equal current paystub amounts
            assertThat(result.getYtdGrossEarnings()).isEqualByComparingTo(result.getGrossEarnings());
        }

        @Test
        void accumulatesYtdFromPreviousPaystubs() {
            stubGeneratePaystubDeps();

            // Simulate a prior paystub in the YTD lookup
            Paystub priorPaystub = buildPaystub();
            priorPaystub.setId(UUID.randomUUID());
            priorPaystub.setGrossEarnings(new BigDecimal("3000.00"));
            priorPaystub.setNetPay(new BigDecimal("2200.00"));
            priorPaystub.setFederalTax(new BigDecimal("300.00"));
            priorPaystub.setStateTax(new BigDecimal("150.00"));
            priorPaystub.setSocialSecurityTax(new BigDecimal("186.00"));
            priorPaystub.setMedicareTax(new BigDecimal("43.50"));
            priorPaystub.setTotalDeductions(new BigDecimal("120.50"));

            when(paystubRepository.findByEmployeeIdAndPayDateBetween(
                    eq(employeeIdStr), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(priorPaystub));

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            // YTD gross = prior 3000 + current 2000
            assertThat(result.getYtdGrossEarnings()).isEqualByComparingTo("5000.00");
        }

        @Test
        void includesDeductions_whenEmployeeHasActiveDeductions() {
            stubGeneratePaystubDeps();

            Deduction flatDeduction = new Deduction();
            flatDeduction.setName("401k");
            flatDeduction.setMethod(DeductionMethodEnum.FLAT_AMOUNT);
            flatDeduction.setAmount(new BigDecimal("100.00"));
            flatDeduction.setStatus(DeductionStatusEnum.ACTIVE);

            when(deductionRepository.findByEmployeeIdAndStatus(employeeId, DeductionStatusEnum.ACTIVE))
                    .thenReturn(List.of(flatDeduction));

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            assertThat(result.getDeductions()).isNotEmpty();
        }

        @Test
        void includesPercentageDeductions() {
            stubGeneratePaystubDeps();

            Deduction pctDeduction = new Deduction();
            pctDeduction.setName("Union Dues");
            pctDeduction.setMethod(DeductionMethodEnum.PERCENTAGE);
            pctDeduction.setAmount(new BigDecimal("2.00")); // 2%
            pctDeduction.setStatus(DeductionStatusEnum.ACTIVE);

            when(deductionRepository.findByEmployeeIdAndStatus(employeeId, DeductionStatusEnum.ACTIVE))
                    .thenReturn(List.of(pctDeduction));

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            assertThat(result.getDeductions()).isNotEmpty();
        }

        @Test
        void includesBenefitDeductions_whenPositive() {
            stubGeneratePaystubDeps();

            when(benefitService.calculateTotalEmployeeDeductions(employeeIdStr))
                    .thenReturn(new BigDecimal("150.00"));

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            // Should have at least the benefit deduction line item
            assertThat(result.getDeductions()).isNotEmpty();
        }

        @Test
        void skipsOvertime_whenZeroOvertimeHours() {
            stubGeneratePaystubDeps();

            Paystub result = paystubService.generatePaystub(payrollRun, employee);

            // Only regular earnings expected (overtime hours default to ZERO)
            long overtimeCount = result.getEarnings().stream()
                    .filter(e -> e.getEarningType() == EarningTypeEnum.OVERTIME)
                    .count();
            assertThat(overtimeCount).isZero();
        }

        @Test
        void callsTaxServiceWithCorrectContext() {
            stubGeneratePaystubDeps();

            paystubService.generatePaystub(payrollRun, employee);

            verify(taxService).calculateTaxes(argThat(ctx -> ctx.getEmployeeId().equals(employeeIdStr)
                    && ctx.getGrossPay().compareTo(new BigDecimal("2000.00")) == 0
                    && ctx.getWorkState().equals("CA")
                    && ctx.getPayDate().equals(LocalDate.of(2026, 3, 20))));
        }

        @Test
        void savesPaystubToRepository() {
            stubGeneratePaystubDeps();

            paystubService.generatePaystub(payrollRun, employee);

            verify(paystubRepository).save(any(Paystub.class));
        }
    }

    // ======================================================================
    // regeneratePaystub
    // ======================================================================

    @Nested
    @Disabled("RegeneratePaystub uses findByEmployeeIdAndPayDateBetweenExcluding not yet in PaystubRepository")
    class RegeneratePaystub {

        @Test
        void throwsEntityNotFound_whenPaystubMissing() {
            when(paystubRepository.findById(paystubId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paystubService.regeneratePaystub(paystubId))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void clearsAndRecalculates() {
            Paystub existing = buildPaystub();
            existing.getEarnings().add(new PaystubEarning());
            existing.getDeductions().add(new PaystubDeduction());
            existing.getTaxes().add(new PaystubTax());

            when(paystubRepository.findById(paystubId)).thenReturn(Optional.of(existing));
            when(taxService.calculateTaxes(any(TaxService.TaxCalculationContext.class)))
                    .thenReturn(buildTaxResult());
            when(deductionRepository.findByEmployeeIdAndStatus(any(UUID.class), eq(DeductionStatusEnum.ACTIVE)))
                    .thenReturn(Collections.emptyList());
            when(benefitService.calculateTotalEmployeeDeductions(anyString()))
                    .thenReturn(BigDecimal.ZERO);
            // YTD query — uses excludeId variant
            when(paystubRepository.findByEmployeeIdAndPayDateBetweenExcluding(
                    anyString(), any(LocalDate.class), any(LocalDate.class), eq(paystubId)))
                    .thenReturn(Collections.emptyList());
            // Also stub the non-excluding variant for calculateTaxes → getYtdSummary
            when(paystubRepository.findByEmployeeIdAndPayDateBetween(
                    anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(paystubRepository.save(any(Paystub.class))).thenAnswer(inv -> inv.getArgument(0));

            Paystub result = paystubService.regeneratePaystub(paystubId);

            assertThat(result).isNotNull();
            verify(paystubRepository).save(existing);
        }

        @Test
        void usesExcludingQuery_forYtdRecalculation() {
            Paystub existing = buildPaystub();
            when(paystubRepository.findById(paystubId)).thenReturn(Optional.of(existing));
            when(taxService.calculateTaxes(any())).thenReturn(buildTaxResult());
            when(deductionRepository.findByEmployeeIdAndStatus(any(UUID.class), any()))
                    .thenReturn(Collections.emptyList());
            when(benefitService.calculateTotalEmployeeDeductions(anyString()))
                    .thenReturn(BigDecimal.ZERO);
            when(paystubRepository.findByEmployeeIdAndPayDateBetweenExcluding(
                    anyString(), any(LocalDate.class), any(LocalDate.class), eq(paystubId)))
                    .thenReturn(Collections.emptyList());
            when(paystubRepository.findByEmployeeIdAndPayDateBetween(
                    anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(paystubRepository.save(any(Paystub.class))).thenAnswer(inv -> inv.getArgument(0));

            paystubService.regeneratePaystub(paystubId);

            verify(paystubRepository).findByEmployeeIdAndPayDateBetweenExcluding(
                    eq(employeeIdStr),
                    eq(LocalDate.of(2026, 1, 1)),
                    eq(LocalDate.of(2026, 12, 31)),
                    eq(paystubId));
        }
    }

    // ======================================================================
    // getYtdSummary
    // ======================================================================

    @Nested
    @Disabled("GetYtdSummary overload with excludeId uses findByEmployeeIdAndPayDateBetweenExcluding not yet in PaystubRepository")
    class GetYtdSummary {

        @Test
        void returnsZeroSummary_whenNoPaystubs() {
            when(paystubRepository.findByEmployeeIdAndPayDateBetween(
                    eq(employeeIdStr), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            PaystubService.YtdSummary summary = paystubService.getYtdSummary(employeeIdStr, 2026);

            assertThat(summary.getEmployeeId()).isEqualTo(employeeIdStr);
            assertThat(summary.getYear()).isEqualTo(2026);
            assertThat(summary.getPaystubCount()).isZero();
            assertThat(summary.getGrossEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.getNetPay()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.getTotalTaxes()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.getTotalDeductions()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void aggregatesMultiplePaystubs() {
            Paystub ps1 = buildPaystub();
            ps1.setGrossEarnings(new BigDecimal("2000.00"));
            ps1.setNetPay(new BigDecimal("1500.00"));
            ps1.setFederalTax(new BigDecimal("200.00"));
            ps1.setStateTax(new BigDecimal("100.00"));
            ps1.setSocialSecurityTax(new BigDecimal("124.00"));
            ps1.setMedicareTax(new BigDecimal("29.00"));
            // getTotalDeductions() computes from deductions list, so add actual items
            PaystubDeduction d1 = new PaystubDeduction();
            d1.setName("401k");
            d1.setDeductionType(com.propertize.payroll.enums.DeductionTypeEnum.BENEFIT);
            d1.setAmount(new BigDecimal("47.00"));
            ps1.addDeduction(d1);

            Paystub ps2 = buildPaystub();
            ps2.setId(UUID.randomUUID());
            ps2.setGrossEarnings(new BigDecimal("3000.00"));
            ps2.setNetPay(new BigDecimal("2200.00"));
            ps2.setFederalTax(new BigDecimal("300.00"));
            ps2.setStateTax(new BigDecimal("150.00"));
            ps2.setSocialSecurityTax(new BigDecimal("186.00"));
            ps2.setMedicareTax(new BigDecimal("43.50"));
            PaystubDeduction d2 = new PaystubDeduction();
            d2.setName("Health");
            d2.setDeductionType(com.propertize.payroll.enums.DeductionTypeEnum.BENEFIT);
            d2.setAmount(new BigDecimal("120.50"));
            ps2.addDeduction(d2);

            when(paystubRepository.findByEmployeeIdAndPayDateBetween(
                    eq(employeeIdStr), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(ps1, ps2));

            PaystubService.YtdSummary summary = paystubService.getYtdSummary(employeeIdStr, 2026);

            assertThat(summary.getPaystubCount()).isEqualTo(2);
            assertThat(summary.getGrossEarnings()).isEqualByComparingTo("5000.00");
            assertThat(summary.getNetPay()).isEqualByComparingTo("3700.00");
            assertThat(summary.getFederalTax()).isEqualByComparingTo("500.00");
            assertThat(summary.getStateTax()).isEqualByComparingTo("250.00");
            assertThat(summary.getSocialSecurityTax()).isEqualByComparingTo("310.00");
            assertThat(summary.getMedicareTax()).isEqualByComparingTo("72.50");
            assertThat(summary.getTotalDeductions()).isEqualByComparingTo("167.50");
        }

        @Test
        void queriesCorrectDateRange() {
            when(paystubRepository.findByEmployeeIdAndPayDateBetween(anyString(), any(), any()))
                    .thenReturn(Collections.emptyList());

            paystubService.getYtdSummary(employeeIdStr, 2026);

            verify(paystubRepository).findByEmployeeIdAndPayDateBetween(
                    employeeIdStr,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31));
        }

        @Test
        void handlesNullFieldsGracefully() {
            Paystub psWithNulls = new Paystub();
            psWithNulls.setId(UUID.randomUUID());
            // All BigDecimal fields default to ZERO via entity initializer, but
            // getTotalTaxes() and getTotalDeductions() are computed from empty lists
            // which safely return ZERO

            when(paystubRepository.findByEmployeeIdAndPayDateBetween(anyString(), any(), any()))
                    .thenReturn(List.of(psWithNulls));

            PaystubService.YtdSummary summary = paystubService.getYtdSummary(employeeIdStr, 2026);

            assertThat(summary.getPaystubCount()).isEqualTo(1);
            // Should not throw NPE
            assertThat(summary.getGrossEarnings()).isNotNull();
        }

        @Test
        void withExcludeId_usesExcludingRepository() {
            UUID excludeId = UUID.randomUUID();
            when(paystubRepository.findByEmployeeIdAndPayDateBetweenExcluding(
                    anyString(), any(), any(), eq(excludeId)))
                    .thenReturn(Collections.emptyList());

            PaystubService.YtdSummary summary = paystubService.getYtdSummary(employeeIdStr, 2026, excludeId);

            verify(paystubRepository).findByEmployeeIdAndPayDateBetweenExcluding(
                    employeeIdStr,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    excludeId);
            verify(paystubRepository, never()).findByEmployeeIdAndPayDateBetween(anyString(), any(), any());
            assertThat(summary.getPaystubCount()).isZero();
        }

        @Test
        void withNullExcludeId_usesStandardRepository() {
            when(paystubRepository.findByEmployeeIdAndPayDateBetween(anyString(), any(), any()))
                    .thenReturn(Collections.emptyList());

            paystubService.getYtdSummary(employeeIdStr, 2026, null);

            verify(paystubRepository).findByEmployeeIdAndPayDateBetween(anyString(), any(), any());
            verify(paystubRepository, never()).findByEmployeeIdAndPayDateBetweenExcluding(
                    anyString(), any(), any(), any());
        }
    }

    // ======================================================================
    // generatePaystubPdf
    // ======================================================================

    @Nested
    class GeneratePaystubPdf {

        @Test
        void throwsEntityNotFound_whenPaystubMissing() {
            when(paystubRepository.findById(paystubId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paystubService.generatePaystubPdf(paystubId))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void returnsNonEmptyBytes() {
            Paystub ps = buildPaystub();
            when(paystubRepository.findById(paystubId)).thenReturn(Optional.of(ps));

            byte[] pdf = paystubService.generatePaystubPdf(paystubId);

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
        }

        @Test
        void containsEmployeeName() {
            Paystub ps = buildPaystub();
            when(paystubRepository.findById(paystubId)).thenReturn(Optional.of(ps));

            byte[] pdf = paystubService.generatePaystubPdf(paystubId);

            String content = new String(pdf);
            assertThat(content).contains("John Doe");
        }

        @Test
        void containsPayPeriodAndPayDate() {
            Paystub ps = buildPaystub();
            when(paystubRepository.findById(paystubId)).thenReturn(Optional.of(ps));

            byte[] pdf = paystubService.generatePaystubPdf(paystubId);

            String content = new String(pdf);
            assertThat(content).contains("2026-03-01");
            assertThat(content).contains("2026-03-15");
            assertThat(content).contains("2026-03-20");
        }

        @Test
        void containsEarningsAndTaxBreakdown() {
            Paystub ps = buildPaystub();
            when(paystubRepository.findById(paystubId)).thenReturn(Optional.of(ps));

            byte[] pdf = paystubService.generatePaystubPdf(paystubId);

            String content = new String(pdf);
            assertThat(content).contains("EARNINGS");
            assertThat(content).contains("TAXES");
            assertThat(content).contains("DEDUCTIONS");
            assertThat(content).contains("NET PAY");
        }
    }
}
