package com.propertize.platform.employecraft.service;

import com.propertize.platform.employecraft.context.OrganizationContext;
import com.propertize.platform.employecraft.dto.DepartmentSummary;
import com.propertize.platform.employecraft.entity.Department;
import com.propertize.platform.employecraft.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Provides department listing for employee management workflows.
 * Scoped to the organization extracted from the gateway context.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * Returns all active departments for the currently authenticated organization.
     * Used to populate department dropdowns in employee creation and edit forms.
     *
     * @return list of active department summaries, ordered by name
     */
    @Transactional(readOnly = true)
    public List<DepartmentSummary> getActiveDepartments() {
        UUID organizationId = OrganizationContext.requireOrganizationId();
        List<Department> departments = departmentRepository
                .findByOrganizationIdAndIsActiveTrue(organizationId.toString());
        log.debug("Found {} active departments for org {}", departments.size(), organizationId);
        return departments.stream()
                .map(d -> DepartmentSummary.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .code(d.getCode())
                        .build())
                .sorted(java.util.Comparator.comparing(DepartmentSummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}

