package com.propertize.platform.employecraft.dto.employee.request;

import com.propertize.commons.enums.common.RelationshipTypeEnum;

import com.propertize.commons.enums.employee.PayFrequencyEnum;

import com.propertize.commons.enums.employee.PayTypeEnum;

import com.propertize.commons.enums.employee.EmploymentTypeEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Request to create a new employee
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCreateRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String workEmail;
    private String phoneNumber;
    private String workPhone;
    private LocalDate dateOfBirth;

    @NotNull(message = "Employment type is required")
    private EmploymentTypeEnum employmentType;

    @NotNull(message = "Hire date is required")
    private LocalDate hireDate;

    private UUID departmentId;
    private UUID positionId;
    private UUID managerId;

    // Address
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // Compensation
    private PayTypeEnum payType;
    private BigDecimal payRate;
    private PayFrequencyEnum payFrequency;
    private String bankName;
    private String bankAccountNumber;
    private String bankRoutingNumber;

    // Emergency Contact
    private String emergencyContactName;
    private RelationshipTypeEnum emergencyContactRelationship;
    private String emergencyContactPhone;
    private String emergencyContactEmail;

    // System Access
    private boolean createSystemAccess;
    private Set<String> systemRoles;
    private String tempPassword;

    /**
     * Optional: link the employee to an existing auth-service user.
     * When provided, the employee record is linked to the user's ID,
     * enabling the user to log in and access their employee profile.
     */
    private Long userId;
}
