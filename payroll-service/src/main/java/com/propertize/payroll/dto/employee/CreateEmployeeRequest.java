package com.propertize.payroll.dto.employee;

import com.propertize.commons.enums.employee.PayFrequencyEnum;

import com.propertize.commons.enums.employee.PayTypeEnum;

import com.propertize.commons.enums.employee.EmploymentTypeEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateEmployeeRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    private UUID externalEmployeeId;

    @NotBlank(message = "Employee number is required")
    private String employeeNumber;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String preferredName;

    private String ssnLastFour;
    private String ssnEncrypted;

    private LocalDate dateOfBirth;

    private String email;
    private String phone;
    private String mobile;

    // Address
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;

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

    private String jobTitle;
    private String departmentId;
    private String departmentName;
    private String managerId;
    private String workLocation;
    private String costCenter;
}
