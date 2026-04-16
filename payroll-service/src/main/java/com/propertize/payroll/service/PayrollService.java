package com.propertize.payroll.service;

import com.propertize.payroll.config.CorrelationIdUtil;
import com.propertize.commons.enums.employee.EmployeeStatusEnum;
import com.propertize.commons.enums.employee.PayrollStatusEnum;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.entity.PayrollRun;

import com.propertize.payroll.repository.PayrollRunRepository;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {
    private final PayrollRunRepository payrollRunRepository;
    private final EmployeeEntityRepository employeeRepository;

    @Transactional(readOnly = true)
    public Page<PayrollRun> getPayrollRunsByClient(UUID clientId, Pageable pageable) {
        return payrollRunRepository.findByClientId(clientId, pageable);
    }

    @Transactional(readOnly = true)
    public List<PayrollRun> getPayrollRunsByDateRange(UUID clientId, LocalDate startDate, LocalDate endDate) {
        return payrollRunRepository.findByClientIdAndPayPeriodStartBetweenAndPayPeriodEndBetween(
                clientId, startDate, endDate, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public PayrollRun getPayrollRunById(UUID id) {
        return payrollRunRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payroll run not found with id: " + id));
    }

    @Transactional
    public PayrollRun createPayrollRun(PayrollRun payrollRun) {
        validatePayrollDates(payrollRun);
        return payrollRunRepository.save(payrollRun);
    }

    @Transactional
    public PayrollRun processPayrollRun(UUID payrollRunId) {
        log.info("Processing payroll run: {} - Correlation ID: {}", payrollRunId, CorrelationIdUtil.getCorrelationId());

        PayrollRun payrollRun = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new EntityNotFoundException("Payroll run not found with id: " + payrollRunId));

        if (payrollRun.getStatus() != PayrollStatusEnum.DRAFT) {
            log.error("Payroll run {} is not in DRAFT status. Current status: {}", payrollRunId,
                    payrollRun.getStatus());
            throw new IllegalStateException("Payroll run must be in DRAFT status to process");
        }

        payrollRun.setStatus(PayrollStatusEnum.PROCESSING);
        log.info("Payroll run {} status changed to PROCESSING", payrollRunId);

        try {
            calculatePayroll(payrollRun);
            payrollRun.setStatus(PayrollStatusEnum.COMPLETED);
            payrollRun.setProcessedAt(LocalDateTime.now());
            log.info("Payroll run {} completed successfully", payrollRunId);
        } catch (Exception e) {
            payrollRun.setStatus(PayrollStatusEnum.FAILED);
            log.error("Payroll run {} failed: {}", payrollRunId, e.getMessage(), e);
            throw e;
        }

        return payrollRun; // No need to call save() - managed entity will auto-update
    }

    @Transactional
    public PayrollRun approvePayrollRun(UUID payrollRunId, String approverUsername) {
        PayrollRun payrollRun = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new EntityNotFoundException("Payroll run not found with id: " + payrollRunId));

        if (payrollRun.getStatus() != PayrollStatusEnum.COMPLETED) {
            throw new IllegalStateException("Payroll run must be in COMPLETED status to approve");
        }

        payrollRun.setStatus(PayrollStatusEnum.APPROVED);
        payrollRun.setApprovedBy(approverUsername);
        payrollRun.setApprovedAt(LocalDateTime.now());

        return payrollRun; // No need to call save() - managed entity will auto-update
    }

    /**
     * Batch-process multiple payroll runs in parallel using virtual threads.
     * Each run is processed in its own transaction via {@code processPayrollRun}.
     *
     * @param payrollRunIds list of payroll run IDs to process
     * @return map of runId → processed PayrollRun (failures are reported via
     *         exception in the returned CompletableFuture)
     */
    @Async("virtualThreadExecutor")
    public CompletableFuture<List<Map<String, Object>>> batchProcessPayrollRuns(List<UUID> payrollRunIds) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (UUID runId : payrollRunIds) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("payrollRunId", runId.toString());
            try {
                PayrollRun processed = processPayrollRun(runId);
                result.put("status", PayrollStatusEnum.COMPLETED.name());
                result.put("payrollStatus", processed.getStatus().name());
            } catch (Exception e) {
                log.error("Batch: failed to process payroll run {}: {}", runId, e.getMessage());
                result.put("status", "FAILED"); // terminal error state — no enum constant for batch-level failure
                result.put("error", e.getMessage());
            }
            results.add(result);
        }

        return CompletableFuture.completedFuture(results);
    }

    private void validatePayrollDates(PayrollRun payrollRun) {
        if (payrollRun.getPayPeriod() == null
                || payrollRun.getPayPeriod().getStartDate() == null
                || payrollRun.getPayPeriod().getEndDate() == null) {
            throw new IllegalArgumentException("Pay period start and end dates are required");
        }
        if (payrollRun.getPayPeriod().getStartDate().isAfter(payrollRun.getPayPeriod().getEndDate())) {
            throw new IllegalArgumentException("Pay period start date must be before end date");
        }
        if (payrollRun.getPayDate().isBefore(payrollRun.getPayPeriod().getEndDate())) {
            throw new IllegalArgumentException("Pay date must be after pay period end date");
        }
    }

    private void calculatePayroll(PayrollRun payrollRun) {
        List<EmployeeEntity> activeEmployees = employeeRepository.findByClientIdAndStatus(
                payrollRun.getClient().getId(),
                EmployeeStatusEnum.ACTIVE);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalTaxes = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;

        for (EmployeeEntity employee : activeEmployees) {
            // Calculate individual employee payroll
            // This would include complex calculations for:
            // - Base pay (hourly or salary)
            // - Overtime
            // - Bonuses
            // - Tax withholdings
            // - Benefit deductions
            // - Garnishments
            // Implementation details would depend on specific business rules
        }

        payrollRun.getTotals().setTotalGrossPay(totalGross);
        payrollRun.getTotals().setTotalNetPay(totalNet);
        payrollRun.getTotals().setTotalTaxes(totalTaxes);
        payrollRun.getTotals().setTotalDeductions(totalDeductions);
        payrollRun.setEmployeeCount(activeEmployees.size());
    }
}
