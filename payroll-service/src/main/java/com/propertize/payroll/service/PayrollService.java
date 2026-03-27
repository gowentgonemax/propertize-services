package com.propertize.payroll.service;

import com.propertize.payroll.config.CorrelationIdUtil;
import com.propertize.payroll.enums.EmployeeStatusEnum;
import com.propertize.payroll.enums.PayrollStatusEnum;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.entity.PayrollRun;

import com.propertize.payroll.repository.PayrollRunRepository;
import com.propertize.payroll.repository.EmployeeEntityRepository;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {
    private final PayrollRunRepository payrollRunRepository;
    private final EmployeeEntityRepository employeeRepository;

    public Page<PayrollRun> getPayrollRunsByClient(UUID clientId, Pageable pageable) {
        return payrollRunRepository.findByClientId(clientId, pageable);
    }

    public List<PayrollRun> getPayrollRunsByDateRange(UUID clientId, LocalDate startDate, LocalDate endDate) {
        return payrollRunRepository.findByClientIdAndPayPeriodStartBetweenAndPayPeriodEndBetween(
                clientId, startDate, endDate, startDate, endDate);
    }

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
