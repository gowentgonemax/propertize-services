package com.propertize.platform.employecraft.dto.employee.response;

import com.propertize.commons.enums.employee.EmployeeStatusEnum;

import com.propertize.commons.enums.employee.PayFrequencyEnum;

import com.propertize.commons.enums.employee.PayTypeEnum;

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
 * Minimal employee DTO for payroll-service sync.
 * Contains only the fields needed for payroll processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePayrollSummary {
    private UUID id;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private EmployeeStatusEnum status;
    private EmploymentTypeEnum employmentType;
    private LocalDate hireDate;
    private LocalDate terminationDate;

    // Compensation
    private PayTypeEnum payType;
    private BigDecimal payRate;
    private PayFrequencyEnum payFrequency;

    // Metadata
    private LocalDateTime updatedAt;
}
