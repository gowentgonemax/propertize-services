package com.propertize.payroll.service;

import com.propertize.payroll.entity.BenefitEnrollment;
import com.propertize.payroll.entity.BenefitPlan;
import com.propertize.payroll.enums.EnrollmentStatusEnum;
import com.propertize.payroll.enums.PlanStatusEnum;
import com.propertize.payroll.repository.BenefitEnrollmentRepository;
import com.propertize.payroll.repository.BenefitPlanRepository;
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

/**
 * Service for managing employee benefits enrollment and benefit plans.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitService {

    private final BenefitPlanRepository benefitPlanRepository;
    private final BenefitEnrollmentRepository benefitEnrollmentRepository;

    // ==================== Benefit Plan Operations ====================

    /**
     * Get all benefit plans for a client
     */
    public Page<BenefitPlan> getAllBenefitPlans(UUID clientId, Pageable pageable) {
        log.info("Fetching all benefit plans for client: {}", clientId);
        return benefitPlanRepository.findByClientId(clientId, pageable);
    }

    /**
     * Get active benefit plans for a client
     */
    public List<BenefitPlan> getActiveBenefitPlans(UUID clientId) {
        log.info("Fetching active benefit plans for client: {}", clientId);
        return benefitPlanRepository.findByClientIdAndStatus(clientId, PlanStatusEnum.ACTIVE);
    }

    /**
     * Get a benefit plan by ID
     */
    public BenefitPlan getBenefitPlanById(UUID id) {
        return benefitPlanRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Benefit plan not found with id: " + id));
    }

    /**
     * Create a new benefit plan
     */
    @Transactional
    public BenefitPlan createBenefitPlan(BenefitPlan plan) {
        log.info("Creating new benefit plan: {} for client: {}", plan.getPlanName(), plan.getClient().getId());
        validateBenefitPlan(plan);
        plan.setStatus(PlanStatusEnum.ACTIVE);
        return benefitPlanRepository.save(plan);
    }

    /**
     * Update an existing benefit plan
     */
    @Transactional
    public BenefitPlan updateBenefitPlan(UUID id, BenefitPlan updatedPlan) {
        BenefitPlan existingPlan = getBenefitPlanById(id);
        log.info("Updating benefit plan: {}", id);

        existingPlan.setPlanName(updatedPlan.getPlanName());
        existingPlan.setDescription(updatedPlan.getDescription());
        existingPlan.setEmployeeCost(updatedPlan.getEmployeeCost());
        existingPlan.setEmployerCost(updatedPlan.getEmployerCost());
        existingPlan.setPlanStartDate(updatedPlan.getPlanStartDate());
        existingPlan.setPlanEndDate(updatedPlan.getPlanEndDate());

        return benefitPlanRepository.save(existingPlan);
    }

    /**
     * Deactivate a benefit plan
     */
    @Transactional
    public void deactivateBenefitPlan(UUID id) {
        BenefitPlan plan = getBenefitPlanById(id);
        log.info("Deactivating benefit plan: {}", id);
        plan.setStatus(PlanStatusEnum.INACTIVE);
        benefitPlanRepository.save(plan);
    }

    // ==================== Benefit Enrollment Operations ====================

    /**
     * Get all enrollments for an employee
     */
    public List<BenefitEnrollment> getEmployeeBenefitEnrollments(String employeeId) {
        log.info("Fetching benefit enrollments for employee: {}", employeeId);
        return benefitEnrollmentRepository.findByEmployeeId(employeeId);
    }

    /**
     * Get active enrollments for an employee
     */
    public List<BenefitEnrollment> getActiveEmployeeEnrollments(String employeeId) {
        log.info("Fetching active benefit enrollments for employee: {}", employeeId);
        return benefitEnrollmentRepository.findByEmployeeIdAndStatus(employeeId, EnrollmentStatusEnum.ACTIVE);
    }

    /**
     * Get enrollment by ID
     */
    public BenefitEnrollment getEnrollmentById(UUID id) {
        return benefitEnrollmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Benefit enrollment not found with id: " + id));
    }

    /**
     * Enroll an employee in a benefit plan
     */
    @Transactional
    public BenefitEnrollment enrollEmployee(BenefitEnrollment enrollment) {
        log.info("Enrolling employee {} in benefit plan {}",
            enrollment.getEmployee().getId(), enrollment.getBenefitPlan().getId());

        // Validate the enrollment
        validateEnrollment(enrollment);

        // Check for existing enrollment in same plan
        List<BenefitEnrollment> existingEnrollments = benefitEnrollmentRepository
            .findByEmployeeIdAndBenefitPlanId(
                enrollment.getEmployee().getId().toString(),
                enrollment.getBenefitPlan().getId());

        for (BenefitEnrollment existing : existingEnrollments) {
            if (existing.getStatus() == EnrollmentStatusEnum.ACTIVE) {
                throw new IllegalStateException("Employee is already enrolled in this benefit plan");
            }
        }

        enrollment.setStatus(EnrollmentStatusEnum.ACTIVE);
        enrollment.setEnrollmentDate(LocalDate.now());

        return benefitEnrollmentRepository.save(enrollment);
    }

    /**
     * Update an enrollment
     */
    @Transactional
    public BenefitEnrollment updateEnrollment(UUID id, BenefitEnrollment updatedEnrollment) {
        BenefitEnrollment existing = getEnrollmentById(id);
        log.info("Updating enrollment: {}", id);

        existing.setCoverageLevel(updatedEnrollment.getCoverageLevel());
        existing.setEffectiveDate(updatedEnrollment.getEffectiveDate());

        return benefitEnrollmentRepository.save(existing);
    }

    /**
     * Terminate an enrollment
     */
    @Transactional
    public void terminateEnrollment(UUID id, LocalDate terminationDate, String reason) {
        BenefitEnrollment enrollment = getEnrollmentById(id);
        log.info("Terminating enrollment: {} on date: {}", id, terminationDate);

        enrollment.setStatus(EnrollmentStatusEnum.TERMINATED);
        enrollment.setTerminationDate(terminationDate);
        enrollment.setTerminationReason(reason);

        benefitEnrollmentRepository.save(enrollment);
    }

    /**
     * Cancel an enrollment (before effective date)
     */
    @Transactional
    public void cancelEnrollment(UUID id) {
        BenefitEnrollment enrollment = getEnrollmentById(id);

        if (enrollment.getEffectiveDate() != null &&
            !enrollment.getEffectiveDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Cannot cancel enrollment after effective date");
        }

        log.info("Canceling enrollment: {}", id);
        enrollment.setStatus(EnrollmentStatusEnum.CANCELLED);
        benefitEnrollmentRepository.save(enrollment);
    }

    // ==================== Calculation Methods ====================

    /**
     * Calculate total benefit deductions for an employee
     */
    public BigDecimal calculateTotalEmployeeDeductions(String employeeId) {
        List<BenefitEnrollment> activeEnrollments = getActiveEmployeeEnrollments(employeeId);

        return activeEnrollments.stream()
            .filter(e -> e.getBenefitPlan() != null)
            .map(e -> e.getBenefitPlan().getEmployeeCost() != null ?
                e.getBenefitPlan().getEmployeeCost() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total employer contributions for an employee
     */
    public BigDecimal calculateTotalEmployerContributions(String employeeId) {
        List<BenefitEnrollment> activeEnrollments = getActiveEmployeeEnrollments(employeeId);

        return activeEnrollments.stream()
            .filter(e -> e.getBenefitPlan() != null)
            .map(e -> e.getBenefitPlan().getEmployerCost() != null ?
                e.getBenefitPlan().getEmployerCost() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get enrollments by plan
     */
    public List<BenefitEnrollment> getEnrollmentsByPlan(UUID planId) {
        log.info("Fetching enrollments for plan: {}", planId);
        return benefitEnrollmentRepository.findByBenefitPlanId(planId);
    }

    /**
     * Get active enrollments count by plan
     */
    public long getActiveEnrollmentCountByPlan(UUID planId) {
        return benefitEnrollmentRepository.countByBenefitPlanIdAndStatus(planId, EnrollmentStatusEnum.ACTIVE);
    }

    // ==================== Validation Methods ====================

    private void validateBenefitPlan(BenefitPlan plan) {
        if (plan.getPlanName() == null || plan.getPlanName().isBlank()) {
            throw new IllegalArgumentException("Plan name is required");
        }
        if (plan.getClient() == null) {
            throw new IllegalArgumentException("Client is required");
        }
        if (plan.getBenefitType() == null) {
            throw new IllegalArgumentException("Benefit type is required");
        }
    }

    private void validateEnrollment(BenefitEnrollment enrollment) {
        if (enrollment.getEmployee() == null) {
            throw new IllegalArgumentException("Employee is required");
        }
        if (enrollment.getBenefitPlan() == null) {
            throw new IllegalArgumentException("Benefit plan is required");
        }

        BenefitPlan plan = enrollment.getBenefitPlan();
        if (plan.getStatus() != PlanStatusEnum.ACTIVE) {
            throw new IllegalStateException("Cannot enroll in an inactive benefit plan");
        }
    }
}
