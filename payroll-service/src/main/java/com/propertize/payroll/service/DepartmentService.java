package com.propertize.payroll.service;

import com.propertize.payroll.dto.department.request.DepartmentCreateRequest;
import com.propertize.payroll.dto.department.response.DepartmentResponse;
import com.propertize.payroll.entity.Client;
import com.propertize.payroll.entity.DepartmentEntity;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.repository.ClientRepository;
import com.propertize.payroll.repository.DepartmentRepository;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ClientRepository clientRepository;
    private final EmployeeEntityRepository employeeRepository;

    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        log.info("Creating department with code: {}", request.getDepartmentCode());

        Client client = clientRepository.findById(request.getClientId())
            .orElseThrow(() -> new EntityNotFoundException("Client not found: " + request.getClientId()));

        if (departmentRepository.existsByClientIdAndDepartmentCode(request.getClientId(), request.getDepartmentCode())) {
            throw new IllegalArgumentException("Department code already exists: " + request.getDepartmentCode());
        }

        DepartmentEntity department = new DepartmentEntity();
        department.setClient(client);
        department.setName(request.getName());
        department.setDepartmentCode(request.getDepartmentCode());
        department.setDescription(request.getDescription());
        department.setCostCenter(request.getCostCenter());
        department.setGlAccountCode(request.getGlAccountCode());
        department.setDefaultWorkLocation(request.getDefaultWorkLocation());
        department.setStateCode(request.getStateCode());
        department.setIsActive(true);

        if (request.getParentDepartmentId() != null) {
            DepartmentEntity parent = departmentRepository.findById(request.getParentDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Parent department not found"));
            department.setParentDepartment(parent);
        }

        if (request.getManagerId() != null) {
            EmployeeEntity manager = employeeRepository.findById(request.getManagerId())
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
            department.setManager(manager);
        }

        department = departmentRepository.save(department);
        log.info("Created department: {}", department.getId());

        return mapToResponse(department);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(UUID id) {
        DepartmentEntity department = departmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Department not found: " + id));
        return mapToResponse(department);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getAllDepartments(UUID clientId, Pageable pageable) {
        return departmentRepository.findByClientId(clientId, pageable)
            .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getActiveDepartments(UUID clientId) {
        return departmentRepository.findByClientIdAndIsActiveTrue(clientId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID id, DepartmentCreateRequest request) {
        DepartmentEntity department = departmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Department not found: " + id));

        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setCostCenter(request.getCostCenter());
        department.setGlAccountCode(request.getGlAccountCode());
        department.setDefaultWorkLocation(request.getDefaultWorkLocation());
        department.setStateCode(request.getStateCode());

        department = departmentRepository.save(department);
        return mapToResponse(department);
    }

    @Transactional
    public void deactivateDepartment(UUID id) {
        DepartmentEntity department = departmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Department not found: " + id));
        department.setIsActive(false);
        departmentRepository.save(department);
        log.info("Deactivated department: {}", id);
    }

    @Transactional
    public void deleteDepartment(UUID id) {
        if (!departmentRepository.existsById(id)) {
            throw new EntityNotFoundException("Department not found: " + id);
        }
        departmentRepository.deleteById(id);
        log.info("Deleted department: {}", id);
    }

    private DepartmentResponse mapToResponse(DepartmentEntity entity) {
        DepartmentResponse.DepartmentResponseBuilder builder = DepartmentResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .departmentCode(entity.getDepartmentCode())
            .description(entity.getDescription())
            .costCenter(entity.getCostCenter())
            .glAccountCode(entity.getGlAccountCode())
            .defaultWorkLocation(entity.getDefaultWorkLocation())
            .stateCode(entity.getStateCode())
            .isActive(entity.getIsActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt());

        if (entity.getParentDepartment() != null) {
            builder.parentDepartment(DepartmentResponse.ParentDepartmentSummary.builder()
                .id(entity.getParentDepartment().getId())
                .name(entity.getParentDepartment().getName())
                .departmentCode(entity.getParentDepartment().getDepartmentCode())
                .build());
        }

        if (entity.getManager() != null) {
            String managerEmail = entity.getManager().getContactInfo() != null
                ? entity.getManager().getContactInfo().getEmail()
                : null;
            builder.manager(DepartmentResponse.ManagerSummary.builder()
                .id(entity.getManager().getId())
                .firstName(entity.getManager().getFirstName())
                .lastName(entity.getManager().getLastName())
                .email(managerEmail)
                .build());
        }

        return builder.build();
    }
}
