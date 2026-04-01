package com.propertize.payroll.client.dto;

import lombok.Builder;
import java.util.UUID;

/**
 * DTO for Department data from Employecraft microservice
 */
@Builder
public record DepartmentDto(
        UUID id,
        UUID organizationId,
        String name,
        String code,
        String description,
        UUID parentDepartmentId,
        String parentDepartmentName,
        UUID managerId,
        String managerName,
        String status,
        Integer employeeCount,
        String createdAt,
        String updatedAt) {
}
