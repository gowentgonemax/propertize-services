package com.propertize.payroll.service;

import com.propertize.payroll.dto.compensation.request.CompensationCreateRequest;
import com.propertize.payroll.dto.compensation.request.CompensationUpdateRequest;
import com.propertize.payroll.dto.compensation.response.CompensationHistoryResponse;
import com.propertize.payroll.dto.compensation.response.CompensationResponse;
import com.propertize.payroll.entity.CompensationEntity;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.enums.CompensationStatusEnum;
import com.propertize.payroll.enums.PayFrequencyEnum;
import com.propertize.payroll.exception.ResourceNotFoundException;
import com.propertize.payroll.exception.ValidationException;
import com.propertize.payroll.repository.CompensationRepository;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing employee compensation.
 * Handles salary, hourly rates, raises, and compensation history.
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CompensationService {

    private final CompensationRepository compensationRepository;
    private final EmployeeEntityRepository employeeRepository;

    /**
     * Create a new compensation record for an employee.
     *
     * @param request Compensation creation request
     * @return Created compensation response
     */
    public CompensationResponse createCompensation(CompensationCreateRequest request) {
        log.info("Creating compensation for employee: {}", request.getEmployeeId());

        // Validate employee exists
        EmployeeEntity employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + request.getEmployeeId()));

        // Validate compensation data
        validateCompensationRequest(request);

        // Mark previous compensation as inactive if this is current
        if (Boolean.TRUE.equals(request.getIsCurrentCompensation())) {
            deactivatePreviousCompensation(employee.getId());
        }

        // Create compensation entity
        CompensationEntity compensation = CompensationEntity.builder()
                .employee(employee)
                .compensationType(request.getCompensationType())
                .status(CompensationStatusEnum.ACTIVE)
                .payFrequency(request.getPayFrequency())
                .hourlyRate(request.getHourlyRate())
                .annualSalary(request.getAnnualSalary())
                .standardHoursPerPeriod(request.getStandardHoursPerPeriod())
                .overtimeMultiplier(request.getOvertimeMultiplier() != null
                        ? request.getOvertimeMultiplier()
                        : new BigDecimal("1.50"))
                .doubleTimeMultiplier(request.getDoubleTimeMultiplier() != null
                        ? request.getDoubleTimeMultiplier()
                        : new BigDecimal("2.00"))
                .effectiveDate(request.getEffectiveDate())
                .endDate(request.getEndDate())
                .changeReason(request.getChangeReason())
                .notes(request.getNotes())
                .isCurrent(request.getIsCurrentCompensation())
                .build();

        // Calculate pay rate per period
        compensation.setPayRatePerPeriod(calculatePayRatePerPeriod(compensation));

        CompensationEntity saved = compensationRepository.save(compensation);
        log.info("Compensation created successfully: {}", saved.getId());

        return mapToResponse(saved);
    }

    /**
     * Update existing compensation record.
     *
     * @param id      Compensation ID
     * @param request Update request
     * @return Updated compensation response
     */
    public CompensationResponse updateCompensation(UUID id, CompensationUpdateRequest request) {
        log.info("Updating compensation: {}", id);

        CompensationEntity compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compensation not found with ID: " + id));

        // Update fields if provided
        if (request.getStatus() != null) {
            compensation.setStatus(request.getStatus());
        }
        if (request.getPayFrequency() != null) {
            compensation.setPayFrequency(request.getPayFrequency());
        }
        if (request.getHourlyRate() != null) {
            compensation.setHourlyRate(request.getHourlyRate());
        }
        if (request.getAnnualSalary() != null) {
            compensation.setAnnualSalary(request.getAnnualSalary());
        }
        if (request.getStandardHoursPerPeriod() != null) {
            compensation.setStandardHoursPerPeriod(request.getStandardHoursPerPeriod());
        }
        if (request.getOvertimeMultiplier() != null) {
            compensation.setOvertimeMultiplier(request.getOvertimeMultiplier());
        }
        if (request.getDoubleTimeMultiplier() != null) {
            compensation.setDoubleTimeMultiplier(request.getDoubleTimeMultiplier());
        }
        if (request.getEndDate() != null) {
            compensation.setEndDate(request.getEndDate());
        }
        if (request.getChangeReason() != null) {
            compensation.setChangeReason(request.getChangeReason());
        }
        if (request.getNotes() != null) {
            compensation.setNotes(request.getNotes());
        }

        // Recalculate pay rate per period
        compensation.setPayRatePerPeriod(calculatePayRatePerPeriod(compensation));

        CompensationEntity updated = compensationRepository.save(compensation);
        log.info("Compensation updated successfully: {}", id);

        return mapToResponse(updated);
    }

    /**
     * Get compensation by ID.
     *
     * @param id Compensation ID
     * @return Compensation response
     */
    @Transactional(readOnly = true)
    public CompensationResponse getCompensationById(UUID id) {
        log.debug("Fetching compensation: {}", id);

        CompensationEntity compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compensation not found with ID: " + id));

        return mapToResponse(compensation);
    }

    /**
     * Get current compensation for an employee.
     *
     * @param employeeId Employee ID
     * @return Current compensation response
     */
    @Transactional(readOnly = true)
    public CompensationResponse getCurrentCompensation(UUID employeeId) {
        log.debug("Fetching current compensation for employee: {}", employeeId);

        // Validate employee exists
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + employeeId));

        CompensationEntity compensation = compensationRepository
                .findByEmployeeIdAndIsCurrent(employeeId, true)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No current compensation found for employee: " + employeeId));

        return mapToResponse(compensation);
    }

    /**
     * Get compensation history for an employee.
     *
     * @param employeeId Employee ID
     * @return List of compensation history
     */
    @Transactional(readOnly = true)
    public List<CompensationHistoryResponse> getCompensationHistory(UUID employeeId) {
        log.debug("Fetching compensation history for employee: {}", employeeId);

        // Validate employee exists
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + employeeId));

        List<CompensationEntity> history = compensationRepository
                .findByEmployeeIdOrderByEffectiveDateDesc(employeeId);

        return history.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Deactivate a compensation record.
     *
     * @param id        Compensation ID
     * @param endDate   End date for the compensation
     * @param reason    Reason for deactivation
     */
    public void deactivateCompensation(UUID id, LocalDate endDate, String reason) {
        log.info("Deactivating compensation: {}", id);

        CompensationEntity compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compensation not found with ID: " + id));

        compensation.setStatus(CompensationStatusEnum.INACTIVE);
        compensation.setEndDate(endDate);
        compensation.setChangeReason(reason);
        compensation.setIsCurrent(false);

        compensationRepository.save(compensation);
        log.info("Compensation deactivated successfully: {}", id);
    }

    /**
     * Delete a compensation record (soft delete).
     *
     * @param id Compensation ID
     */
    public void deleteCompensation(UUID id) {
        log.info("Deleting compensation: {}", id);

        CompensationEntity compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compensation not found with ID: " + id));

        // Soft delete
        compensation.setStatus(CompensationStatusEnum.INACTIVE);
        compensation.setIsCurrent(false);
        compensationRepository.save(compensation);

        log.info("Compensation deleted successfully: {}", id);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Validate compensation request data.
     */
    private void validateCompensationRequest(CompensationCreateRequest request) {
        // Validate that either hourlyRate or annualSalary is provided (not both)
        boolean hasHourlyRate = request.getHourlyRate() != null &&
                request.getHourlyRate().compareTo(BigDecimal.ZERO) > 0;
        boolean hasAnnualSalary = request.getAnnualSalary() != null &&
                request.getAnnualSalary().compareTo(BigDecimal.ZERO) > 0;

        if (!hasHourlyRate && !hasAnnualSalary) {
            throw new ValidationException(
                    "Either hourly rate or annual salary must be provided");
        }

        if (hasHourlyRate && hasAnnualSalary) {
            throw new ValidationException(
                    "Cannot specify both hourly rate and annual salary");
        }

        // Validate effective date is not too far in the past
        if (request.getEffectiveDate().isBefore(LocalDate.now().minusYears(1))) {
            throw new ValidationException(
                    "Effective date cannot be more than 1 year in the past");
        }

        // Validate end date is after effective date
        if (request.getEndDate() != null &&
                request.getEndDate().isBefore(request.getEffectiveDate())) {
            throw new ValidationException(
                    "End date must be after effective date");
        }
    }

    /**
     * Deactivate previous compensation records for the employee.
     */
    private void deactivatePreviousCompensation(UUID employeeId) {
        List<CompensationEntity> activeCompensations = compensationRepository
                .findByEmployeeIdAndIsCurrent(employeeId, true)
                .stream()
                .toList();

        activeCompensations.forEach(comp -> {
            comp.setIsCurrent(false);
            comp.setEndDate(LocalDate.now().minusDays(1));
        });

        if (!activeCompensations.isEmpty()) {
            compensationRepository.saveAll(activeCompensations);
            log.info("Deactivated {} previous compensation record(s) for employee: {}",
                    activeCompensations.size(), employeeId);
        }
    }

    /**
     * Calculate pay rate per period based on salary/hourly rate and frequency.
     */
    private BigDecimal calculatePayRatePerPeriod(CompensationEntity compensation) {
        if (compensation.getAnnualSalary() != null &&
                compensation.getAnnualSalary().compareTo(BigDecimal.ZERO) > 0) {
            // Calculate from annual salary
            int periodsPerYear = getPeriodsPerYear(compensation.getPayFrequency());
            return compensation.getAnnualSalary()
                    .divide(BigDecimal.valueOf(periodsPerYear), 2, RoundingMode.HALF_UP);
        } else if (compensation.getHourlyRate() != null &&
                compensation.getHourlyRate().compareTo(BigDecimal.ZERO) > 0 &&
                compensation.getStandardHoursPerPeriod() != null) {
            // Calculate from hourly rate
            return compensation.getHourlyRate()
                    .multiply(compensation.getStandardHoursPerPeriod())
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get number of pay periods per year based on pay frequency.
     */
    private int getPeriodsPerYear(PayFrequencyEnum frequency) {
        return switch (frequency) {
            case WEEKLY -> 52;
            case BI_WEEKLY -> 26;
            case SEMI_MONTHLY -> 24;
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case ANNUALLY -> 1;
        };
    }

    /**
     * Map entity to response DTO.
     */
    private CompensationResponse mapToResponse(CompensationEntity entity) {
        return CompensationResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee().getId())
                .employeeNumber(entity.getEmployee().getEmployeeNumber())
                .employeeFullName(entity.getEmployee().getFirstName() + " " +
                        entity.getEmployee().getLastName())
                .compensationType(entity.getCompensationType())
                .status(entity.getStatus())
                .payFrequency(entity.getPayFrequency())
                .hourlyRate(entity.getHourlyRate())
                .annualSalary(entity.getAnnualSalary())
                .payRatePerPeriod(entity.getPayRatePerPeriod())
                .standardHoursPerPeriod(entity.getStandardHoursPerPeriod())
                .overtimeMultiplier(entity.getOvertimeMultiplier())
                .doubleTimeMultiplier(entity.getDoubleTimeMultiplier())
                .effectiveDate(entity.getEffectiveDate())
                .endDate(entity.getEndDate())
                .changeReason(entity.getChangeReason())
                .notes(entity.getNotes())
                .isCurrentCompensation(entity.getIsCurrent())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    /**
     * Map entity to history response DTO.
     */
    private CompensationHistoryResponse mapToHistoryResponse(CompensationEntity entity) {
        return CompensationHistoryResponse.builder()
                .id(entity.getId())
                .compensationType(entity.getCompensationType().name())
                .status(entity.getStatus().name())
                .hourlyRate(entity.getHourlyRate())
                .annualSalary(entity.getAnnualSalary())
                .payFrequency(entity.getPayFrequency().name())
                .effectiveDate(entity.getEffectiveDate())
                .endDate(entity.getEndDate())
                .changeReason(entity.getChangeReason())
                .isCurrent(entity.getIsCurrent())
                .build();
    }
}
