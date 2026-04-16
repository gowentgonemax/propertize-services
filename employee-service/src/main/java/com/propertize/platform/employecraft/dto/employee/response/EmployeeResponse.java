package com.propertize.platform.employecraft.dto.employee.response;

import com.propertize.platform.employecraft.dto.*;
import com.propertize.commons.enums.employee.EmployeeStatusEnum;
import com.propertize.commons.enums.employee.EmploymentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Employee response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private UUID id;
    private UUID organizationId;
    private Long userId;
    private String employeeNumber;

    // Personal Info
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String email;
    private String workEmail;
    private String phoneNumber;
    private String workPhone;
    private LocalDate dateOfBirth;
    private String profilePhotoUrl;

    // Employment
    private EmploymentTypeEnum employmentType;
    private EmployeeStatusEnum status;
    private LocalDate hireDate;
    private LocalDate terminationDate;

    // Relationships
    private DepartmentSummary department;
    private PositionSummary position;
    private ManagerSummary manager;

    // Address
    private AddressSummary homeAddress;

    // Compensation
    private CompensationSummary compensation;

    // System Access
    private boolean hasSystemAccess;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
