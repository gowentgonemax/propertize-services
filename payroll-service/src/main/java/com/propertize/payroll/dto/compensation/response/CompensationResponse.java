package com.propertize.payroll.dto.compensation.response;

import com.propertize.payroll.enums.CompensationStatusEnum;
import com.propertize.payroll.enums.CompensationTypeEnum;
import com.propertize.payroll.enums.PayFrequencyEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for compensation details.
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationResponse {

    private UUID id;

    private UUID employeeId;
    private String employeeNumber;
    private String employeeFullName;

    private CompensationTypeEnum compensationType;
    private CompensationStatusEnum status;
    private PayFrequencyEnum payFrequency;

    private BigDecimal hourlyRate;
    private BigDecimal annualSalary;
    private BigDecimal payRatePerPeriod;
    private BigDecimal standardHoursPerPeriod;
    private BigDecimal overtimeMultiplier;
    private BigDecimal doubleTimeMultiplier;

    private LocalDate effectiveDate;
    private LocalDate endDate;
    private String changeReason;
    private String notes;

    private Boolean isCurrentCompensation;

    // Audit fields
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
