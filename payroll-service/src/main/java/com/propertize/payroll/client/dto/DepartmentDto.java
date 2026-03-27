package com.propertize.payroll.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for Department data from Employecraft microservice
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {

    private UUID id;
    private UUID organizationId;
    private String name;
    private String code;
    private String description;
    private UUID parentDepartmentId;
    private String parentDepartmentName;
    private UUID managerId;
    private String managerName;
    private String status;
    private Integer employeeCount;
    private String createdAt;
    private String updatedAt;
}
