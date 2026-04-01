package com.propertize.payroll.client.dto;

import lombok.Builder;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Position data from Employecraft microservice
 */
@Builder
public record PositionDto(
        UUID id,
        UUID organizationId,
        String title,
        String code,
        String description,
        UUID departmentId,
        String departmentName,
        Integer jobLevel,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        String currency,
        String status,
        Integer employeeCount,
        String createdAt,
        String updatedAt) {
}
