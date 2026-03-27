package com.propertize.platform.employecraft.dto.employee.response;

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
    private String status;
    private String employmentType;
    private LocalDate hireDate;
    private LocalDate terminationDate;

    // Compensation
    private String payType;
    private BigDecimal payRate;
    private String payFrequency;

    // Metadata
    private LocalDateTime updatedAt;
}
