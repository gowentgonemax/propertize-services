package com.propertize.payroll.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Position data from Employecraft microservice
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionDto {

    private UUID id;
    private UUID organizationId;
    private String title;
    private String code;
    private String description;
    private UUID departmentId;
    private String departmentName;
    private Integer jobLevel;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency;
    private String status;
    private Integer employeeCount;
    private String createdAt;
    private String updatedAt;
}
