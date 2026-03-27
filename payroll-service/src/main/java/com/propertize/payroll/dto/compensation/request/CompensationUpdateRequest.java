package com.propertize.payroll.dto.compensation.request;

import com.propertize.payroll.enums.CompensationStatusEnum;
import com.propertize.payroll.enums.PayFrequencyEnum;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating an existing compensation record.
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationUpdateRequest {

    private CompensationStatusEnum status;

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

    private LocalDate endDate;

    @Size(max = 500, message = "Change reason cannot exceed 500 characters")
    private String changeReason;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
