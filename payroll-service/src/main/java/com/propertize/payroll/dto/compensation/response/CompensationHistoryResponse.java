package com.propertize.payroll.dto.compensation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for compensation history (all compensation records for an employee).
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
    private String compensationType;
    private String status;
    private BigDecimal hourlyRate;
    private BigDecimal annualSalary;
    private String payFrequency;
    private LocalDate effectiveDate;
    private LocalDate endDate;
    private String changeReason;
    private Boolean isCurrent;
}
