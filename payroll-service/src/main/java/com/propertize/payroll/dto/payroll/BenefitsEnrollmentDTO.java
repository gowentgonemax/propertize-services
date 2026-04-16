package com.propertize.payroll.dto.payroll;

import com.propertize.payroll.enums.EnrollmentStatusEnum;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenefitsEnrollmentDTO {
    @NotNull
    private Long employeeId;

    private String planType; // HEALTH, DENTAL, VISION, LIFE, DISABILITY, FSA, HSA

    private String planTier; // INDIVIDUAL, EMPLOYEE_SPOUSE, FAMILY

    private String planCode;

    private String carrierName;

    private String groupNumber;

    private String memberNumber;

    @NotNull
    private LocalDate effectiveDate;

    @Future
    private LocalDate terminationDate;

    private BigDecimal employeeCost;

    private BigDecimal employerCost;

    private String payrollFrequency; // BI_WEEKLY, MONTHLY, etc.

    private Boolean isPreTax;

    private EnrollmentStatusEnum status;

    private String dependentsCovered; // JSON string with dependent information

    private String specialProvisions;

    private String waiverReason; // If benefits are waived
}
