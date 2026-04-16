package com.propertize.payroll.service;

import com.propertize.payroll.entity.Client;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.entity.PayrollRun;
import com.propertize.payroll.entity.embedded.DatePeriod;
import com.propertize.commons.enums.employee.EmployeeStatusEnum;
import com.propertize.commons.enums.employee.PayrollStatusEnum;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import com.propertize.payroll.repository.PayrollRunRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    PayrollRunRepository payrollRunRepository;
    @Mock
    EmployeeEntityRepository employeeRepository;

    @InjectMocks
    PayrollService payrollService;

    UUID clientId;
    UUID payrollRunId;
    Client client;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        payrollRunId = UUID.randomUUID();

        client = new Client();
        client.setId(clientId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private PayrollRun buildDraftRun(LocalDate start, LocalDate end, LocalDate payDate) {
        PayrollRun run = new PayrollRun();
        run.setId(payrollRunId);
        run.setClient(client);
        run.setStatus(PayrollStatusEnum.DRAFT);
        run.setPayPeriod(new DatePeriod(start, end));
        run.setPayDate(payDate);
        return run;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getPayrollRunsByClient
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getPayrollRunsByClient_returnsPage() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 1, 20));
        Page<PayrollRun> page = new PageImpl<>(List.of(run));
        when(payrollRunRepository.findByClientId(eq(clientId), any(Pageable.class))).thenReturn(page);

        Page<PayrollRun> result = payrollService.getPayrollRunsByClient(clientId, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getPayrollRunById
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getPayrollRunById_returnsRun_whenFound() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 1, 20));
        when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.of(run));

        PayrollRun result = payrollService.getPayrollRunById(payrollRunId);

        assertThat(result.getId()).isEqualTo(payrollRunId);
        assertThat(result.getStatus()).isEqualTo(PayrollStatusEnum.DRAFT);
    }

    @Test
    void getPayrollRunById_throwsEntityNotFound_whenMissing() {
        when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.getPayrollRunById(payrollRunId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // createPayrollRun
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void createPayrollRun_validatesAndSaves() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 3, 15),
                LocalDate.of(2025, 3, 20));
        when(payrollRunRepository.save(run)).thenReturn(run);

        PayrollRun result = payrollService.createPayrollRun(run);

        verify(payrollRunRepository).save(run);
        assertThat(result.getStatus()).isEqualTo(PayrollStatusEnum.DRAFT);
    }

    @Test
    void createPayrollRun_throwsException_whenPayPeriodIsNull() {
        PayrollRun run = new PayrollRun();
        run.setClient(client);
        run.setPayDate(LocalDate.of(2025, 3, 20));
        // payPeriod left null

        assertThatThrownBy(() -> payrollService.createPayrollRun(run))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pay period");
    }

    @Test
    void createPayrollRun_throwsException_whenStartAfterEnd() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 3, 20), // start AFTER end
                LocalDate.of(2025, 3, 10),
                LocalDate.of(2025, 3, 25));

        assertThatThrownBy(() -> payrollService.createPayrollRun(run))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start date must be before end date");
    }

    @Test
    void createPayrollRun_throwsException_whenPayDateBeforePeriodEnd() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 3, 15),
                LocalDate.of(2025, 3, 10)); // pay date BEFORE period end

        assertThatThrownBy(() -> payrollService.createPayrollRun(run))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pay date must be");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // processPayrollRun
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void processPayrollRun_transitionsDraftToCompleted() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 4, 15),
                LocalDate.of(2025, 4, 20));
        when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.of(run));
        when(employeeRepository.findByClientIdAndStatus(clientId, EmployeeStatusEnum.ACTIVE))
                .thenReturn(List.of());

        PayrollRun result = payrollService.processPayrollRun(payrollRunId);

        assertThat(result.getStatus()).isEqualTo(PayrollStatusEnum.COMPLETED);
        assertThat(result.getProcessedAt()).isNotNull();
    }

    @Test
    void processPayrollRun_setsEmployeeCount() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 4, 15),
                LocalDate.of(2025, 4, 20));
        when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.of(run));

        EmployeeEntity emp1 = new EmployeeEntity();
        EmployeeEntity emp2 = new EmployeeEntity();
        when(employeeRepository.findByClientIdAndStatus(clientId, EmployeeStatusEnum.ACTIVE))
                .thenReturn(List.of(emp1, emp2));

        PayrollRun result = payrollService.processPayrollRun(payrollRunId);

        assertThat(result.getEmployeeCount()).isEqualTo(2);
    }

    @Test
    void processPayrollRun_throwsNotFound_whenMissing() {
        when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.processPayrollRun(payrollRunId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void processPayrollRun_throwsIllegalState_whenNotDraft() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 4, 15),
                LocalDate.of(2025, 4, 20));
        run.setStatus(PayrollStatusEnum.COMPLETED);
        when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> payrollService.processPayrollRun(payrollRunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // approvePayrollRun
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void approvePayrollRun_transitionsCompletedToApproved() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 5, 1),
                LocalDate.of(2025, 5, 15),
                LocalDate.of(2025, 5, 20));
        run.setStatus(PayrollStatusEnum.COMPLETED);
        when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.of(run));

        PayrollRun result = payrollService.approvePayrollRun(payrollRunId, "manager@company.com");

        assertThat(result.getStatus()).isEqualTo(PayrollStatusEnum.APPROVED);
        assertThat(result.getApprovedBy()).isEqualTo("manager@company.com");
        assertThat(result.getApprovedAt()).isNotNull();
    }

    @Test
    void approvePayrollRun_throwsIllegalState_whenNotCompleted() {
        PayrollRun run = buildDraftRun(
                LocalDate.of(2025, 5, 1),
                LocalDate.of(2025, 5, 15),
                LocalDate.of(2025, 5, 20));
        run.setStatus(PayrollStatusEnum.DRAFT);
        when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> payrollService.approvePayrollRun(payrollRunId, "approver"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void approvePayrollRun_throwsNotFound_whenMissing() {
        when(payrollRunRepository.findById(payrollRunId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.approvePayrollRun(payrollRunId, "approver"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
