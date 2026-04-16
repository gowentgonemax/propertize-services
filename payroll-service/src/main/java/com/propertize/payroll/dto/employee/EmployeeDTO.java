package com.propertize.payroll.dto.employee;

import com.propertize.commons.enums.employee.EmployeeStatusEnum;
import com.propertize.commons.enums.employee.EmploymentTypeEnum;
import com.propertize.commons.enums.employee.PayFrequencyEnum;
import com.propertize.commons.enums.employee.PayTypeEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {

    private UUID id;

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    private UUID externalEmployeeId;

    @NotBlank(message = "Employee number is required")
    @Size(max = 50, message = "Employee number cannot exceed 50 characters")
    private String employeeNumber;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Middle name cannot exceed 100 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    private String preferredName;

    @Size(max = 4, message = "SSN last four must be 4 digits")
    private String ssnLastFour;

    private LocalDate dateOfBirth;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String mobile;

    // Address fields
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    @NotNull(message = "Status is required")
    private EmployeeStatusEnum status;

    @NotNull(message = "Employment type is required")
    private EmploymentTypeEnum employmentType;

    @NotNull(message = "Pay type is required")
    private PayTypeEnum payType;

    @NotNull(message = "Pay frequency is required")
    private PayFrequencyEnum payFrequency;

    private BigDecimal hourlyRate;
    private BigDecimal annualSalary;

    @NotNull(message = "Hire date is required")
    private LocalDate hireDate;

    private LocalDate terminationDate;

    private String jobTitle;
    private String departmentId;
    private String departmentName;
    private String managerId;
    private String managerName;
    private String workLocation;
    private String costCenter;

    private BigDecimal standardHoursPerWeek;
    private BigDecimal overtimeMultiplier;
    private Boolean overtimeEligible;

    // Computed fields
    private String fullName;
    private String displayName;
    private BigDecimal effectivePayRate;
}
