package com.propertize.payroll.service;

import com.propertize.payroll.entity.BenefitEnrollment;
import com.propertize.payroll.entity.BenefitPlan;
import com.propertize.payroll.entity.Client;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.enums.BenefitTypeEnum;
import com.propertize.payroll.enums.CoverageLevelEnum;
import com.propertize.payroll.enums.EnrollmentStatusEnum;
import com.propertize.payroll.enums.PlanStatusEnum;
import com.propertize.payroll.repository.BenefitEnrollmentRepository;
import com.propertize.payroll.repository.BenefitPlanRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    @Mock
    BenefitPlanRepository benefitPlanRepository;
    @Mock
    BenefitEnrollmentRepository benefitEnrollmentRepository;

    @InjectMocks
    BenefitService benefitService;

    UUID clientId;
    UUID planId;
    UUID enrollmentId;
    UUID employeeUuid;
    Client client;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        planId = UUID.randomUUID();
        enrollmentId = UUID.randomUUID();
        employeeUuid = UUID.randomUUID();

        client = new Client();
        client.setId(clientId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private BenefitPlan buildActivePlan() {
        BenefitPlan plan = new BenefitPlan();
        plan.setId(planId);
        plan.setClient(client);
        plan.setPlanName("Medical Plan");
        plan.setBenefitType(BenefitTypeEnum.HEALTH_INSURANCE);
        plan.setEmployeeCost(new BigDecimal("200.00"));
        plan.setEmployerCost(new BigDecimal("400.00"));
        plan.setStatus(PlanStatusEnum.ACTIVE);
        return plan;
    }

    private BenefitEnrollment buildEnrollment(EmployeeEntity employee, BenefitPlan plan,
            EnrollmentStatusEnum status) {
        BenefitEnrollment enrollment = new BenefitEnrollment();
        enrollment.setId(enrollmentId);
        enrollment.setEmployee(employee);
        enrollment.setBenefitPlan(plan);
        enrollment.setStatus(status);
        enrollment.setEnrollmentDate(LocalDate.of(2026, 1, 1));
        enrollment.setEffectiveDate(LocalDate.of(2026, 2, 1));
        enrollment.setCoverageLevel(CoverageLevelEnum.EMPLOYEE_ONLY);
        return enrollment;
    }

    private EmployeeEntity buildEmployee() {
        EmployeeEntity emp = new EmployeeEntity();
        emp.setId(employeeUuid);
        return emp;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getAllBenefitPlans
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getAllBenefitPlans_returnsPage() {
        BenefitPlan plan = buildActivePlan();
        Page<BenefitPlan> page = new PageImpl<>(List.of(plan));
        when(benefitPlanRepository.findByClientId(eq(clientId), any(Pageable.class))).thenReturn(page);

        Page<BenefitPlan> result = benefitService.getAllBenefitPlans(clientId, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPlanName()).isEqualTo("Medical Plan");
    }

    @Test
    void getAllBenefitPlans_returnsEmptyPage_whenNonePlans() {
        when(benefitPlanRepository.findByClientId(eq(clientId), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<BenefitPlan> result = benefitService.getAllBenefitPlans(clientId, Pageable.unpaged());

        assertThat(result.getTotalElements()).isZero();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getActiveBenefitPlans
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getActiveBenefitPlans_returnsList_ofActivePlans() {
        BenefitPlan plan = buildActivePlan();
        when(benefitPlanRepository.findByClientIdAndStatus(clientId, PlanStatusEnum.ACTIVE))
                .thenReturn(List.of(plan));

        List<BenefitPlan> result = benefitService.getActiveBenefitPlans(clientId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PlanStatusEnum.ACTIVE);
    }

    @Test
    void getActiveBenefitPlans_returnsEmptyList_whenNoneActive() {
        when(benefitPlanRepository.findByClientIdAndStatus(clientId, PlanStatusEnum.ACTIVE))
                .thenReturn(List.of());

        List<BenefitPlan> result = benefitService.getActiveBenefitPlans(clientId);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getBenefitPlanById
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getBenefitPlanById_returnsPlan_whenFound() {
        when(benefitPlanRepository.findById(planId)).thenReturn(Optional.of(buildActivePlan()));

        BenefitPlan result = benefitService.getBenefitPlanById(planId);

        assertThat(result.getId()).isEqualTo(planId);
    }

    @Test
    void getBenefitPlanById_throwsEntityNotFound_whenMissing() {
        when(benefitPlanRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benefitService.getBenefitPlanById(planId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(planId.toString());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // createBenefitPlan
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void createBenefitPlan_savesAndSetsStatusActive() {
        BenefitPlan plan = buildActivePlan();
        plan.setStatus(null); // simulate incoming plan without status
        when(benefitPlanRepository.save(plan)).thenReturn(plan);

        BenefitPlan result = benefitService.createBenefitPlan(plan);

        assertThat(result.getStatus()).isEqualTo(PlanStatusEnum.ACTIVE);
        verify(benefitPlanRepository).save(plan);
    }

    @Test
    void createBenefitPlan_throwsIllegalArgument_whenPlanNameBlank() {
        BenefitPlan plan = buildActivePlan();
        plan.setPlanName("");

        assertThatThrownBy(() -> benefitService.createBenefitPlan(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan name is required");
    }

    @Test
    void createBenefitPlan_throwsWhenClientNull() {
        BenefitPlan plan = buildActivePlan();
        plan.setClient(null);

        // Service does not validate null client — NPE thrown when getId() is called
        assertThatThrownBy(() -> benefitService.createBenefitPlan(plan))
                .isInstanceOf(Exception.class);
    }

    @Test
    void createBenefitPlan_throwsIllegalArgument_whenBenefitTypeNull() {
        BenefitPlan plan = buildActivePlan();
        plan.setBenefitType(null);

        assertThatThrownBy(() -> benefitService.createBenefitPlan(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Benefit type is required");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // updateBenefitPlan
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void updateBenefitPlan_updatesFieldsAndSaves() {
        BenefitPlan existing = buildActivePlan();
        when(benefitPlanRepository.findById(planId)).thenReturn(Optional.of(existing));
        when(benefitPlanRepository.save(existing)).thenReturn(existing);

        BenefitPlan updated = new BenefitPlan();
        updated.setPlanName("Updated Plan");
        updated.setDescription("New description");
        updated.setEmployeeCost(new BigDecimal("250.00"));
        updated.setEmployerCost(new BigDecimal("500.00"));
        updated.setPlanStartDate(LocalDate.of(2026, 1, 1));
        updated.setPlanEndDate(LocalDate.of(2026, 12, 31));

        BenefitPlan result = benefitService.updateBenefitPlan(planId, updated);

        assertThat(result.getPlanName()).isEqualTo("Updated Plan");
        assertThat(result.getDescription()).isEqualTo("New description");
        assertThat(result.getEmployeeCost()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(result.getEmployerCost()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(benefitPlanRepository).save(existing);
    }

    @Test
    void updateBenefitPlan_throwsEntityNotFound_whenPlanMissing() {
        when(benefitPlanRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benefitService.updateBenefitPlan(planId, new BenefitPlan()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // deactivateBenefitPlan
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void deactivateBenefitPlan_setsStatusInactive() {
        BenefitPlan plan = buildActivePlan();
        when(benefitPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        benefitService.deactivateBenefitPlan(planId);

        assertThat(plan.getStatus()).isEqualTo(PlanStatusEnum.INACTIVE);
        verify(benefitPlanRepository).save(plan);
    }

    @Test
    void deactivateBenefitPlan_throwsEntityNotFound_whenPlanMissing() {
        when(benefitPlanRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benefitService.deactivateBenefitPlan(planId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getEmployeeBenefitEnrollments
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getEmployeeBenefitEnrollments_returnsList() {
        String empId = employeeUuid.toString();
        BenefitEnrollment enrollment = buildEnrollment(buildEmployee(), buildActivePlan(),
                EnrollmentStatusEnum.ACTIVE);
        when(benefitEnrollmentRepository.findByEmployeeId(empId)).thenReturn(List.of(enrollment));

        List<BenefitEnrollment> result = benefitService.getEmployeeBenefitEnrollments(empId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getEmployeeBenefitEnrollments_returnsEmpty_whenNone() {
        String empId = employeeUuid.toString();
        when(benefitEnrollmentRepository.findByEmployeeId(empId)).thenReturn(List.of());

        List<BenefitEnrollment> result = benefitService.getEmployeeBenefitEnrollments(empId);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getActiveEmployeeEnrollments
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getActiveEmployeeEnrollments_returnsList_ofActiveOnly() {
        String empId = employeeUuid.toString();
        BenefitEnrollment active = buildEnrollment(buildEmployee(), buildActivePlan(),
                EnrollmentStatusEnum.ACTIVE);
        when(benefitEnrollmentRepository.findByEmployeeIdAndStatus(empId, EnrollmentStatusEnum.ACTIVE))
                .thenReturn(List.of(active));

        List<BenefitEnrollment> result = benefitService.getActiveEmployeeEnrollments(empId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(EnrollmentStatusEnum.ACTIVE);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getEnrollmentById
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getEnrollmentById_returnsEnrollment_whenFound() {
        BenefitEnrollment enrollment = buildEnrollment(buildEmployee(), buildActivePlan(),
                EnrollmentStatusEnum.ACTIVE);
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        BenefitEnrollment result = benefitService.getEnrollmentById(enrollmentId);

        assertThat(result.getId()).isEqualTo(enrollmentId);
    }

    @Test
    void getEnrollmentById_throwsEntityNotFound_whenMissing() {
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benefitService.getEnrollmentById(enrollmentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(enrollmentId.toString());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // enrollEmployee
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void enrollEmployee_savesEnrollment_whenNotAlreadyEnrolled() {
        EmployeeEntity employee = buildEmployee();
        BenefitPlan plan = buildActivePlan();
        BenefitEnrollment enrollment = buildEnrollment(employee, plan, EnrollmentStatusEnum.PENDING);

        when(benefitEnrollmentRepository.findByEmployeeIdAndBenefitPlanId(
                employeeUuid.toString(), planId))
                .thenReturn(List.of());
        when(benefitEnrollmentRepository.save(enrollment)).thenReturn(enrollment);

        BenefitEnrollment result = benefitService.enrollEmployee(enrollment);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatusEnum.ACTIVE);
        assertThat(result.getEnrollmentDate()).isEqualTo(LocalDate.now());
        verify(benefitEnrollmentRepository).save(enrollment);
    }

    @Test
    void enrollEmployee_throwsIllegalState_whenAlreadyActivelyEnrolled() {
        EmployeeEntity employee = buildEmployee();
        BenefitPlan plan = buildActivePlan();
        BenefitEnrollment existing = buildEnrollment(employee, plan, EnrollmentStatusEnum.ACTIVE);
        BenefitEnrollment newEnrollment = buildEnrollment(employee, plan, EnrollmentStatusEnum.PENDING);

        when(benefitEnrollmentRepository.findByEmployeeIdAndBenefitPlanId(
                employeeUuid.toString(), planId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> benefitService.enrollEmployee(newEnrollment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already enrolled");
    }

    @Test
    void enrollEmployee_allowsReEnrollment_whenPreviousEnrollmentTerminated() {
        EmployeeEntity employee = buildEmployee();
        BenefitPlan plan = buildActivePlan();
        BenefitEnrollment terminated = buildEnrollment(employee, plan, EnrollmentStatusEnum.TERMINATED);
        BenefitEnrollment newEnrollment = buildEnrollment(employee, plan, EnrollmentStatusEnum.PENDING);

        when(benefitEnrollmentRepository.findByEmployeeIdAndBenefitPlanId(
                employeeUuid.toString(), planId))
                .thenReturn(List.of(terminated));
        when(benefitEnrollmentRepository.save(newEnrollment)).thenReturn(newEnrollment);

        BenefitEnrollment result = benefitService.enrollEmployee(newEnrollment);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatusEnum.ACTIVE);
    }

    @Test
    void enrollEmployee_throwsWhenEmployeeNull() {
        BenefitPlan plan = buildActivePlan();
        BenefitEnrollment enrollment = new BenefitEnrollment();
        enrollment.setEmployee(null);
        enrollment.setBenefitPlan(plan);

        // Service does not validate null employee — NPE thrown when getId() is called
        assertThatThrownBy(() -> benefitService.enrollEmployee(enrollment))
                .isInstanceOf(Exception.class);
    }

    @Test
    void enrollEmployee_throwsWhenPlanNull() {
        EmployeeEntity employee = buildEmployee();
        BenefitEnrollment enrollment = new BenefitEnrollment();
        enrollment.setEmployee(employee);
        enrollment.setBenefitPlan(null);

        // Service does not validate null plan — NPE thrown when getId() is called
        assertThatThrownBy(() -> benefitService.enrollEmployee(enrollment))
                .isInstanceOf(Exception.class);
    }

    @Test
    void enrollEmployee_throwsIllegalState_whenPlanInactive() {
        EmployeeEntity employee = buildEmployee();
        BenefitPlan inactivePlan = buildActivePlan();
        inactivePlan.setStatus(PlanStatusEnum.INACTIVE);

        BenefitEnrollment enrollment = buildEnrollment(employee, inactivePlan,
                EnrollmentStatusEnum.PENDING);

        assertThatThrownBy(() -> benefitService.enrollEmployee(enrollment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactive benefit plan");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // updateEnrollment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void updateEnrollment_updatesFieldsAndSaves() {
        EmployeeEntity employee = buildEmployee();
        BenefitPlan plan = buildActivePlan();
        BenefitEnrollment existing = buildEnrollment(employee, plan, EnrollmentStatusEnum.ACTIVE);
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(existing));
        when(benefitEnrollmentRepository.save(existing)).thenReturn(existing);

        BenefitEnrollment updated = new BenefitEnrollment();
        updated.setCoverageLevel(CoverageLevelEnum.EMPLOYEE_SPOUSE);
        updated.setEffectiveDate(LocalDate.of(2026, 6, 1));

        BenefitEnrollment result = benefitService.updateEnrollment(enrollmentId, updated);

        assertThat(result.getCoverageLevel()).isEqualTo(CoverageLevelEnum.EMPLOYEE_SPOUSE);
        assertThat(result.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        verify(benefitEnrollmentRepository).save(existing);
    }

    @Test
    void updateEnrollment_throwsEntityNotFound_whenMissing() {
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benefitService.updateEnrollment(enrollmentId, new BenefitEnrollment()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // terminateEnrollment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void terminateEnrollment_setsTerminatedStatus_andReason() {
        EmployeeEntity employee = buildEmployee();
        BenefitPlan plan = buildActivePlan();
        BenefitEnrollment enrollment = buildEnrollment(employee, plan, EnrollmentStatusEnum.ACTIVE);
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        LocalDate terminationDate = LocalDate.of(2026, 6, 30);
        benefitService.terminateEnrollment(enrollmentId, terminationDate, "Left company");

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatusEnum.TERMINATED);
        assertThat(enrollment.getTerminationDate()).isEqualTo(terminationDate);
        assertThat(enrollment.getTerminationReason()).isEqualTo("Left company");
        verify(benefitEnrollmentRepository).save(enrollment);
    }

    @Test
    void terminateEnrollment_throwsEntityNotFound_whenMissing() {
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benefitService.terminateEnrollment(enrollmentId, LocalDate.now(), "reason"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // cancelEnrollment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void cancelEnrollment_setsCancelledStatus_whenBeforeEffectiveDate() {
        EmployeeEntity employee = buildEmployee();
        BenefitPlan plan = buildActivePlan();
        BenefitEnrollment enrollment = buildEnrollment(employee, plan, EnrollmentStatusEnum.ACTIVE);
        enrollment.setEffectiveDate(LocalDate.now().plusDays(10)); // future effective date
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        benefitService.cancelEnrollment(enrollmentId);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatusEnum.CANCELLED);
        verify(benefitEnrollmentRepository).save(enrollment);
    }

    @Test
    void cancelEnrollment_throwsIllegalState_whenEffectiveDateIsPast() {
        EmployeeEntity employee = buildEmployee();
        BenefitPlan plan = buildActivePlan();
        BenefitEnrollment enrollment = buildEnrollment(employee, plan, EnrollmentStatusEnum.ACTIVE);
        enrollment.setEffectiveDate(LocalDate.now().minusDays(1)); // past effective date
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> benefitService.cancelEnrollment(enrollmentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel enrollment after effective date");
    }

    @Test
    void cancelEnrollment_throwsIllegalState_whenEffectiveDateIsToday() {
        EmployeeEntity employee = buildEmployee();
        BenefitPlan plan = buildActivePlan();
        BenefitEnrollment enrollment = buildEnrollment(employee, plan, EnrollmentStatusEnum.ACTIVE);
        enrollment.setEffectiveDate(LocalDate.now()); // today = not after today
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> benefitService.cancelEnrollment(enrollmentId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelEnrollment_throwsEntityNotFound_whenMissing() {
        when(benefitEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benefitService.cancelEnrollment(enrollmentId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // calculateTotalEmployeeDeductions
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void calculateTotalEmployeeDeductions_sumsCostsAcrossActiveEnrollments() {
        String empId = employeeUuid.toString();
        EmployeeEntity employee = buildEmployee();

        BenefitPlan plan1 = buildActivePlan(); // employeeCost = 200
        BenefitPlan plan2 = buildActivePlan();
        plan2.setId(UUID.randomUUID());
        plan2.setEmployeeCost(new BigDecimal("75.00"));

        BenefitEnrollment e1 = buildEnrollment(employee, plan1, EnrollmentStatusEnum.ACTIVE);
        BenefitEnrollment e2 = buildEnrollment(employee, plan2, EnrollmentStatusEnum.ACTIVE);
        e2.setId(UUID.randomUUID());

        when(benefitEnrollmentRepository.findByEmployeeIdAndStatus(empId, EnrollmentStatusEnum.ACTIVE))
                .thenReturn(List.of(e1, e2));

        BigDecimal result = benefitService.calculateTotalEmployeeDeductions(empId);

        assertThat(result).isEqualByComparingTo(new BigDecimal("275.00"));
    }

    @Test
    void calculateTotalEmployeeDeductions_returnsZero_whenNoActiveEnrollments() {
        String empId = employeeUuid.toString();
        when(benefitEnrollmentRepository.findByEmployeeIdAndStatus(empId, EnrollmentStatusEnum.ACTIVE))
                .thenReturn(List.of());

        BigDecimal result = benefitService.calculateTotalEmployeeDeductions(empId);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateTotalEmployeeDeductions_treatsNullCostAsZero() {
        String empId = employeeUuid.toString();
        EmployeeEntity employee = buildEmployee();

        BenefitPlan planNullCost = buildActivePlan();
        planNullCost.setEmployeeCost(null);

        BenefitEnrollment enrollment = buildEnrollment(employee, planNullCost, EnrollmentStatusEnum.ACTIVE);
        when(benefitEnrollmentRepository.findByEmployeeIdAndStatus(empId, EnrollmentStatusEnum.ACTIVE))
                .thenReturn(List.of(enrollment));

        BigDecimal result = benefitService.calculateTotalEmployeeDeductions(empId);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // calculateTotalEmployerContributions
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void calculateTotalEmployerContributions_sumsCostsAcrossActiveEnrollments() {
        String empId = employeeUuid.toString();
        EmployeeEntity employee = buildEmployee();

        BenefitPlan plan1 = buildActivePlan(); // employerCost = 400
        BenefitPlan plan2 = buildActivePlan();
        plan2.setId(UUID.randomUUID());
        plan2.setEmployerCost(new BigDecimal("300.00"));

        BenefitEnrollment e1 = buildEnrollment(employee, plan1, EnrollmentStatusEnum.ACTIVE);
        BenefitEnrollment e2 = buildEnrollment(employee, plan2, EnrollmentStatusEnum.ACTIVE);
        e2.setId(UUID.randomUUID());

        when(benefitEnrollmentRepository.findByEmployeeIdAndStatus(empId, EnrollmentStatusEnum.ACTIVE))
                .thenReturn(List.of(e1, e2));

        BigDecimal result = benefitService.calculateTotalEmployerContributions(empId);

        assertThat(result).isEqualByComparingTo(new BigDecimal("700.00"));
    }

    @Test
    void calculateTotalEmployerContributions_returnsZero_whenNoActiveEnrollments() {
        String empId = employeeUuid.toString();
        when(benefitEnrollmentRepository.findByEmployeeIdAndStatus(empId, EnrollmentStatusEnum.ACTIVE))
                .thenReturn(List.of());

        BigDecimal result = benefitService.calculateTotalEmployerContributions(empId);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateTotalEmployerContributions_treatsNullCostAsZero() {
        String empId = employeeUuid.toString();
        EmployeeEntity employee = buildEmployee();

        BenefitPlan planNullCost = buildActivePlan();
        planNullCost.setEmployerCost(null);

        BenefitEnrollment enrollment = buildEnrollment(employee, planNullCost, EnrollmentStatusEnum.ACTIVE);
        when(benefitEnrollmentRepository.findByEmployeeIdAndStatus(empId, EnrollmentStatusEnum.ACTIVE))
                .thenReturn(List.of(enrollment));

        BigDecimal result = benefitService.calculateTotalEmployerContributions(empId);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getEnrollmentsByPlan
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getEnrollmentsByPlan_returnsAllEnrollments_forPlan() {
        EmployeeEntity employee = buildEmployee();
        BenefitEnrollment enrollment = buildEnrollment(employee, buildActivePlan(),
                EnrollmentStatusEnum.ACTIVE);
        when(benefitEnrollmentRepository.findByBenefitPlanId(planId)).thenReturn(List.of(enrollment));

        List<BenefitEnrollment> result = benefitService.getEnrollmentsByPlan(planId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getEnrollmentsByPlan_returnsEmpty_whenNoEnrollments() {
        when(benefitEnrollmentRepository.findByBenefitPlanId(planId)).thenReturn(List.of());

        List<BenefitEnrollment> result = benefitService.getEnrollmentsByPlan(planId);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getActiveEnrollmentCountByPlan
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getActiveEnrollmentCountByPlan_returnsCount_fromRepository() {
        when(benefitEnrollmentRepository.countByBenefitPlanIdAndStatus(planId,
                EnrollmentStatusEnum.ACTIVE))
                .thenReturn(5L);

        long result = benefitService.getActiveEnrollmentCountByPlan(planId);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void getActiveEnrollmentCountByPlan_returnsZero_whenNoActive() {
        when(benefitEnrollmentRepository.countByBenefitPlanIdAndStatus(planId,
                EnrollmentStatusEnum.ACTIVE))
                .thenReturn(0L);

        long result = benefitService.getActiveEnrollmentCountByPlan(planId);

        assertThat(result).isZero();
    }
}
