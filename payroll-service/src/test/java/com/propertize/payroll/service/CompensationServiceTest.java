package com.propertize.payroll.service;

import com.propertize.payroll.dto.compensation.request.CompensationCreateRequest;
import com.propertize.payroll.dto.compensation.request.CompensationUpdateRequest;
import com.propertize.payroll.dto.compensation.response.CompensationHistoryResponse;
import com.propertize.payroll.dto.compensation.response.CompensationResponse;
import com.propertize.payroll.entity.CompensationEntity;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.enums.CompensationStatusEnum;
import com.propertize.payroll.enums.CompensationTypeEnum;
import com.propertize.payroll.enums.PayFrequencyEnum;
import com.propertize.payroll.exception.ResourceNotFoundException;
import com.propertize.payroll.exception.ValidationException;
import com.propertize.payroll.repository.CompensationRepository;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompensationServiceTest {

    @Mock
    CompensationRepository compensationRepository;
    @Mock
    EmployeeEntityRepository employeeRepository;

    @InjectMocks
    CompensationService compensationService;

    @Captor
    ArgumentCaptor<CompensationEntity> compensationCaptor;

    UUID employeeId;
    UUID compensationId;
    EmployeeEntity employee;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        compensationId = UUID.randomUUID();

        employee = new EmployeeEntity();
        employee.setId(employeeId);
        employee.setEmployeeNumber("EMP-001");
        employee.setFirstName("John");
        employee.setLastName("Doe");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private CompensationCreateRequest buildSalaryCreateRequest() {
        return CompensationCreateRequest.builder()
                .employeeId(employeeId)
                .compensationType(CompensationTypeEnum.BASE_SALARY)
                .payFrequency(PayFrequencyEnum.BI_WEEKLY)
                .annualSalary(new BigDecimal("75000.00"))
                .effectiveDate(LocalDate.now())
                .isCurrentCompensation(true)
                .changeReason("New hire")
                .notes("Starting salary")
                .build();
    }

    private CompensationCreateRequest buildHourlyCreateRequest() {
        return CompensationCreateRequest.builder()
                .employeeId(employeeId)
                .compensationType(CompensationTypeEnum.HOURLY_WAGE)
                .payFrequency(PayFrequencyEnum.WEEKLY)
                .hourlyRate(new BigDecimal("25.0000"))
                .standardHoursPerPeriod(new BigDecimal("40.00"))
                .effectiveDate(LocalDate.now())
                .isCurrentCompensation(false)
                .build();
    }

    private CompensationEntity buildCompensationEntity(BigDecimal annualSalary,
            BigDecimal hourlyRate,
            PayFrequencyEnum frequency) {
        CompensationEntity entity = CompensationEntity.builder()
                .employee(employee)
                .compensationType(CompensationTypeEnum.BASE_SALARY)
                .status(CompensationStatusEnum.ACTIVE)
                .payFrequency(frequency)
                .annualSalary(annualSalary)
                .hourlyRate(hourlyRate)
                .standardHoursPerPeriod(new BigDecimal("80.00"))
                .overtimeMultiplier(new BigDecimal("1.50"))
                .doubleTimeMultiplier(new BigDecimal("2.00"))
                .effectiveDate(LocalDate.now())
                .isCurrent(true)
                .changeReason("Initial")
                .notes("Test note")
                .build();
        entity.setId(compensationId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");
        return entity;
    }

    private CompensationEntity buildSalaryEntity() {
        return buildCompensationEntity(new BigDecimal("75000.00"), null, PayFrequencyEnum.BI_WEEKLY);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // createCompensation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class CreateCompensation {

        @Test
        void createCompensation_withSalary_createsAndReturnsResponse() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> {
                        CompensationEntity e = inv.getArgument(0);
                        e.setId(compensationId);
                        return e;
                    });

            CompensationResponse response = compensationService.createCompensation(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(compensationId);
            assertThat(response.getEmployeeId()).isEqualTo(employeeId);
            assertThat(response.getEmployeeFullName()).isEqualTo("John Doe");
            assertThat(response.getAnnualSalary()).isEqualByComparingTo("75000.00");
            assertThat(response.getCompensationType()).isEqualTo(CompensationTypeEnum.BASE_SALARY);
            assertThat(response.getStatus()).isEqualTo(CompensationStatusEnum.ACTIVE);

            verify(compensationRepository).save(compensationCaptor.capture());
            CompensationEntity saved = compensationCaptor.getValue();
            // BI_WEEKLY = 26 periods => 75000 / 26 = 2884.62
            assertThat(saved.getPayRatePerPeriod()).isEqualByComparingTo("2884.62");
        }

        @Test
        void createCompensation_withHourlyRate_calculatesPayRatePerPeriod() {
            CompensationCreateRequest request = buildHourlyCreateRequest();
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> {
                        CompensationEntity e = inv.getArgument(0);
                        e.setId(compensationId);
                        return e;
                    });

            CompensationResponse response = compensationService.createCompensation(request);

            assertThat(response).isNotNull();
            verify(compensationRepository).save(compensationCaptor.capture());
            CompensationEntity saved = compensationCaptor.getValue();
            // 25.00 * 40.00 = 1000.00
            assertThat(saved.getPayRatePerPeriod()).isEqualByComparingTo("1000.0000");
        }

        @Test
        void createCompensation_setsDefaultOvertimeMultiplier_whenNull() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setOvertimeMultiplier(null);
            request.setDoubleTimeMultiplier(null);

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            compensationService.createCompensation(request);

            verify(compensationRepository).save(compensationCaptor.capture());
            CompensationEntity saved = compensationCaptor.getValue();
            assertThat(saved.getOvertimeMultiplier()).isEqualByComparingTo("1.50");
            assertThat(saved.getDoubleTimeMultiplier()).isEqualByComparingTo("2.00");
        }

        @Test
        void createCompensation_usesCustomMultipliers_whenProvided() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setOvertimeMultiplier(new BigDecimal("2.00"));
            request.setDoubleTimeMultiplier(new BigDecimal("3.00"));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            compensationService.createCompensation(request);

            verify(compensationRepository).save(compensationCaptor.capture());
            CompensationEntity saved = compensationCaptor.getValue();
            assertThat(saved.getOvertimeMultiplier()).isEqualByComparingTo("2.00");
            assertThat(saved.getDoubleTimeMultiplier()).isEqualByComparingTo("3.00");
        }

        @Test
        void createCompensation_deactivatesPreviousCurrent_whenIsCurrentTrue() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setIsCurrentCompensation(true);

            CompensationEntity previousComp = buildSalaryEntity();
            previousComp.setIsCurrent(true);

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.of(previousComp));
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> {
                        CompensationEntity e = inv.getArgument(0);
                        if (e.getId() == null)
                            e.setId(UUID.randomUUID());
                        return e;
                    });

            compensationService.createCompensation(request);

            verify(compensationRepository).saveAll(anyList());
            assertThat(previousComp.getIsCurrent()).isFalse();
            assertThat(previousComp.getEndDate()).isEqualTo(LocalDate.now().minusDays(1));
        }

        @Test
        void createCompensation_doesNotDeactivatePrevious_whenIsCurrentFalse() {
            CompensationCreateRequest request = buildHourlyCreateRequest();
            request.setIsCurrentCompensation(false);

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> {
                        CompensationEntity e = inv.getArgument(0);
                        e.setId(compensationId);
                        return e;
                    });

            compensationService.createCompensation(request);

            verify(compensationRepository, never()).saveAll(anyList());
        }

        @Test
        void createCompensation_throwsResourceNotFound_whenEmployeeMissing() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> compensationService.createCompensation(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee not found");
        }

        @Test
        void createCompensation_throwsValidation_whenNeitherRateNorSalary() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setAnnualSalary(null);
            request.setHourlyRate(null);

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> compensationService.createCompensation(request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Either hourly rate or annual salary must be provided");
        }

        @Test
        void createCompensation_throwsValidation_whenBothRateAndSalaryProvided() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setAnnualSalary(new BigDecimal("75000.00"));
            request.setHourlyRate(new BigDecimal("25.00"));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> compensationService.createCompensation(request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Cannot specify both hourly rate and annual salary");
        }

        @Test
        void createCompensation_throwsValidation_whenEffectiveDateTooFarInPast() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setEffectiveDate(LocalDate.now().minusYears(2));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> compensationService.createCompensation(request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("more than 1 year in the past");
        }

        @Test
        void createCompensation_throwsValidation_whenEndDateBeforeEffective() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setEffectiveDate(LocalDate.now());
            request.setEndDate(LocalDate.now().minusDays(10));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> compensationService.createCompensation(request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("End date must be after effective date");
        }

        @Test
        void createCompensation_acceptsZeroHourlyRate_treatsAsNotProvided() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setAnnualSalary(new BigDecimal("60000.00"));
            request.setHourlyRate(BigDecimal.ZERO);

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> {
                        CompensationEntity e = inv.getArgument(0);
                        e.setId(compensationId);
                        return e;
                    });

            CompensationResponse response = compensationService.createCompensation(request);

            assertThat(response).isNotNull();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // updateCompensation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class UpdateCompensation {

        @Test
        void updateCompensation_updatesAllProvidedFields() {
            CompensationEntity existing = buildSalaryEntity();
            existing.setPayFrequency(PayFrequencyEnum.BI_WEEKLY);

            CompensationUpdateRequest request = CompensationUpdateRequest.builder()
                    .status(CompensationStatusEnum.INACTIVE)
                    .payFrequency(PayFrequencyEnum.MONTHLY)
                    .annualSalary(new BigDecimal("80000.00"))
                    .overtimeMultiplier(new BigDecimal("2.00"))
                    .doubleTimeMultiplier(new BigDecimal("3.00"))
                    .standardHoursPerPeriod(new BigDecimal("86.67"))
                    .endDate(LocalDate.now().plusMonths(6))
                    .changeReason("Annual raise")
                    .notes("Performance-based increase")
                    .build();

            when(compensationRepository.findById(compensationId)).thenReturn(Optional.of(existing));
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CompensationResponse response = compensationService.updateCompensation(compensationId, request);

            assertThat(response.getStatus()).isEqualTo(CompensationStatusEnum.INACTIVE);
            assertThat(response.getPayFrequency()).isEqualTo(PayFrequencyEnum.MONTHLY);
            assertThat(response.getAnnualSalary()).isEqualByComparingTo("80000.00");
            assertThat(response.getChangeReason()).isEqualTo("Annual raise");
            assertThat(response.getNotes()).isEqualTo("Performance-based increase");

            verify(compensationRepository).save(compensationCaptor.capture());
            CompensationEntity saved = compensationCaptor.getValue();
            // MONTHLY = 12 periods => 80000 / 12 = 6666.67
            assertThat(saved.getPayRatePerPeriod()).isEqualByComparingTo("6666.67");
        }

        @Test
        void updateCompensation_leavesFieldsUnchanged_whenNull() {
            CompensationEntity existing = buildSalaryEntity();
            existing.setChangeReason("Original reason");
            existing.setNotes("Original notes");

            CompensationUpdateRequest request = CompensationUpdateRequest.builder().build();

            when(compensationRepository.findById(compensationId)).thenReturn(Optional.of(existing));
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CompensationResponse response = compensationService.updateCompensation(compensationId, request);

            assertThat(response.getChangeReason()).isEqualTo("Original reason");
            assertThat(response.getNotes()).isEqualTo("Original notes");
            assertThat(response.getAnnualSalary()).isEqualByComparingTo("75000.00");
        }

        @Test
        void updateCompensation_recalculatesPayRatePerPeriod_onHourlyRateChange() {
            CompensationEntity existing = buildCompensationEntity(null, new BigDecimal("30.0000"),
                    PayFrequencyEnum.WEEKLY);
            existing.setCompensationType(CompensationTypeEnum.HOURLY_WAGE);
            existing.setStandardHoursPerPeriod(new BigDecimal("40.00"));

            CompensationUpdateRequest request = CompensationUpdateRequest.builder()
                    .hourlyRate(new BigDecimal("35.0000"))
                    .build();

            when(compensationRepository.findById(compensationId)).thenReturn(Optional.of(existing));
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            compensationService.updateCompensation(compensationId, request);

            verify(compensationRepository).save(compensationCaptor.capture());
            CompensationEntity saved = compensationCaptor.getValue();
            // 35.00 * 40.00 = 1400.00
            assertThat(saved.getPayRatePerPeriod()).isEqualByComparingTo("1400.0000");
        }

        @Test
        void updateCompensation_throwsResourceNotFound_whenMissing() {
            when(compensationRepository.findById(compensationId)).thenReturn(Optional.empty());

            CompensationUpdateRequest request = CompensationUpdateRequest.builder().build();

            assertThatThrownBy(() -> compensationService.updateCompensation(compensationId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Compensation not found");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getCompensationById
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class GetCompensationById {

        @Test
        void getCompensationById_returnsResponse_whenFound() {
            CompensationEntity entity = buildSalaryEntity();
            when(compensationRepository.findById(compensationId)).thenReturn(Optional.of(entity));

            CompensationResponse response = compensationService.getCompensationById(compensationId);

            assertThat(response.getId()).isEqualTo(compensationId);
            assertThat(response.getEmployeeId()).isEqualTo(employeeId);
            assertThat(response.getEmployeeNumber()).isEqualTo("EMP-001");
            assertThat(response.getEmployeeFullName()).isEqualTo("John Doe");
            assertThat(response.getCompensationType()).isEqualTo(CompensationTypeEnum.BASE_SALARY);
            assertThat(response.getIsCurrentCompensation()).isTrue();
        }

        @Test
        void getCompensationById_throwsResourceNotFound_whenMissing() {
            when(compensationRepository.findById(compensationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> compensationService.getCompensationById(compensationId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Compensation not found");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getCurrentCompensation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class GetCurrentCompensation {

        @Test
        void getCurrentCompensation_returnsCurrentRecord() {
            CompensationEntity entity = buildSalaryEntity();
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.of(entity));

            CompensationResponse response = compensationService.getCurrentCompensation(employeeId);

            assertThat(response.getId()).isEqualTo(compensationId);
            assertThat(response.getIsCurrentCompensation()).isTrue();
        }

        @Test
        void getCurrentCompensation_throwsResourceNotFound_whenEmployeeMissing() {
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> compensationService.getCurrentCompensation(employeeId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee not found");
        }

        @Test
        void getCurrentCompensation_throwsResourceNotFound_whenNoCurrentRecord() {
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> compensationService.getCurrentCompensation(employeeId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No current compensation found");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getCompensationHistory
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class GetCompensationHistory {

        @Test
        void getCompensationHistory_returnsHistoryList() {
            CompensationEntity current = buildSalaryEntity();
            CompensationEntity previous = buildCompensationEntity(
                    new BigDecimal("65000.00"), null, PayFrequencyEnum.BI_WEEKLY);
            previous.setId(UUID.randomUUID());
            previous.setIsCurrent(false);
            previous.setStatus(CompensationStatusEnum.INACTIVE);
            previous.setEffectiveDate(LocalDate.now().minusYears(1));
            previous.setEndDate(LocalDate.now().minusDays(1));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId))
                    .thenReturn(List.of(current, previous));

            List<CompensationHistoryResponse> history = compensationService.getCompensationHistory(employeeId);

            assertThat(history).hasSize(2);
            assertThat(history.get(0).getId()).isEqualTo(compensationId);
            assertThat(history.get(0).getIsCurrent()).isTrue();
            assertThat(history.get(0).getCompensationType()).isEqualTo(CompensationTypeEnum.BASE_SALARY);
            assertThat(history.get(0).getStatus()).isEqualTo(CompensationStatusEnum.ACTIVE);
            assertThat(history.get(0).getPayFrequency()).isEqualTo(PayFrequencyEnum.BI_WEEKLY);
            assertThat(history.get(1).getIsCurrent()).isFalse();
        }

        @Test
        void getCompensationHistory_returnsEmptyList_whenNoHistory() {
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId))
                    .thenReturn(List.of());

            List<CompensationHistoryResponse> history = compensationService.getCompensationHistory(employeeId);

            assertThat(history).isEmpty();
        }

        @Test
        void getCompensationHistory_throwsResourceNotFound_whenEmployeeMissing() {
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> compensationService.getCompensationHistory(employeeId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee not found");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // deactivateCompensation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class DeactivateCompensation {

        @Test
        void deactivateCompensation_setsInactiveAndEndDate() {
            CompensationEntity entity = buildSalaryEntity();
            LocalDate endDate = LocalDate.now();
            String reason = "Employee terminated";

            when(compensationRepository.findById(compensationId)).thenReturn(Optional.of(entity));

            compensationService.deactivateCompensation(compensationId, endDate, reason);

            verify(compensationRepository).save(compensationCaptor.capture());
            CompensationEntity saved = compensationCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(CompensationStatusEnum.INACTIVE);
            assertThat(saved.getEndDate()).isEqualTo(endDate);
            assertThat(saved.getChangeReason()).isEqualTo(reason);
            assertThat(saved.getIsCurrent()).isFalse();
        }

        @Test
        void deactivateCompensation_throwsResourceNotFound_whenMissing() {
            when(compensationRepository.findById(compensationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> compensationService.deactivateCompensation(
                    compensationId, LocalDate.now(), "reason"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Compensation not found");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // deleteCompensation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class DeleteCompensation {

        @Test
        void deleteCompensation_softDeletesSetsInactive() {
            CompensationEntity entity = buildSalaryEntity();
            when(compensationRepository.findById(compensationId)).thenReturn(Optional.of(entity));

            compensationService.deleteCompensation(compensationId);

            verify(compensationRepository).save(compensationCaptor.capture());
            CompensationEntity saved = compensationCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(CompensationStatusEnum.INACTIVE);
            assertThat(saved.getIsCurrent()).isFalse();
        }

        @Test
        void deleteCompensation_doesNotHardDelete() {
            CompensationEntity entity = buildSalaryEntity();
            when(compensationRepository.findById(compensationId)).thenReturn(Optional.of(entity));

            compensationService.deleteCompensation(compensationId);

            verify(compensationRepository, never()).delete(any());
            verify(compensationRepository, never()).deleteById(any());
        }

        @Test
        void deleteCompensation_throwsResourceNotFound_whenMissing() {
            when(compensationRepository.findById(compensationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> compensationService.deleteCompensation(compensationId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Compensation not found");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Pay rate calculation edge cases
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class PayRateCalculation {

        @Test
        void createCompensation_calculatesWeeklyPayRate_fromSalary() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setPayFrequency(PayFrequencyEnum.WEEKLY);
            request.setAnnualSalary(new BigDecimal("52000.00"));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            compensationService.createCompensation(request);

            verify(compensationRepository).save(compensationCaptor.capture());
            // 52000 / 52 = 1000.00
            assertThat(compensationCaptor.getValue().getPayRatePerPeriod())
                    .isEqualByComparingTo("1000.00");
        }

        @Test
        void createCompensation_calculatesMonthlyPayRate_fromSalary() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setPayFrequency(PayFrequencyEnum.MONTHLY);
            request.setAnnualSalary(new BigDecimal("120000.00"));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            compensationService.createCompensation(request);

            verify(compensationRepository).save(compensationCaptor.capture());
            // 120000 / 12 = 10000.00
            assertThat(compensationCaptor.getValue().getPayRatePerPeriod())
                    .isEqualByComparingTo("10000.00");
        }

        @Test
        void createCompensation_calculatesSemiMonthlyPayRate_fromSalary() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setPayFrequency(PayFrequencyEnum.SEMI_MONTHLY);
            request.setAnnualSalary(new BigDecimal("72000.00"));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            compensationService.createCompensation(request);

            verify(compensationRepository).save(compensationCaptor.capture());
            // 72000 / 24 = 3000.00
            assertThat(compensationCaptor.getValue().getPayRatePerPeriod())
                    .isEqualByComparingTo("3000.00");
        }

        @Test
        void createCompensation_calculatesQuarterlyPayRate_fromSalary() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setPayFrequency(PayFrequencyEnum.QUARTERLY);
            request.setAnnualSalary(new BigDecimal("100000.00"));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            compensationService.createCompensation(request);

            verify(compensationRepository).save(compensationCaptor.capture());
            // 100000 / 4 = 25000.00
            assertThat(compensationCaptor.getValue().getPayRatePerPeriod())
                    .isEqualByComparingTo("25000.00");
        }

        @Test
        void createCompensation_calculatesAnnualPayRate_fromSalary() {
            CompensationCreateRequest request = buildSalaryCreateRequest();
            request.setPayFrequency(PayFrequencyEnum.ANNUALLY);
            request.setAnnualSalary(new BigDecimal("90000.00"));

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdAndIsCurrent(employeeId, true))
                    .thenReturn(Optional.empty());
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            compensationService.createCompensation(request);

            verify(compensationRepository).save(compensationCaptor.capture());
            // 90000 / 1 = 90000.00
            assertThat(compensationCaptor.getValue().getPayRatePerPeriod())
                    .isEqualByComparingTo("90000.00");
        }

        @Test
        void createCompensation_returnsZeroPayRate_whenNoRateOrSalary() {
            // Edge case: hourly rate with no standard hours
            CompensationCreateRequest request = buildHourlyCreateRequest();
            request.setStandardHoursPerPeriod(null);
            // hourlyRate is set but standardHoursPerPeriod is null
            // calculatePayRatePerPeriod should return ZERO since hourly needs hours

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.save(any(CompensationEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            compensationService.createCompensation(request);

            verify(compensationRepository).save(compensationCaptor.capture());
            // hourlyRate > 0 but standardHoursPerPeriod is null => the check fails =>
            // returns ZERO
            assertThat(compensationCaptor.getValue().getPayRatePerPeriod())
                    .isEqualByComparingTo("0");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Response mapping verification
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class ResponseMapping {

        @Test
        void mapToResponse_includesAllAuditFields() {
            CompensationEntity entity = buildSalaryEntity();
            entity.setCreatedBy("admin");
            entity.setUpdatedBy("admin");
            LocalDateTime now = LocalDateTime.now();
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);

            when(compensationRepository.findById(compensationId)).thenReturn(Optional.of(entity));

            CompensationResponse response = compensationService.getCompensationById(compensationId);

            assertThat(response.getCreatedBy()).isEqualTo("admin");
            assertThat(response.getUpdatedBy()).isEqualTo("admin");
            assertThat(response.getCreatedAt()).isEqualTo(now);
            assertThat(response.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        void mapToHistoryResponse_mapsEnumNamesToStrings() {
            CompensationEntity entity = buildSalaryEntity();
            entity.setCompensationType(CompensationTypeEnum.COMMISSION);
            entity.setStatus(CompensationStatusEnum.PENDING);
            entity.setPayFrequency(PayFrequencyEnum.MONTHLY);

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(compensationRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId))
                    .thenReturn(List.of(entity));

            List<CompensationHistoryResponse> history = compensationService.getCompensationHistory(employeeId);

            assertThat(history).hasSize(1);
            assertThat(history.get(0).getCompensationType()).isEqualTo(CompensationTypeEnum.COMMISSION);
            assertThat(history.get(0).getStatus()).isEqualTo(CompensationStatusEnum.PENDING);
            assertThat(history.get(0).getPayFrequency()).isEqualTo(PayFrequencyEnum.MONTHLY);
        }
    }
}
