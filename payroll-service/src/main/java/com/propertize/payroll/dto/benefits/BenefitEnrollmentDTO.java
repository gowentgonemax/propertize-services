package com.propertize.payroll.dto.benefits;

import com.propertize.payroll.enums.BenefitTypeEnum;

import com.propertize.payroll.enums.CoverageLevelEnum;
import com.propertize.payroll.enums.EnrollmentStatusEnum;
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
public class BenefitEnrollmentDTO {

    private UUID id;

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    private String employeeName;

    @NotNull(message = "Benefit plan ID is required")
    private UUID benefitPlanId;

    private String benefitPlanName;
    private BenefitTypeEnum benefitType;

    @NotNull(message = "Enrollment date is required")
    private LocalDate enrollmentDate;

    private LocalDate effectiveDate;
    private LocalDate terminationDate;

    private CoverageLevelEnum coverageLevel;

    private BigDecimal employeeContribution;
    private BigDecimal employerContribution;
    private BigDecimal totalContribution;

    private EnrollmentStatusEnum status;
}
