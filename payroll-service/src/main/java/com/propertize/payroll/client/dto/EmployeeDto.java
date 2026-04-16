package com.propertize.payroll.client.dto;

import com.propertize.commons.enums.employee.EmployeeStatusEnum;

import com.propertize.commons.enums.employee.PayFrequencyEnum;

import com.propertize.commons.enums.employee.PayTypeEnum;

import com.propertize.commons.enums.employee.EmploymentTypeEnum;

import com.propertize.commons.enums.common.RelationshipTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Employee data from Employecraft microservice
 * This is a read-only representation of employee data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {

    private UUID id;
    private UUID organizationId;
    private Long userId;

    // Identification
    private String employeeNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String workEmail;
    private String phoneNumber;
    private String workPhone;
    private LocalDate dateOfBirth;

    // SSN (encrypted in source)
    private String ssnEncrypted;

    // Employment Details
    private EmploymentTypeEnum employmentType;
    private EmployeeStatusEnum status;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private String terminationReason;

    // Job Details
    private String jobTitle;
    private UUID departmentId;
    private String departmentName;
    private UUID positionId;
    private String positionTitle;
    private UUID managerId;

    // Compensation (from Employecraft)
    private CompensationDto compensation;

    // Address
    private AddressDto address;

    // Emergency Contact
    private EmergencyContactDto emergencyContact;

    // Metadata
    private String createdAt;
    private String updatedAt;

    /**
     * Get full name
     */
    public String getFullName() {
        if (middleName != null && !middleName.isEmpty()) {
            return String.format("%s %s %s", firstName, middleName, lastName);
        }
        return String.format("%s %s", firstName, lastName);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompensationDto {
        private PayTypeEnum payType;           // HOURLY, SALARIED
        private PayFrequencyEnum payFrequency;      // WEEKLY, BI_WEEKLY, SEMI_MONTHLY, MONTHLY
        private BigDecimal hourlyRate;
        private BigDecimal annualSalary;
        private String currency;
        private LocalDate effectiveDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private String street;
        private String street2;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyContactDto {
        private String name;
        private RelationshipTypeEnum relationship;
        private String phone;
        private String email;
    }
}
