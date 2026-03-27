package com.propertize.payroll.dto.department.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentCreateRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotBlank(message = "Department name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Department code is required")
    @Size(max = 20, message = "Code cannot exceed 20 characters")
    private String departmentCode;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private UUID parentDepartmentId;

    @Size(max = 20, message = "Cost center cannot exceed 20 characters")
    private String costCenter;

    @Size(max = 20, message = "GL account code cannot exceed 20 characters")
    private String glAccountCode;

    private UUID managerId;

    @Size(max = 100, message = "Default work location cannot exceed 100 characters")
    private String defaultWorkLocation;

    @Size(max = 2, message = "State code must be 2 characters")
    private String stateCode;
}
