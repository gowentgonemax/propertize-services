package com.propertize.payroll.dto.compensation.response;

import com.propertize.payroll.enums.CompensationStatusEnum;
import com.propertize.payroll.enums.CompensationTypeEnum;

import com.propertize.commons.enums.employee.PayFrequencyEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for compensation history (all compensation records for an
 * employee).
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationHistoryResponse {

    private UUID id;
    private CompensationTypeEnum compensationType;
    private CompensationStatusEnum status;
    private BigDecimal hourlyRate;
    private BigDecimal annualSalary;
    private PayFrequencyEnum payFrequency;
    private LocalDate effectiveDate;
    private LocalDate endDate;
    private String changeReason;
    private Boolean isCurrent;
}
