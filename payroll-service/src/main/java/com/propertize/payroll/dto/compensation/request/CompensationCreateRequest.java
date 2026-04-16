package com.propertize.payroll.dto.compensation.request;

import com.propertize.payroll.enums.CompensationTypeEnum;
import com.propertize.commons.enums.employee.PayFrequencyEnum;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new compensation record.
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationCreateRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    @NotNull(message = "Compensation type is required")
    private CompensationTypeEnum compensationType;

    @NotNull(message = "Pay frequency is required")
    private PayFrequencyEnum payFrequency;

    @DecimalMin(value = "0.01", message = "Hourly rate must be greater than 0")
    @Digits(integer = 10, fraction = 4, message = "Invalid hourly rate format")
    private BigDecimal hourlyRate;

    @DecimalMin(value = "0.01", message = "Annual salary must be greater than 0")
    @Digits(integer = 12, fraction = 2, message = "Invalid annual salary format")
    private BigDecimal annualSalary;

    @DecimalMin(value = "0.0", message = "Standard hours cannot be negative")
    @Digits(integer = 3, fraction = 2, message = "Invalid standard hours format")
    private BigDecimal standardHoursPerPeriod;

    @DecimalMin(value = "1.0", message = "Overtime multiplier must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Overtime multiplier cannot exceed 5.0")
    private BigDecimal overtimeMultiplier;

    @DecimalMin(value = "1.0", message = "Double time multiplier must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Double time multiplier cannot exceed 5.0")
    private BigDecimal doubleTimeMultiplier;

    @NotNull(message = "Effective date is required")
    @FutureOrPresent(message = "Effective date cannot be in the past")
    private LocalDate effectiveDate;

    @Size(max = 500, message = "Change reason cannot exceed 500 characters")
    private String changeReason;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    private LocalDate endDate;

    private Boolean isCurrentCompensation;
}
