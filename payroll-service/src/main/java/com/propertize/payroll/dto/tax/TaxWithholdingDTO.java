package com.propertize.payroll.dto.tax;

import com.propertize.payroll.enums.FilingStatusEnum;
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
public class TaxWithholdingDTO {

    private UUID id;

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    private String employeeName;

    @NotNull(message = "Tax year is required")
    private Integer taxYear;

    @NotNull(message = "Federal filing status is required")
    private FilingStatusEnum federalFilingStatus;

    private FilingStatusEnum stateFilingStatus;

    private Integer federalAllowances;

    private BigDecimal federalAdditionalWithholding;
    private BigDecimal stateAdditionalWithholding;

    private Boolean federalExempt;
    private Boolean stateExempt;
    private Boolean ficaExempt;

    // New W-4 (2020+) fields
    private BigDecimal otherIncome;
    private BigDecimal deductions;
    private BigDecimal dependentCredit;
    private Boolean isNewW4Format;
    private Boolean multipleJobs;

    private String workState;
    private String residentState;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private Boolean isCurrent;

    private String notes;
}
