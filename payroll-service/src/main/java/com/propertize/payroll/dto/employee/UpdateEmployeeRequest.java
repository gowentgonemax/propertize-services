package com.propertize.payroll.dto.employee;

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
public class UpdateEmployeeRequest {

    private String firstName;
    private String middleName;
    private String lastName;
    private String preferredName;

    private String email;
    private String phone;
    private String mobile;

    // Address
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    private String status;
    private String employmentType;
    private String payType;
    private String payFrequency;

    private BigDecimal hourlyRate;
    private BigDecimal annualSalary;

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
}
