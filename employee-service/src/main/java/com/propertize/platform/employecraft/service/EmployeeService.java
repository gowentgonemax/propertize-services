package com.propertize.platform.employecraft.service;

import com.propertize.platform.employecraft.context.OrganizationContext;
import com.propertize.platform.employecraft.client.PropertizeFeignClient;
import com.propertize.platform.employecraft.dto.*;
import com.propertize.platform.employecraft.dto.employee.request.EmployeeCreateRequest;
import com.propertize.platform.employecraft.dto.employee.response.EmployeePayrollSummary;
import com.propertize.platform.employecraft.dto.employee.response.EmployeeResponse;
import com.propertize.platform.employecraft.dto.propertize.UserCreateRequest;
import com.propertize.platform.employecraft.dto.propertize.UserDto;
import com.propertize.platform.employecraft.entity.Department;
import com.propertize.platform.employecraft.entity.Employee;
import com.propertize.platform.employecraft.entity.Position;
import com.propertize.platform.employecraft.entity.embedded.Address;
import com.propertize.platform.employecraft.entity.embedded.Compensation;
import com.propertize.platform.employecraft.entity.embedded.EmergencyContact;
import com.propertize.platform.employecraft.enums.EmployeeStatusEnum;
import com.propertize.platform.employecraft.enums.PayFrequencyEnum;
import com.propertize.platform.employecraft.enums.PayTypeEnum;
import com.propertize.platform.employecraft.event.EmployeeEvent;
import com.propertize.platform.employecraft.event.EmployeeEventPublisher;
import com.propertize.platform.employecraft.exception.EmployeeNotFoundException;
import com.propertize.platform.employecraft.exception.PropertizeIntegrationException;
import com.propertize.platform.employecraft.repository.DepartmentRepository;
import com.propertize.platform.employecraft.repository.EmployeeRepository;
import com.propertize.platform.employecraft.repository.PositionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Employee Service - Handles employee management
 * Integrates with Propertize for user provisioning
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final PropertizeFeignClient propertizeFeignClient;
    private final EmployeeNumberGenerator employeeNumberGenerator;
    private final EmployeeEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        UUID organizationId = OrganizationContext.requireOrganizationId();
        Page<Employee> employees = employeeRepository.findByOrganizationId(organizationId, pageable);
        return employees.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(UUID employeeId) {
        UUID organizationId = OrganizationContext.requireOrganizationId();
        Employee employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByUserId(Long userId) {
        UUID organizationId = OrganizationContext.requireOrganizationId();
        Employee employee = employeeRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new EmployeeNotFoundException("No employee found for user: " + userId));
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        UUID organizationId = OrganizationContext.requireOrganizationId();

        // Validate email uniqueness
        if (employeeRepository.existsByEmailAndOrganizationId(request.getEmail(), organizationId)) {
            throw new IllegalArgumentException("Employee with email already exists: " + request.getEmail());
        }

        // Build employee entity
        Employee employee = Employee.builder()
                .organizationId(organizationId)
                .employeeNumber(employeeNumberGenerator.generate(organizationId))
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .workEmail(request.getWorkEmail())
                .phoneNumber(request.getPhoneNumber())
                .workPhone(request.getWorkPhone())
                .dateOfBirth(request.getDateOfBirth())
                .employmentType(request.getEmploymentType())
                .status(EmployeeStatusEnum.PENDING)
                .hireDate(request.getHireDate())
                .homeAddress(buildAddress(request))
                .compensation(buildCompensation(request))
                .emergencyContact(buildEmergencyContact(request))
                .build();

        // Set relationships
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findByIdAndOrganizationId(request.getDepartmentId(), organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
            employee.setDepartment(dept);
        }

        if (request.getPositionId() != null) {
            Position pos = positionRepository.findByIdAndOrganizationId(request.getPositionId(), organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Position not found"));
            employee.setPosition(pos);
        }

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findByIdAndOrganizationId(request.getManagerId(), organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Manager not found"));
            employee.setManager(manager);
        }

        // Create user in Propertize if system access needed
        if (request.isCreateSystemAccess()) {
            Long userId = createUserInPropertize(request, organizationId);
            employee.setUserId(userId);
        }

        Employee saved = employeeRepository.save(employee);
        logger.info("Created employee: {} for organization: {}", saved.getEmployeeNumber(), organizationId);
        eventPublisher.publish(saved, EmployeeEvent.EventType.CREATED);

        return toResponse(saved);
    }

    @Transactional
    public EmployeeResponse activateEmployee(UUID employeeId) {
        UUID organizationId = OrganizationContext.requireOrganizationId();
        Employee employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        employee.setStatus(EmployeeStatusEnum.ACTIVE);
        Employee saved = employeeRepository.save(employee);

        logger.info("Activated employee: {}", saved.getEmployeeNumber());
        eventPublisher.publish(saved, EmployeeEvent.EventType.ACTIVATED);
        return toResponse(saved);
    }

    /**
     * Return employees modified after a given timestamp (incremental sync for
     * payroll-service).
     */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getChangedSince(LocalDateTime since, Pageable pageable) {
        UUID organizationId = OrganizationContext.requireOrganizationId();
        return employeeRepository
                .findByOrganizationIdAndUpdatedAtAfter(organizationId, since, pageable)
                .map(this::toResponse);
    }

    /**
     * Return a minimal payroll-ready summary for all active/onleave employees.
     */
    @Transactional(readOnly = true)
    public List<EmployeePayrollSummary> getPayrollSummaries() {
        UUID organizationId = OrganizationContext.requireOrganizationId();
        List<EmployeeStatusEnum> payrollStatuses = List.of(
                EmployeeStatusEnum.ACTIVE,
                EmployeeStatusEnum.ON_LEAVE);
        return employeeRepository
                .findByOrganizationIdAndStatusIn(organizationId, payrollStatuses)
                .stream()
                .map(this::toPayrollSummary)
                .toList();
    }

    @Transactional
    public EmployeeResponse terminateEmployee(UUID employeeId, String reason) {
        UUID organizationId = OrganizationContext.requireOrganizationId();
        Employee employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        employee.setStatus(EmployeeStatusEnum.TERMINATED);
        employee.setTerminationDate(java.time.LocalDate.now());
        employee.setTerminationReason(reason);

        // Deactivate user in Propertize if linked
        if (employee.getUserId() != null) {
            try {
                propertizeFeignClient.deactivateUser(employee.getUserId(), getAuthToken());
                logger.info("Deactivated Propertize user: {} for terminated employee", employee.getUserId());
            } catch (Exception e) {
                logger.error("Failed to deactivate Propertize user: {}", e.getMessage());
                // Don't fail the termination, just log
            }
        }

        Employee saved = employeeRepository.save(employee);
        logger.info("Terminated employee: {}", saved.getEmployeeNumber());
        eventPublisher.publish(saved, EmployeeEvent.EventType.TERMINATED);

        return toResponse(saved);
    }

    private Long createUserInPropertize(EmployeeCreateRequest request, UUID organizationId) {
        try {
            UserCreateRequest userRequest = UserCreateRequest.builder()
                    .username(request.getWorkEmail() != null ? request.getWorkEmail() : request.getEmail())
                    .email(request.getEmail())
                    .password(request.getTempPassword() != null ? request.getTempPassword() : generateTempPassword())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .organizationId(organizationId)
                    .roles(request.getSystemRoles() != null ? request.getSystemRoles() : new HashSet<>())
                    .build();

            ResponseEntity<UserDto> response = propertizeFeignClient.createUser(userRequest, getAuthToken());

            if (response.getBody() != null) {
                logger.info("Created Propertize user: {} for employee", response.getBody().getId());
                return response.getBody().getId();
            }
            throw new PropertizeIntegrationException("Failed to create user - no response body", 500);
        } catch (PropertizeIntegrationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create user in Propertize: {}", e.getMessage());
            throw new PropertizeIntegrationException("Failed to create user: " + e.getMessage(), 500, e);
        }
    }

    private String getAuthToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isBlank()) {
                return authHeader;
            }
        }
        throw new IllegalStateException("No authentication token available in current request");
    }

    private String generateTempPassword() {
        return "Temp" + UUID.randomUUID().toString().substring(0, 8) + "!";
    }

    private Address buildAddress(EmployeeCreateRequest request) {
        if (request.getStreetAddress() == null && request.getCity() == null) {
            return null;
        }
        return Address.builder()
                .streetAddress(request.getStreetAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .build();
    }

    private Compensation buildCompensation(EmployeeCreateRequest request) {
        if (request.getPayType() == null && request.getPayRate() == null) {
            return null;
        }
        return Compensation.builder()
                .payType(request.getPayType() != null ? PayTypeEnum.valueOf(request.getPayType()) : null)
                .payRate(request.getPayRate())
                .payFrequency(
                        request.getPayFrequency() != null ? PayFrequencyEnum.valueOf(request.getPayFrequency()) : null)
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankRoutingNumber(request.getBankRoutingNumber())
                .build();
    }

    private EmergencyContact buildEmergencyContact(EmployeeCreateRequest request) {
        if (request.getEmergencyContactName() == null) {
            return null;
        }
        return EmergencyContact.builder()
                .name(request.getEmergencyContactName())
                .relationship(request.getEmergencyContactRelationship())
                .phone(request.getEmergencyContactPhone())
                .email(request.getEmergencyContactEmail())
                .build();
    }

    private EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse.EmployeeResponseBuilder builder = EmployeeResponse.builder()
                .id(employee.getId())
                .organizationId(employee.getOrganizationId())
                .userId(employee.getUserId())
                .employeeNumber(employee.getEmployeeNumber())
                .firstName(employee.getFirstName())
                .middleName(employee.getMiddleName())
                .lastName(employee.getLastName())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .workEmail(employee.getWorkEmail())
                .phoneNumber(employee.getPhoneNumber())
                .workPhone(employee.getWorkPhone())
                .dateOfBirth(employee.getDateOfBirth())
                .profilePhotoUrl(employee.getProfilePhotoUrl())
                .employmentType(employee.getEmploymentType())
                .status(employee.getStatus())
                .hireDate(employee.getHireDate())
                .terminationDate(employee.getTerminationDate())
                .hasSystemAccess(employee.hasSystemAccess())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt());

        if (employee.getDepartment() != null) {
            builder.department(DepartmentSummary.builder()
                    .id(employee.getDepartment().getId())
                    .name(employee.getDepartment().getName())
                    .code(employee.getDepartment().getCode())
                    .build());
        }

        if (employee.getPosition() != null) {
            builder.position(PositionSummary.builder()
                    .id(employee.getPosition().getId())
                    .title(employee.getPosition().getTitle())
                    .code(employee.getPosition().getCode())
                    .build());
        }

        if (employee.getManager() != null) {
            builder.manager(ManagerSummary.builder()
                    .id(employee.getManager().getId())
                    .fullName(employee.getManager().getFullName())
                    .email(employee.getManager().getEmail())
                    .build());
        }

        if (employee.getHomeAddress() != null) {
            Address addr = employee.getHomeAddress();
            builder.homeAddress(AddressSummary.builder()
                    .streetAddress(addr.getStreetAddress())
                    .city(addr.getCity())
                    .state(addr.getState())
                    .zipCode(addr.getZipCode())
                    .country(addr.getCountry())
                    .build());
        }

        if (employee.getCompensation() != null) {
            Compensation comp = employee.getCompensation();
            builder.compensation(CompensationSummary.builder()
                    .payType(comp.getPayType() != null ? comp.getPayType().name() : null)
                    .payRate(comp.getPayRate())
                    .payFrequency(comp.getPayFrequency() != null ? comp.getPayFrequency().name() : null)
                    .annualSalary(comp.getAnnualSalary())
                    .build());
        }

        return builder.build();
    }

    private EmployeePayrollSummary toPayrollSummary(Employee employee) {
        EmployeePayrollSummary.EmployeePayrollSummaryBuilder b = EmployeePayrollSummary.builder()
                .id(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .status(employee.getStatus().name())
                .employmentType(employee.getEmploymentType().name())
                .hireDate(employee.getHireDate())
                .terminationDate(employee.getTerminationDate())
                .updatedAt(employee.getUpdatedAt());

        if (employee.getCompensation() != null) {
            Compensation comp = employee.getCompensation();
            b.payType(comp.getPayType() != null ? comp.getPayType().name() : null)
                    .payRate(comp.getPayRate())
                    .payFrequency(comp.getPayFrequency() != null ? comp.getPayFrequency().name() : null);
        }

        return b.build();
    }
}
