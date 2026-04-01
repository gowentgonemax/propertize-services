package com.propertize.platform.employecraft.service;

import com.propertize.platform.employecraft.context.OrganizationContext;
import com.propertize.platform.employecraft.dto.employee.request.EmployeeCreateRequest;
import com.propertize.platform.employecraft.dto.employee.response.EmployeePayrollSummary;
import com.propertize.platform.employecraft.dto.employee.response.EmployeeResponse;
import com.propertize.platform.employecraft.entity.Employee;
import com.propertize.platform.employecraft.enums.EmployeeStatusEnum;
import com.propertize.platform.employecraft.enums.EmploymentTypeEnum;
import com.propertize.platform.employecraft.event.EmployeeEvent;
import com.propertize.platform.employecraft.event.EmployeeEventPublisher;
import com.propertize.platform.employecraft.exception.EmployeeNotFoundException;
import com.propertize.platform.employecraft.client.PropertizeFeignClient;
import com.propertize.platform.employecraft.repository.DepartmentRepository;
import com.propertize.platform.employecraft.repository.EmployeeRepository;
import com.propertize.platform.employecraft.repository.PositionRepository;
import org.junit.jupiter.api.AfterEach;
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
class EmployeeServiceTest {

    @Mock
    EmployeeRepository employeeRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    PositionRepository positionRepository;
    @Mock
    PropertizeFeignClient propertizeFeignClient;
    @Mock
    EmployeeNumberGenerator employeeNumberGenerator;
    @Mock
    EmployeeEventPublisher eventPublisher;

    @InjectMocks
    EmployeeService employeeService;

    UUID orgId;
    UUID empId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        empId = UUID.randomUUID();
        OrganizationContext.setOrganizationId(orgId);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Employee buildEmployee(UUID id, EmployeeStatusEnum status) {
        return Employee.builder()
                .id(id)
                .organizationId(orgId)
                .employeeNumber("EMP-001")
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .employmentType(EmploymentTypeEnum.FULL_TIME)
                .status(status)
                .hireDate(LocalDate.of(2023, 1, 15))
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getAllEmployees
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getAllEmployees_returnsMappedPage() {
        Employee emp = buildEmployee(empId, EmployeeStatusEnum.ACTIVE);
        Page<Employee> page = new PageImpl<>(List.of(emp));
        when(employeeRepository.findByOrganizationId(eq(orgId), any(Pageable.class))).thenReturn(page);

        Page<EmployeeResponse> result = employeeService.getAllEmployees(Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void getAllEmployees_returnsEmptyPage_whenNoEmployees() {
        when(employeeRepository.findByOrganizationId(eq(orgId), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<EmployeeResponse> result = employeeService.getAllEmployees(Pageable.unpaged());

        assertThat(result.getTotalElements()).isZero();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getEmployee
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getEmployee_returnsEmployee_whenFound() {
        Employee emp = buildEmployee(empId, EmployeeStatusEnum.ACTIVE);
        when(employeeRepository.findByIdAndOrganizationId(empId, orgId)).thenReturn(Optional.of(emp));

        EmployeeResponse result = employeeService.getEmployee(empId);

        assertThat(result.getId()).isEqualTo(empId);
        assertThat(result.getFirstName()).isEqualTo("Jane");
    }

    @Test
    void getEmployee_throwsEmployeeNotFoundException_whenNotFound() {
        when(employeeRepository.findByIdAndOrganizationId(empId, orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployee(empId))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getEmployeeByUserId
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getEmployeeByUserId_returnsEmployee_whenFound() {
        Employee emp = buildEmployee(empId, EmployeeStatusEnum.ACTIVE);
        emp.setUserId(42L);
        when(employeeRepository.findByUserIdAndOrganizationId(42L, orgId)).thenReturn(Optional.of(emp));

        EmployeeResponse result = employeeService.getEmployeeByUserId(42L);

        assertThat(result.getUserId()).isEqualTo(42L);
    }

    @Test
    void getEmployeeByUserId_throwsEmployeeNotFoundException_whenNotFound() {
        when(employeeRepository.findByUserIdAndOrganizationId(anyLong(), eq(orgId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeByUserId(99L))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // createEmployee
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void createEmployee_savesAndReturnsEmployee() {
        EmployeeCreateRequest req = new EmployeeCreateRequest();
        req.setFirstName("Alice");
        req.setLastName("Smith");
        req.setEmail("alice@example.com");
        req.setEmploymentType(EmploymentTypeEnum.FULL_TIME);
        req.setHireDate(LocalDate.now());

        when(employeeRepository.existsByEmailAndOrganizationId("alice@example.com", orgId)).thenReturn(false);
        when(employeeNumberGenerator.generate(orgId)).thenReturn("EMP-002");

        Employee saved = Employee.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .employeeNumber("EMP-002")
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .employmentType(EmploymentTypeEnum.FULL_TIME)
                .status(EmployeeStatusEnum.PENDING)
                .hireDate(LocalDate.now())
                .build();
        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        EmployeeResponse result = employeeService.createEmployee(req);

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getEmployeeNumber()).isEqualTo("EMP-002");
        verify(eventPublisher).publish(any(Employee.class), eq(EmployeeEvent.EventType.CREATED));
    }

    @Test
    void createEmployee_throwsIllegalArgument_whenEmailAlreadyExists() {
        EmployeeCreateRequest req = new EmployeeCreateRequest();
        req.setFirstName("Alice");
        req.setLastName("Smith");
        req.setEmail("duplicate@example.com");
        req.setEmploymentType(EmploymentTypeEnum.FULL_TIME);
        req.setHireDate(LocalDate.now());

        when(employeeRepository.existsByEmailAndOrganizationId("duplicate@example.com", orgId)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // activateEmployee
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void activateEmployee_setsStatusActive() {
        Employee emp = buildEmployee(empId, EmployeeStatusEnum.PENDING);
        when(employeeRepository.findByIdAndOrganizationId(empId, orgId)).thenReturn(Optional.of(emp));
        when(employeeRepository.save(emp)).thenReturn(emp);

        EmployeeResponse result = employeeService.activateEmployee(empId);

        assertThat(result.getStatus()).isEqualTo(EmployeeStatusEnum.ACTIVE);
        verify(eventPublisher).publish(emp, EmployeeEvent.EventType.ACTIVATED);
    }

    @Test
    void activateEmployee_throwsNotFound_whenMissing() {
        when(employeeRepository.findByIdAndOrganizationId(empId, orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.activateEmployee(empId))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // terminateEmployee
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void terminateEmployee_setsStatusTerminated() {
        Employee emp = buildEmployee(empId, EmployeeStatusEnum.ACTIVE);
        when(employeeRepository.findByIdAndOrganizationId(empId, orgId)).thenReturn(Optional.of(emp));
        when(employeeRepository.save(emp)).thenReturn(emp);

        EmployeeResponse result = employeeService.terminateEmployee(empId, "Resignation");

        assertThat(result.getStatus()).isEqualTo(EmployeeStatusEnum.TERMINATED);
        verify(eventPublisher).publish(emp, EmployeeEvent.EventType.TERMINATED);
    }

    @Test
    void terminateEmployee_throwsNotFound_whenMissing() {
        when(employeeRepository.findByIdAndOrganizationId(empId, orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.terminateEmployee(empId, "Resignation"))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getPayrollSummaries
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getPayrollSummaries_returnsActiveAndOnLeaveEmployees() {
        Employee active = buildEmployee(empId, EmployeeStatusEnum.ACTIVE);
        Employee onLeave = buildEmployee(UUID.randomUUID(), EmployeeStatusEnum.ON_LEAVE);
        when(employeeRepository.findByOrganizationIdAndStatusIn(eq(orgId), anyList()))
                .thenReturn(List.of(active, onLeave));

        List<EmployeePayrollSummary> result = employeeService.getPayrollSummaries();

        assertThat(result).hasSize(2);
    }

    @Test
    void getPayrollSummaries_returnsEmptyList_whenNoEligibleEmployees() {
        when(employeeRepository.findByOrganizationIdAndStatusIn(eq(orgId), anyList()))
                .thenReturn(List.of());

        List<EmployeePayrollSummary> result = employeeService.getPayrollSummaries();

        assertThat(result).isEmpty();
    }
}
